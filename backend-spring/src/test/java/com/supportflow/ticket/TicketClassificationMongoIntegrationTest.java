package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportflow.tenant.TenantStatus;
import com.supportflow.user.OperationalUserRole;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TicketClassificationMongoIntegrationTest {

    @Container
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7");

    @Container
    static final GenericContainer<?> aiService = new GenericContainer<>(
            new ImageFromDockerfile("supportflow-ai-service-test", false)
                    .withFileFromPath("Dockerfile", Path.of("../ai-service-python/Dockerfile"))
                    .withFileFromPath("pyproject.toml", Path.of("../ai-service-python/pyproject.toml"))
                    .withFileFromPath("app", Path.of("../ai-service-python/app"))
    )
            .withExposedPorts(8000)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    @DynamicPropertySource
    static void classificationProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getReplicaSetUrl);
        registry.add("supportflow.ai.classification.base-url",
                () -> "http://" + aiService.getHost() + ":" + aiService.getMappedPort(8000));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void ticketCreationCallsRealFastApiServiceAndAppliesSuccessfulClassification() {
        TenantResponse tenant = createTenant("AI Tenant", "ai-tenant");

        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenant.id() + "/tickets"),
                new TicketCreateRequest(
                        "Urgent billing payment failed",
                        "Ada Lovelace",
                        "ada@example.com",
                        "Urgent billing payment failed and I am frustrated",
                        null,
                        null,
                        null
                ),
                TicketResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        TicketResponse ticket = response.getBody();
        assertThat(ticket).isNotNull();
        assertThat(ticket.category()).isEqualTo("billing");
        assertThat(ticket.priority()).isIn(TicketPriority.HIGH, TicketPriority.URGENT);
        assertThat(ticket.classificationAttempts()).hasSize(1);
        TicketClassificationAttemptResponse attempt = ticket.classificationAttempts().getFirst();
        assertThat(attempt.status()).isEqualTo(TicketClassificationAttemptStatus.SUCCESS);
        assertThat(attempt.trigger()).isEqualTo(TicketClassificationTrigger.AUTO_ON_CREATE);
        assertThat(attempt.actorUserId()).isNull();
        assertThat(attempt.category()).isEqualTo("billing");
        assertThat(attempt.priority()).isEqualTo(ticket.priority());
        assertThat(attempt.urgency()).isEqualTo(TicketClassificationUrgency.HIGH);
        assertThat(attempt.sentiment()).isEqualTo(TicketClassificationSentiment.NEGATIVE);
        assertThat(attempt.confidence()).isNotNull().isBetween(0.0, 1.0);
        assertThat(attempt.classifierVersion()).isEqualTo("rules-v1");
        assertThat(ticket.history()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.eventType()).isEqualTo(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED);
                    assertThat(entry.classificationAttemptId()).isEqualTo(attempt.id());
                });
    }

    @Test
    void manualReanalysisAppendsSecondClassificationAttempt() {
        TenantResponse tenant = createTenant("Manual AI Tenant", "manual-ai-tenant");
        OperationalUserResponse actor = createUser(tenant.id(), "Tenant Admin", "admin@manual-ai.example",
                OperationalUserRole.TENANT_ADMIN);
        TicketResponse ticket = createTicket(tenant.id(), "Billing question", "Please explain my invoice charge");

        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenant.id() + "/tickets/" + ticket.id() + "/classification-attempts"),
                new ReanalyzeTicketRequest(actor.id()),
                TicketResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TicketResponse reanalyzed = response.getBody();
        assertThat(reanalyzed).isNotNull();
        assertThat(reanalyzed.classificationAttempts()).hasSize(2);
        assertThat(reanalyzed.classificationAttempts()).extracting(TicketClassificationAttemptResponse::trigger)
                .containsExactly(TicketClassificationTrigger.AUTO_ON_CREATE,
                        TicketClassificationTrigger.MANUAL_REANALYSIS);
        TicketClassificationAttemptResponse manualAttempt = reanalyzed.classificationAttempts().get(1);
        assertThat(manualAttempt.actorUserId()).isEqualTo(actor.id());
        assertThat(reanalyzed.history()).extracting(TicketHistoryEntryResponse::classificationAttemptId)
                .contains(manualAttempt.id());
    }

    @Test
    void crossTenantTicketDetailCannotReadClassificationAttempts() {
        TenantResponse tenantA = createTenant("AI Tenant A", "ai-tenant-a");
        TenantResponse tenantB = createTenant("AI Tenant B", "ai-tenant-b");
        TicketResponse ticket = createTicket(tenantA.id(), "Billing question", "Please explain my invoice charge");

        ResponseEntity<String> crossTenantResponse = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenantB.id() + "/tickets/" + ticket.id()),
                String.class);

        assertThat(crossTenantResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private TenantResponse createTenant(String name, String slug) {
        ResponseEntity<TenantResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants"),
                new TenantCreateRequest(name, slug, null),
                TenantResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private OperationalUserResponse createUser(String tenantId, String displayName, String email,
            OperationalUserRole role) {
        ResponseEntity<OperationalUserResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantId + "/users"),
                new OperationalUserCreateRequest(displayName, email, role),
                OperationalUserResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private TicketResponse createTicket(String tenantId, String subject, String message) {
        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantId + "/tickets"),
                new TicketCreateRequest(subject, "Ada Lovelace", "ada@example.com", message, null, null, null),
                TicketResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    record TenantCreateRequest(String name, String slug, String description) {
    }

    record TenantResponse(String id, TenantStatus status) {
    }

    record OperationalUserCreateRequest(String displayName, String email, OperationalUserRole role) {
    }

    record OperationalUserResponse(String id) {
    }

    record TicketCreateRequest(String subject, String customerName, String customerEmail, String customerMessage,
            String category, TicketPriority priority, String assigneeId) {
    }

    record ReanalyzeTicketRequest(String actorUserId) {
    }

    record TicketResponse(String id, String category, TicketPriority priority,
            List<TicketHistoryEntryResponse> history,
            List<TicketClassificationAttemptResponse> classificationAttempts) {
    }

    record TicketHistoryEntryResponse(TicketHistoryEventType eventType, String actorUserId,
            String classificationAttemptId, Instant occurredAt, List<TicketFieldChangeResponse> changes) {
    }

    record TicketFieldChangeResponse(String field, String oldValue, String newValue) {
    }

    record TicketClassificationAttemptResponse(String id, TicketClassificationAttemptStatus status,
            TicketClassificationTrigger trigger, String actorUserId, Instant requestedAt, Instant completedAt,
            String category, TicketClassificationUrgency urgency, TicketClassificationSentiment sentiment,
            TicketPriority priority, Double confidence, String classifierVersion, String errorCode,
            String errorMessage) {
    }
}
