# Phase 3: Knowledge Base Core - Pattern Mapping

**Generated:** 2026-06-04
**Phase:** 03 - Knowledge Base Core
**Purpose:** Identify the closest existing code patterns to copy when planning and implementing tenant-scoped knowledge document management.

## 1. Target Files And Closest Analogs

### Likely New Backend Files

| Target file | Role | Data flow | Closest analogs | Pattern to copy |
|-------------|------|-----------|-----------------|-----------------|
| `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocument.java` | Mongo document | Controller response reads service-returned document; repository persists tenant-owned records | `Ticket.java`, `OperationalUser.java` | Java bean, `@Document`, `@Id`, `@Indexed tenantId`, enum fields, `Instant createdAt/updatedAt`, explicit getters/setters |
| `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentType.java` | Enum | Request binding, stored enum, response enum | `TicketPriority.java`, `OperationalUserRole.java` | Small plain enum with locked values only |
| `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentStatus.java` | Enum | Request filter/status, stored enum, response enum | `OperationalUserStatus.java`, `TicketStatus.java` | Small plain enum; Phase 3 values are only `ACTIVE`, `ARCHIVED` |
| `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentRepository.java` | Tenant-scoped persistence | Service queries by tenant and id; list by tenant then filters in Java | `TicketRepository.java`, `OperationalUserRepository.java` | Extend `MongoRepository<KnowledgeDocument, String>`; include `findByTenantId` and `findByTenantIdAndId` |
| `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentService.java` | Domain rules and tenant isolation | Controller builds commands/filters; service validates tenant/actor and saves document | `TicketService.java`, `OperationalUserService.java`, `TenantService.java` | Read calls `tenantService.getTenant`; mutations call `requireActiveTenant`; actor mutations call `validateActiveActor`; cross-tenant document lookup uses tenant id |
| `backend-spring/src/main/java/com/supportflow/knowledge/KnowledgeDocumentController.java` | REST API | HTTP request -> controller record -> service command/filter -> response record | `TicketController.java`, `OperationalUserController.java` | Tenant path mapping, nested request/response records, `@Valid`, `@RequestParam` filters, OpenAPI annotations, create `Location` |

### Likely New/Modified Test And Documentation Files

| Target file | Role | Data flow | Closest analogs | Pattern to copy |
|-------------|------|-----------|-----------------|-----------------|
| `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentServiceTest.java` | Unit tests | Mock repositories/services; assert service rules and saves | `TicketStatusTransitionPolicyTest` plus service patterns in `TicketService` | JUnit Jupiter, Mockito, AssertJ, direct service invocation |
| `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentApiIntegrationTest.java` | WebMvc slice tests | Mock service; exercise JSON request/response and validation | `TicketApiIntegrationTest.java`, `OperationalUserApiIntegrationTest.java` | `@WebMvcTest(controllers = {Controller.class, GlobalExceptionHandler.class})`, `MockMvc`, `@MockitoBean` service |
| `backend-spring/src/test/java/com/supportflow/knowledge/KnowledgeDocumentMongoIntegrationTest.java` | Docker-backed HTTP integration | Real Spring app and Mongo; use HTTP to prove tenant isolation | `TenantWorkflowMongoIntegrationTest.java` | `@SpringBootTest(RANDOM_PORT)`, `MongoDBContainer("mongo:7")`, `TestRestTemplate`, helper records |
| `backend-spring/src/test/java/com/supportflow/common/OpenApiDocumentationTest.java` | OpenAPI route assertions | `/v3/api-docs` contains route strings | Existing `OpenApiDocumentationTest.java` | Add mocked `KnowledgeDocumentService`; assert knowledge route paths |
| `backend-spring/src/test/java/com/supportflow/FoundationVerificationTest.java` or focused equivalent | Reflection verification | Assert controller methods expose Spring mapping annotations and test classes exist | Existing `FoundationVerificationTest.java` | Reflect on controller method signatures and mapping annotations |
| `docs/sdd/phase-03-knowledge-base-core-api.md` | SDD/API contract | Human contract for routes, fields, behavior, exclusions | `docs/sdd/phase-02-tenant-workflow-core-api.md` | Endpoint sections, request fields, validation, inactive tenant behavior, explicit exclusions |

## 2. Controller Patterns To Copy

Copy the tenant-scoped controller shape from `TicketController`:

```java
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
@Tag(name = "Tickets", description = "Tenant-scoped ticket foundation APIs")
public class TicketController {
```

Phase 3 should adapt this to:

```java
@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/knowledge-documents")
@Tag(name = "Knowledge Documents", description = "Tenant-scoped knowledge document APIs")
public class KnowledgeDocumentController {
```

