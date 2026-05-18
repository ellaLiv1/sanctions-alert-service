package com.sanctions.alert.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanctions.alert.domain.AlertStatus;
import com.sanctions.alert.events.AlertEvent;
import com.sanctions.alert.events.DecidedAlertEvent;
import com.sanctions.alert.events.EscalatedAlertEvent;
import com.sanctions.alert.events.LogEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Alert API integration tests")
class AlertControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @SpyBean LogEventPublisher eventPublisher;

    private static final String TENANT = "tenant-A";
    private static final String OTHER_TENANT = "tenant-B";

    @Test
    @DisplayName("POST /alerts → 201 with full alert response")
    void createAlert_returns201() throws Exception {
        mockMvc.perform(post("/alerts")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("tx-1", "OFAC Corp", 85)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.transactionId").value("tx-1"))
                .andExpect(jsonPath("$.matchedEntityName").value("OFAC Corp"))
                .andExpect(jsonPath("$.matchScore").value(85))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    @Test
    @DisplayName("POST /alerts without X-Tenant-ID → 400")
    void createAlert_missingTenant_400() throws Exception {
        mockMvc.perform(post("/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("tx-1", "OFAC Corp", 85)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /alerts with invalid matchScore → 400")
    void createAlert_invalidScore_400() throws Exception {
        mockMvc.perform(post("/alerts")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody("tx-1", "OFAC Corp", 150)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("GET /alerts returns only alerts for the requesting tenant wrapped in alerts field")
    void listAlerts_tenantIsolation() throws Exception {
        mockMvc.perform(post("/alerts")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("tx-1", "Corp A", 80)));

        mockMvc.perform(post("/alerts")
                .header("X-Tenant-ID", OTHER_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody("tx-2", "Corp B", 70)));

        mockMvc.perform(get("/alerts").header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /alerts?status=OPEN returns only OPEN alerts")
    void listAlerts_filterByStatus() throws Exception {
        String id = createAndGetId("tx-1", "Corp A", 80);
        escalate(id);
        createAndGetId("tx-2", "Corp B", 75);

        mockMvc.perform(get("/alerts")
                        .header("X-Tenant-ID", TENANT)
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /alerts?minMatchScore=85 filters by score")
    void listAlerts_filterByMinScore() throws Exception {
        createAndGetId("tx-1", "Corp A", 90);
        createAndGetId("tx-2", "Corp B", 70);

        mockMvc.perform(get("/alerts")
                        .header("X-Tenant-ID", TENANT)
                        .param("minMatchScore", "85"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerts", hasSize(1)))
                .andExpect(jsonPath("$.alerts[0].matchScore").value(90));
    }

    @Test
    @DisplayName("POST /alerts/{id}/escalate → 200 with ESCALATED status and publishes event")
    void escalate_success_and_publishes_event() throws Exception {
        String id = createAndGetId("tx-1", "OFAC Corp", 85);

        mockMvc.perform(post("/alerts/" + id + "/escalate")
                        .header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESCALATED"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        ArgumentCaptor<AlertEvent> cap = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventPublisher).publish(cap.capture());
        assertThat(cap.getValue()).isInstanceOf(EscalatedAlertEvent.class);
        assertThat(cap.getValue().tenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("POST /alerts/{id}/escalate on decided alert → 422 Unprocessable Entity")
    void escalate_alreadyDecided_422() throws Exception {
        String id = createAndGetId("tx-1", "OFAC Corp", 85);
        decide(id, "CLEARED", "no match");
        mockMvc.perform(post("/alerts/" + id + "/escalate")
                        .header("X-Tenant-ID", TENANT))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /alerts/{id}/escalate for another tenant's alert → 404")
    void escalate_crossTenant_404() throws Exception {
        String id = createAndGetId("tx-1", "OFAC Corp", 85);
        mockMvc.perform(post("/alerts/" + id + "/escalate")
                        .header("X-Tenant-ID", OTHER_TENANT))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /alerts/{id}/decide with CLEARED → 200 with decisionNote and publishes event")
    void decide_cleared_success_and_publishes_event() throws Exception {
        String id = createAndGetId("tx-1", "OFAC Corp", 85);

        mockMvc.perform(post("/alerts/" + id + "/decide")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decideBody("CLEARED", "false positive")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"))
                .andExpect(jsonPath("$.decisionNote").value("false positive"))
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());

        ArgumentCaptor<AlertEvent> cap = ArgumentCaptor.forClass(AlertEvent.class);
        verify(eventPublisher).publish(cap.capture());
        DecidedAlertEvent event = (DecidedAlertEvent) cap.getValue();
        assertThat(event.getDecision()).isEqualTo(AlertStatus.CLEARED.name());
        assertThat(event.tenantId()).isEqualTo(TENANT);
    }

    @Test
    @DisplayName("POST /alerts/{id}/decide twice → 409 Conflict (write-once)")
    void decide_twice_409() throws Exception {
        String id = createAndGetId("tx-1", "OFAC Corp", 85);
        decide(id, "CLEARED", "first decision");
        mockMvc.perform(post("/alerts/" + id + "/decide")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decideBody("CONFIRMED_HIT", "second attempt")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /alerts/{id}/decide without decisionNote → 400")
    void decide_missingNote_400() throws Exception {
        String id = createAndGetId("tx-1", "OFAC Corp", 85);
        mockMvc.perform(post("/alerts/" + id + "/decide")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("decision", "CLEARED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /alerts/{id}/decide on another tenant's alert → 404")
    void decide_crossTenant_404() throws Exception {
        String id = createAndGetId("tx-1", "OFAC Corp", 85);
        mockMvc.perform(post("/alerts/" + id + "/decide")
                        .header("X-Tenant-ID", OTHER_TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(decideBody("CLEARED", "not my tenant")))
                .andExpect(status().isNotFound());
    }

    private String createAndGetId(String txId, String entity, int score) throws Exception {
        MvcResult result = mockMvc.perform(post("/alerts")
                        .header("X-Tenant-ID", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(txId, entity, score)))
                .andExpect(status().isCreated())
                .andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void escalate(String id) throws Exception {
        mockMvc.perform(post("/alerts/" + id + "/escalate").header("X-Tenant-ID", TENANT));
    }

    private void decide(String id, String decision, String note) throws Exception {
        mockMvc.perform(post("/alerts/" + id + "/decide")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(decideBody(decision, note)));
    }

    private String createBody(String txId, String entity, int score) throws Exception {
        return mapper.writeValueAsString(Map.of(
                "transactionId", txId,
                "matchedEntityName", entity,
                "matchScore", score
        ));
    }

    private String decideBody(String decision, String note) throws Exception {
        return mapper.writeValueAsString(Map.of("decision", decision, "decisionNote", note));
    }
}
