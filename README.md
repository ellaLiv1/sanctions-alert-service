# Sanctions Alert Service

A REST API service for managing financial sanctions screening alerts, built with Java 21 + Spring Boot 3.

---

## 1. How to Run

**Prerequisites:** Java 21+, Maven 3.9+

**Start the service:**
```bash
cd sanctions-alert-service
mvn spring-boot:run
```
Service starts on `http://localhost:8080`.

**Run tests:**
```bash
mvn test
```

---

## 2. API Reference

All requests must include the `X-Tenant-ID` in header. Missing tenant id in header returns `400 Bad Request`.
All timestamps are ISO-8601 UTC format, e.g. `2024-01-01T10:00:00Z`.

---

#### `POST /alerts` — Create Alert

Request:
```json
{
  "transactionId": "tx-9823",
  "matchedEntityName": "OFAC Corp",
  "matchScore": 87,
  "assignedTo": "analyst-1"
}
```
> `assignedTo` is optional. `matchScore` must be 0–100.

Response `201 Created`:
```json
{
  "id": "a3f1c...",
  "transactionId": "tx-9823",
  "matchedEntityName": "OFAC Corp",
  "matchScore": 87,
  "status": "OPEN",
  "assignedTo": "analyst-1",
  "createdAt": "2024-01-01T10:00:00Z",
  "updatedAt": "2024-01-01T10:00:00Z"
}
```
> Returns the full persisted alert as required by the spec. `tenantId` is excluded from all responses — the caller already knows their tenant and exposing it is an unnecessary security risk.

| Scenario | Status |
|---|---|
| Success | `201 Created` |
| Missing `X-Tenant-ID` header | `400 Bad Request` |
| Blank/missing `transactionId` or `matchedEntityName` | `400 Bad Request` |
| `matchScore` outside 0–100 | `400 Bad Request` |
| Malformed JSON body | `400 Bad Request` |
| Unexpected error | `500 Internal Server Error` |

---

#### `GET /alerts` — List Alerts

Query params (all optional): `status=OPEN`, `minMatchScore=70`

> If no alerts match the filter, returns `{ "alerts": [] }` — empty list, not 404.

Response `200 OK`:
```json
{
  "alerts": [
    {
      "id": "a3f1c...",
      "transactionId": "tx-9823",
      "matchedEntityName": "OFAC Corp",
      "matchScore": 87,
      "status": "OPEN",
      "assignedTo": "analyst-1",
      "decisionNote": null,
      "createdAt": "2024-01-01T10:00:00Z",
      "updatedAt": "2024-01-01T10:00:00Z"
    }
  ]
}
```

| Scenario | Status |
|---|---|
| Success (even if response is empty) | `200 OK` |
| Missing `X-Tenant-ID` header | `400 Bad Request` |
| Unexpected error | `500 Internal Server Error` |

---

#### `POST /alerts/{id}/escalate` — Escalate Alert

No request body. `id` is taken from the URL path.

Response `200 OK`:
```json
{
  "status": "ESCALATED",
  "updatedAt": "2024-01-01T10:05:00Z"
}
```

| Scenario | Status |
|---|---|
| Success (`OPEN` → `ESCALATED`) | `200 OK` |
| Alert not found | `404 Not Found` |
| Invalid state transition | `422 Unprocessable Entity` |
| Already decided (terminal status) | `409 Conflict` |
| Missing `X-Tenant-ID` header | `400 Bad Request` |

---

#### `POST /alerts/{id}/decide` — Submit Decision

Request:
```json
{
  "decision": "CLEARED",
  "decisionNote": "Verified — not the same entity"
}
```
> `decision` must be `CLEARED` or `CONFIRMED_HIT`. `decisionNote` is required.

Response `200 OK`:
```json
{
  "status": "CLEARED",
  "decisionNote": "Verified — not the same entity",
  "updatedAt": "2024-01-01T10:10:00Z"
}
```

| Scenario | Status |
|---|---|
| Success | `200 OK` |
| Alert not found | `404 Not Found` |
| Already decided (write-once) | `409 Conflict` |
| Missing `decisionNote` | `400 Bad Request` |

---

## 3. Design Decisions

#### Multi-Tenancy: `X-Tenant-ID` Header