Copy create response semantics from `TicketController.createTicket`:

```java
@PostMapping
@Operation(summary = "Create a tenant-scoped ticket")
public ResponseEntity<TicketResponse> createTicket(@PathVariable String tenantId,
        @Valid @RequestBody CreateTicketRequest request) {
    Ticket ticket = ticketService.createTicket(tenantId, new TicketService.CreateTicketCommand(...));
    return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId + "/tickets/" + ticket.getId()))
            .body(TicketResponse.from(ticket));
}
```

Use the same pattern for `POST /api/v1/tenants/{tenantId}/knowledge-documents`, with a `Location` header ending in `/knowledge-documents/{documentId}`.

Copy list filter binding from `TicketController.listTickets`:

```java
@GetMapping
@Operation(summary = "List tenant-scoped tickets")
public List<TicketResponse> listTickets(
        @PathVariable String tenantId,
        @RequestParam(required = false) TicketStatus status,
        @RequestParam(required = false) TicketPriority priority,
        @RequestParam(required = false) String assigneeId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo
) {
```

Adapt to exact Phase 3 filters:

- `KnowledgeDocumentType type`
- `KnowledgeDocumentStatus status`
- `String tag`
- `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant activeAt`

Copy nested record style and Jakarta validation:

```java
public record UpdateTicketWorkflowRequest(
        @NotBlank String actorUserId,
        String assigneeId,
        TicketPriority priority,
        String category
) {
}
```

Knowledge mutation records should require `actorUserId`; create should also require `type`, `title`, `content`, and `sourceLabel`. Keep response records inside the controller and map from the Mongo document through a static `from` method, as in:

```java
static TicketResponse from(Ticket ticket) {
    return new TicketResponse(
            ticket.getId(),
            ticket.getTenantId(),
            ...
    );
}
```

## 3. Service/Repository Tenant Isolation Patterns To Copy

Use the Phase 2 read-vs-mutation split exactly.

Reads use `TenantService.getTenant`:

```java
public List<Ticket> listTickets(String tenantId, TicketFilters filters) {
    tenantService.getTenant(tenantId);
    List<Ticket> tickets = new ArrayList<>(ticketRepository.findByTenantId(tenantId));
    return tickets.stream()
            ...
            .toList();
}

public Ticket getTicket(String tenantId, String ticketId) {
    tenantService.getTenant(tenantId);
    return ticketRepository.findByTenantIdAndId(tenantId, ticketId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
}
```

Knowledge list/detail should call `tenantService.getTenant(tenantId)`, then use `knowledgeDocumentRepository.findByTenantId(...)` or `findByTenantIdAndId(...)`. Never use `findById(documentId)` for tenant-scoped routes.

Mutations use `TenantService.requireActiveTenant`:

```java
public Tenant requireActiveTenant(String tenantId) {
    Tenant tenant = getTenant(tenantId);
    if (tenant.getStatus() == TenantStatus.INACTIVE) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant is inactive");
    }
    return tenant;
}
```

Knowledge create, update, archive, and restore must call `requireActiveTenant` before persisting changes.

Actor-attributed mutations use `OperationalUserService.validateActiveActor`:

```java
public OperationalUser validateActiveActor(String tenantId, String actorUserId) {
    OperationalUser user = findTenantUser(tenantId, actorUserId);
    if (user.getStatus() != OperationalUserStatus.ACTIVE) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Operational user is inactive");
    }
    return user;
}

private OperationalUser findTenantUser(String tenantId, String userId) {
    return operationalUserRepository.findByTenantIdAndId(tenantId, userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operational user not found"));
}
```

Knowledge mutations should validate only active same-tenant actor existence. Do not copy `validateActiveSupportAgent`, because Phase 3 decisions require active tenant-local actor validation, not role enforcement.

Copy repository signatures from tenant-scoped repositories:

```java
public interface TicketRepository extends MongoRepository<Ticket, String> {
    List<Ticket> findByTenantId(String tenantId);
    Optional<Ticket> findByTenantIdAndId(String tenantId, String id);
}
```

Recommended Phase 3 repository minimum:

```java
public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {
    List<KnowledgeDocument> findByTenantId(String tenantId);
    Optional<KnowledgeDocument> findByTenantIdAndId(String tenantId, String id);
}
```

Filter in Java for MVP consistency with `TicketService.listTickets`; add exact query helpers only if they stay tenant-scoped.

## 4. Mongo Document And Enum Patterns To Copy

Copy the explicit Mongo document style from `Ticket`:

```java
@Document("tickets")
public class Ticket {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private TicketStatus status;
    private TicketPriority priority;
    private Instant createdAt;
    private Instant updatedAt;
```

