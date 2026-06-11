# Phase 4: AI Classification Integration - Pattern Map

**Mapped:** 2026-06-11
**Status:** Ready for planning

## New Files And Closest Analogs

| Planned File | Role | Closest Existing Analog | Pattern To Reuse |
|--------------|------|-------------------------|------------------|
| `ai-service-python/pyproject.toml` | Python package and test config | `backend-spring/pom.xml` | Pin runtime/test dependencies in a single project config; expose one deterministic test command. |
| `ai-service-python/Dockerfile` | Containerized AI service | `backend-spring/Dockerfile` if present during execution; otherwise `docker-compose.yml` backend service | Build one service image with a fixed port and no external secrets. |
| `ai-service-python/app/main.py` | FastAPI routes | `TicketController.java` | Keep versioned routes explicit and response DTOs strict. |
| `ai-service-python/app/models.py` | Pydantic contract | `TicketController` nested request/response records | Request/response types are the contract consumed by tests and docs. |
| `ai-service-python/app/classifier.py` | Deterministic rules | `TicketStatusTransitionPolicy.java` | Put rule logic in a small pure component with direct unit tests. |
| `ai-service-python/tests/test_classifier_rules.py` | Rule tests | `TicketStatusTransitionPolicyTest.java` | Parametrize cases and assert exact enum/result values. |
| `ai-service-python/tests/test_classification_api.py` | API contract tests | `TicketApiIntegrationTest.java` | Exercise HTTP request/response shape, status codes, and validation errors. |
| `backend-spring/src/main/java/com/supportflow/ai/*` | Backend AI client/config | Existing service/controller packages under `ticket`, `knowledge`, `user` | Keep AI client isolated from ticket domain but inject it into `TicketService`. |
| `TicketClassificationAttempt.java` | Ticket classification artifact | `TicketHistoryEntry.java` | Embedded, timestamped, DTO-friendly domain object with null-safe collection handling if needed. |
| `TicketHistoryEntry.java` changes | Link history to classification | `TicketHistoryEntry.java` current field-change pattern | Preserve existing constructor compatibility and add optional `classificationAttemptId`. |
| `TicketService.java` changes | Auto/manual workflow | `updateWorkflowMetadata` | Reuse category/priority field-change logic, but use new AI event type and no human actor for auto trigger. |
| `TicketController.java` changes | Manual re-analysis and response artifacts | Existing ticket create/detail/workflow endpoints | Keep route under `/api/v1/tenants/{tenantId}/tickets/{ticketId}` and embed artifacts in `TicketResponse`. |
| `TicketClassificationMongoIntegrationTest.java` | Docker-backed integration | `TenantWorkflowMongoIntegrationTest.java` and `KnowledgeDocumentMongoIntegrationTest.java` | Use `@SpringBootTest(RANDOM_PORT)`, `TestRestTemplate`, MongoDB Testcontainers, and same-tenant seeding helpers. |
| `docs/sdd/phase-04-ai-classification-integration-api.md` | API contract docs | `docs/sdd/phase-03-knowledge-base-core-api.md` | Document endpoint paths, request/response fields, failure behavior, tenant isolation, and exclusions. |

## Existing Code Excerpts To Respect

### Tenant-Scoped Ticket Response

`TicketController` currently returns ticket detail with embedded history:

```java
public record TicketResponse(
        String id,
        String tenantId,
        String subject,
        String customerName,
        String customerEmail,
        String customerMessage,
        TicketStatus status,
        String category,
        TicketPriority priority,
        String assigneeId,
        Instant createdAt,
        Instant updatedAt,
        List<TicketHistoryEntry> history
) {
```

Phase 4 should add classification attempts to this response without removing current fields.

### Workflow Metadata Mutation Pattern

`TicketService.updateWorkflowMetadata` already builds `TicketFieldChange` values for `priority` and `category`, updates `updatedAt`, and appends `WORKFLOW_METADATA_CHANGED` history. AI-applied changes should reuse the field-change shape but use `AI_CLASSIFICATION_APPLIED` and a `classificationAttemptId` link.

### Docker-Backed Test Pattern

Phase 2/3 Mongo tests use:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TenantWorkflowMongoIntegrationTest {
    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");
}
```

Phase 4 should extend this pattern with a FastAPI container, not replace Mongo-backed persistence coverage.

## Data Flow

1. Client creates ticket through Spring: `POST /api/v1/tenants/{tenantId}/tickets`.
2. `TicketService.createTicket` persists ticket baseline, calls AI classification synchronously, and records success/failure attempt.
3. Successful attempt updates ticket `category` and `priority`, appends `AI_CLASSIFICATION_APPLIED` history linked to `classificationAttemptId`, and returns ticket response with attempts.
4. Failed attempt records error metadata, leaves ticket category/priority and workflow history unchanged, and returns ticket response with failed artifact for auto-create.
5. Manual route calls the same workflow for existing non-closed tickets after validating `actorUserId`.

## Planning Constraints

- Do not add LLM providers, prompts, embeddings, RAG, async jobs, Redis orchestration, frontend, auth, full RBAC, or draft generation.
- Keep Python service stateless.
- Keep backend persistence tenant-scoped.
- Keep failed attempts visible but non-mutating.
- Keep successful AI-applied updates traceable to `classificationAttemptId`.
