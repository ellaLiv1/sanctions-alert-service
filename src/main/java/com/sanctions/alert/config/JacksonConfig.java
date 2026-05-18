package com.sanctions.alert.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                // Serialize Instant as ISO-8601 string, not epoch array
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                // Omit null fields (e.g. assignedTo, decisionNote when absent)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
}