Knowledge should follow this shape:

```java
@Document("knowledge_documents")
public class KnowledgeDocument {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private KnowledgeDocumentType type;
    private KnowledgeDocumentStatus status;
    private String title;
    private String content;
    private String sourceLabel;
    private String sourceUrl;
    private List<String> tags;
    private Instant effectiveFrom;
    private Instant effectiveTo;
    private String contentHash;
    private String createdByUserId;
    private String updatedByUserId;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant archivedAt;
    private String archivedByUserId;
```

Copy enum minimalism:

```java
public enum OperationalUserStatus {
    ACTIVE,
    INACTIVE
}
```

Phase 3 enums must stay locked to the context decisions:

```java
public enum KnowledgeDocumentType {
    FAQ,
    POLICY
}

public enum KnowledgeDocumentStatus {
    ACTIVE,
    ARCHIVED
}
```

Do not introduce Lombok, builders, JPA annotations, Kotlin data classes, custom converters, text indexes, or vector/index infrastructure. Existing code uses explicit Java beans and Spring Data Mongo annotations only.

For list fields, copy the null-safe defensive style from `Ticket.getHistory` only if the field can be mutated in place:

```java
public List<TicketHistoryEntry> getHistory() {
    if (history == null) {
        history = new ArrayList<>();
    }
    return history;
}
```

For `tags`, prefer service-level normalization to a non-null immutable-ish stored list. If the document setter copies the list, follow `Ticket.setHistory`:

```java
public void setHistory(List<TicketHistoryEntry> history) {
    this.history = history == null ? new ArrayList<>() : new ArrayList<>(history);
}
```

## 5. WebMvc And Testcontainers Test Patterns To Copy

### WebMvc Slice Tests

Copy the slice test scaffold:

```java
@WebMvcTest(controllers = {TicketController.class, GlobalExceptionHandler.class})
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;
```

Knowledge version:

```java
@WebMvcTest(controllers = {KnowledgeDocumentController.class, GlobalExceptionHandler.class})
class KnowledgeDocumentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeDocumentService knowledgeDocumentService;
```

Copy JSON request style:

```java
mockMvc.perform(post("/api/v1/tenants/tenant-1/tickets")
        .contentType("application/json")
        .content("""
                {
                  "subject": "Cannot log in",
                  "customerName": "Ada Lovelace",
                  "customerEmail": "ada@example.com",
                  "customerMessage": "Login fails after reset",
                  "priority": "HIGH",
                  "assigneeId": "agent-7"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.tenantId").value("tenant-1"));
```

Knowledge WebMvc tests should prove:

- create returns 201 and includes `tenantId`, `status`, `contentHash`, `createdByUserId`, `updatedByUserId`
- missing required create fields return 400 through `GlobalExceptionHandler`
- list binds `type`, `status`, `tag`, and `activeAt`
- detail uses tenant/document path
- update accepts partial body plus `actorUserId`
- archive and restore require `actorUserId`
- service `ResponseStatusException` maps to expected HTTP status

### Testcontainers HTTP Integration Tests

Copy the Mongo integration scaffold:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TenantWorkflowMongoIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void mongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
```

Copy HTTP helper style:

```java
private String url(String path) {
    return "http://localhost:" + port + path;
}
```

Copy mutation helpers with `restTemplate.exchange`:

```java
return restTemplate.exchange(
        url("/api/v1/tenants/" + tenantId + "/tickets/" + ticketId + "/workflow"),
        HttpMethod.PATCH,
        new HttpEntity<>(request),
        TicketResponse.class);
```

Knowledge Testcontainers coverage should exercise real HTTP calls for:

- tenant A list/detail never expose tenant B documents
- tenant A cannot read, update, archive, or restore tenant B document ids through tenant A paths
- cross-tenant actors return 404 and do not mutate tenant A documents
- inactive actors return 409 and do not mutate
- inactive tenants allow list/detail but reject create/update/archive/restore
- archive hides from default list but appears with `status=ARCHIVED`
- restore returns the document to default active list
- archived content updates are rejected without changing `content` or `contentHash`
- exact filters combine for `type`, `status`, `tag`, and `activeAt`

## 6. OpenAPI/SDD Documentation Patterns To Copy

Extend `OpenApiDocumentationTest` in the same route-string assertion style:

```java
mockMvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.info.title").value("SupportFlow AI Backend API"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString(
                "/api/v1/tenants/{tenantId}/tickets/{ticketId}/workflow")));
```

Phase 3 additions:

```java
.andExpect(content().string(org.hamcrest.Matchers.containsString(
        "/api/v1/tenants/{tenantId}/knowledge-documents")))