Tenant identity is carried in an `X-Tenant-ID` HTTP header, extracted by `TenantFilter` before the request reaches any controller.

- `TenantFilter` is the **single enforcement point** — no controller or service can accidentally skip isolation
- Tenant is **never read from the request body** — prevents spoofing
- Repository methods accept `tenantId` explicitly — second isolation layer at data access level

**Threat model assumption:** A trusted upstream API gateway authenticates the caller and injects the correct `X-Tenant-ID`. In production this would be derived from a verified JWT claim.

**Why tenant error is handled in the filter directly:** Servlet Filters run before Spring MVC, so `GlobalExceptionHandler` (`@RestControllerAdvice`) cannot intercept exceptions from filters. Writing the `400` response directly in `TenantFilter` is the only viable approach.

---

#### HTTP Layer Naming

The assignment uses the generic term "handler". I chose idiomatic Spring/Java conventions both for package naming (`controller/`) and class naming (`AlertController` annotated with `@RestController`). The assignment terminology is framework-agnostic — since I chose Spring Boot, I aligned with its standard stereotypes and naming conventions. The separation of concerns — controller → service → repository — is fully maintained as required.

---

#### HTTP Verbs and Paths

| Operation | Verb + Path | Rationale |
|---|---|---|
| Create alert | `POST /alerts` | Standard resource creation |
| List alerts | `GET /alerts` | Idempotent read with query params |
| Escalate | `POST /alerts/{id}/escalate` | Triggers a named business operation with state machine rules and event publishing — not a simple field update |
| Decide | `POST /alerts/{id}/decide` | Same reasoning — a named compliance action with write-once rules and event publishing |

---

#### Response Design

Each endpoint has its own dedicated response DTO — returning only what the caller needs:

- `POST /alerts` → returns the full persisted alert as required by the spec. `tenantId` excluded for security — caller already knows it.
- `GET /alerts` → returns full alert details wrapped in `{ "alerts": [...] }` instead of a raw array. This allows adding pagination metadata (page, size, totalElements) in future without breaking the API contract — callers would get a new field, not a changed type.
- `POST /alerts/{id}/escalate` → returns `status`, `updatedAt` (no `id` — caller knows it from the URL)
- `POST /alerts/{id}/decide` → returns `status`, `decisionNote`, `updatedAt`

The assignment does not specify a response body for escalate or decide — returning a response was a design decision for better user experience, giving the caller immediate confirmation of the transition outcome and timestamp without requiring a follow-up `GET` request.

`tenantId` and `transactionId` are excluded from all responses — the caller already knows them (sent in header and request body respectively).

---

#### DTO Implementation: Classes vs Records

All DTOs are implemented as regular Java classes with explicit constructors and getters instead of Java records. This was a conscious decision to make all generated code fully visible for reviewers — making it easier to see the complete structure without requiring knowledge of Java record semantics. In production, Java records would be the correct choice — see production changes section.

---

#### Package Structure

```
controller/     — HTTP layer (@RestController)
dto/            — External request/response DTOs
mapper/         — Maps domain objects to DTOs
domain/         — Core business logic (Alert, AlertStatus, CreateAlertCommand)
  exception/    — Domain exceptions (RuntimeException subclasses)
service/        — Use-case orchestration
repository/     — Persistence interface + in-memory implementation
events/         — Domain events and publisher
middleware/     — TenantFilter, TenantContext
config/         — Jackson configuration
```

---

#### Domain Exceptions

All domain exceptions extend `RuntimeException` (unchecked) because they represent business rule violations discoverable only at runtime. `GlobalExceptionHandler` catches them in one place — no `try/catch` needed in controllers:

- `AlertNotFoundException` → `404 Not Found`
- `AlreadyDecidedException` → `409 Conflict`
- `InvalidTransitionException` → `422 Unprocessable Entity`
- Unknown exceptions → `500 Internal Server Error` (original message and stack trace logged internally, generic message returned to caller for security — internal details must never be exposed)

---

#### State Machine

```
OPEN ──escalate──► ESCALATED ─┐
  │                           │
  └───────────────────────────┴──decide(CLEARED | CONFIRMED_HIT)──► terminal
```

Transition logic lives exclusively in the `Alert` domain class. Decisions are write-once — once decided, no further transitions allowed.