.andExpect(content().string(org.hamcrest.Matchers.containsString(
        "/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}")))
.andExpect(content().string(org.hamcrest.Matchers.containsString(
        "/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/archive")))
.andExpect(content().string(org.hamcrest.Matchers.containsString(
        "/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/restore")));
```

Also add a mock bean for the new service in this Spring Boot OpenAPI test:

```java
@MockitoBean
private KnowledgeDocumentService knowledgeDocumentService;
```

Copy reflection verification from `FoundationVerificationTest`:

```java
Method updateWorkflow = TicketController.class.getMethod("updateWorkflow", String.class, String.class,
        TicketController.UpdateTicketWorkflowRequest.class);

assertThat(updateWorkflow.isAnnotationPresent(PatchMapping.class)).isTrue();
```

Phase 3 should reflect on `KnowledgeDocumentController` create/list/detail/update/archive/restore methods and assert `@PostMapping`, `@GetMapping`, and `@PatchMapping` annotations. Add a class-existence assertion for `com.supportflow.knowledge.KnowledgeDocumentMongoIntegrationTest` or the chosen integration test class name.

Create `docs/sdd/phase-03-knowledge-base-core-api.md` following the Phase 2 SDD shape:

```markdown
## Ticket Workflow Endpoints

### PATCH /api/v1/tenants/{tenantId}/tickets/{ticketId}/status

Request fields:

- `status`: required target status.
- `actorUserId`: required tenant-local operational user id for attribution.

Validation:

- The tenant must be `ACTIVE`.
- The ticket must exist under the requested tenant.
- `actorUserId` must identify an `ACTIVE` operational user in the same tenant.
```

Knowledge SDD should include:

- all six routes: create, list, detail, update, archive, restore
- request and response fields
- enum values: `FAQ`, `POLICY`, `ACTIVE`, `ARCHIVED`
- backend-owned `contentHash`
- default active-only list behavior
- archive/restore semantics and no destructive delete
- inactive tenant read/mutation split
- tenant isolation and cross-tenant 404 behavior
- explicit exclusions for auth, frontend UI, text search, Mongo text indexes, embeddings, vector search, RAG, ranking, immutable revisions, and AI generation

## 7. Planner Cautions: Patterns That Must Not Be Copied Blindly

1. Do not copy ticket lifecycle semantics. `TicketStatusTransitionPolicy`, ticket history events, and closed-ticket edit rules are ticket-specific. Knowledge documents only have `ACTIVE` and `ARCHIVED`, plus the archived-content-update rule.

2. Do not copy `validateActiveSupportAgent`. Phase 3 requires active tenant-local actor validation through `validateActiveActor`; it does not introduce RBAC or role checks.

3. Do not copy `OperationalUserService.createUser` inactive-tenant behavior. It calls `tenantService.getTenant`, but knowledge create/update/archive/restore must call `requireActiveTenant`.

4. Do not expose repository `findById` in service code. All knowledge document tenant-scoped lookup must use `findByTenantIdAndId(tenantId, documentId)` so cross-tenant access returns 404.

5. Do not return Mongo documents directly from controllers. Existing controllers expose response records and `from(...)` mappers.

6. Do not add text search, keyword contains search, Mongo text indexes, embeddings, vector indexes, or RAG evidence retrieval. Phase 3 list filters are exact metadata filters only.

7. Do not add destructive delete. Archive/restore is the lifecycle behavior; content must remain traceable for later evidence workflows.

8. Do not let clients provide `contentHash`. Compute it in the backend from the exact stored normalized content and recompute only when content changes.

9. Do not implicitly filter by effective dates when `activeAt` is absent. Status and effective window are independent; default list behavior only excludes archived documents.

10. Do not blindly copy no-op update behavior without documenting it. `TicketService.updateWorkflowMetadata` returns the unchanged ticket when no fields changed; Phase 3 may copy that, but the SDD and tests must lock the behavior.

11. Do not add extra enum values such as `DRAFT`, `PUBLISHED`, `RETIRED`, `DELETED`, or `ALL`. Phase 3 decisions lock the type/status sets.

12. Do not make archive metadata ambiguous. If `archivedAt` and `archivedByUserId` are added, decide whether restore clears or preserves them and document/test the result.

13. Do not over-tighten optional `sourceUrl` validation. `sourceLabel` is required; `sourceUrl` is optional and should be validated conservatively.

14. Do not skip API documentation assertions. `OpenApiDocumentationTest` currently mocks all controller services to load docs without Mongo; add the knowledge service mock when the controller is introduced.

## PATTERN MAPPING COMPLETE