---

#### Event Publishing

`LogEventPublisher` emits structured JSON to stdout. The key design is the `EventPublisher` interface — swapping to Kafka/NATS/SQS requires zero changes to `AlertService`.

`AlertEvent` is a regular interface — not sealed — because it is an internal contract owned entirely by this service. New event types will always be added by developers who will also update the publisher. The publisher uses a pattern matching switch with a `default` case that throws `IllegalArgumentException` for unknown event types — failing fast if a developer forgets to handle a new event type.

The `decision` field in `DecidedAlertEvent` is defined as `String` rather than `AlertStatus` enum. Events are self-contained messages intended for external consumers — Kafka, log aggregators, other services — who should not depend on internal domain enums. Converting to `String` at the service boundary (`alert.getStatus().name()`) keeps events decoupled from the domain model.

---

#### Repository Pattern

`AlertRepository` is a plain Java interface. `InMemoryAlertRepository` uses `ConcurrentHashMap` for thread safety. To move to a real database: implement `AlertRepository` with JPA — the service layer is untouched.

---

## 4. What I Would Change for Production

| Area | Change |
|---|---|
| **Database** | Replace `InMemoryAlertRepository` with a real database implementation. To switch — create `JpaAlertRepository implements AlertRepository`, add `@Repository` to it, and remove `@Repository` from `InMemoryAlertRepository`. The service layer requires zero changes. Add `@Version` for optimistic locking and a `tenant_id` index. |
| **Authentication** | Deploy an API Gateway to verify JWT tokens and inject `X-Tenant-ID` from the verified claims. Our service requires no code changes — it continues trusting the header, but now the header comes from a verified source rather than directly from the caller. |
| **Message broker** | Replace `LogEventPublisher` with Kafka/NATS/SQS. Add outbox pattern to guarantee no events are lost. |
| **DTOs as records** | Replace DTO classes with Java records — immutable, concise, no boilerplate. See example below. |
| **`AlertStatus` DTO** | Create `AlertStatusDto` enum in `dto/` to fully decouple the HTTP layer from the domain enum. Currently `AlertStatus` domain enum is used directly in response DTOs — if a new internal status is added to the domain it would automatically appear in the API response. A dedicated `AlertStatusDto` gives independent control over what statuses are exposed externally. |
| **`DecisionStatus` enum** | Create a separate `DecisionStatus` enum with only `CLEARED` and `CONFIRMED_HIT` for the decide request DTO. Currently `AlertStatus` (all 4 values) is used in `DecideAlertRequest` — Swagger would show all 4 values as valid, misleading callers. A dedicated `DecisionStatus` enum makes the contract explicit both in code and in generated API documentation. |
| **API Documentation** | Add OpenAPI/Swagger to document all endpoints, request/response shapes, and timestamp formats. |
| **Pagination** | Add cursor-based pagination to `GET /alerts`. Currently all matching alerts are returned in one response — this works for small datasets but will cause performance issues and timeouts as alerts accumulate over time. Cursor-based pagination is preferred over offset-based because it handles concurrent inserts correctly — a new alert added mid-pagination won't cause duplicates or skipped records. The `ListAlertResponse` wrapper is already designed for this — adding `nextCursor` and `hasMore` fields requires no breaking change to existing callers. |
| **Observability** | Structured logging with trace/span IDs (OpenTelemetry), Micrometer metrics, health endpoints. |
| **Idempotency** | `Idempotency-Key` header on `POST /alerts` so upstream systems can safely retry without creating duplicate alerts. |
| **Rate limiting** | Per-tenant rate limiting to prevent noisy tenants affecting others. |
| **Audit trail** | Append-only event log of all status transitions for compliance auditability. |

**DTO Records example** — in production replace:
```java
// Current: explicit class for reviewer visibility
public class ListAlertResponse {
    private final List<AlertResponse> alerts;

    public ListAlertResponse(List<AlertResponse> alerts) {
        this.alerts = alerts;
    }

    public List<AlertResponse> getAlerts() { return alerts; }
}
```
With:
```java
// Production: concise, immutable record
public record ListAlertResponse(List<AlertResponse> alerts) {}
```
Java records auto-generate constructor, getters, `equals()`, `hashCode()`, and `toString()`.
