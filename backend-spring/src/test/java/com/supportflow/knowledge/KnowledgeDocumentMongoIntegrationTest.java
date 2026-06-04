package com.supportflow.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportflow.tenant.TenantStatus;
import com.supportflow.user.OperationalUserRole;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class KnowledgeDocumentMongoIntegrationTest {

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

    @Test
    void crossTenantDocumentDetailUpdateArchiveAndRestoreReturn404() {
        TenantResponse tenantA = createTenant("Knowledge Tenant A", "knowledge-tenant-a");
        TenantResponse tenantB = createTenant("Knowledge Tenant B", "knowledge-tenant-b");
        OperationalUserResponse actorA = createUser(tenantA.id(), "Tenant A Admin",
                "knowledge-admin-a@example.com", OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse actorB = createUser(tenantB.id(), "Tenant B Admin",
                "knowledge-admin-b@example.com", OperationalUserRole.TENANT_ADMIN);
        KnowledgeDocumentResponse tenantADocument = createDocument(tenantA.id(), actorA.id(), "Tenant A FAQ");

        ResponseEntity<KnowledgeDocumentResponse> ownDetail = restTemplate.getForEntity(
                url(documentPath(tenantA.id(), tenantADocument.id())), KnowledgeDocumentResponse.class);
        ResponseEntity<String> crossTenantDetail = restTemplate.getForEntity(
                url(documentPath(tenantB.id(), tenantADocument.id())), String.class);
        ResponseEntity<String> crossTenantUpdate = patchDocumentError(tenantB.id(), tenantADocument.id(),
                new KnowledgeDocumentUpdateRequest(KnowledgeDocumentType.POLICY, "Cross update",
                        "Cross tenant content", "Cross tenant handbook", null, List.of("cross"),
                        null, null, actorB.id()));
        ResponseEntity<String> crossTenantArchive = patchActorError(tenantB.id(), tenantADocument.id(),
                "/archive", actorB.id());
        ResponseEntity<String> crossTenantRestore = patchActorError(tenantB.id(), tenantADocument.id(),
                "/restore", actorB.id());

        assertThat(ownDetail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ownDetail.getBody().id()).isEqualTo(tenantADocument.id());
        assertThat(crossTenantDetail.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossTenantUpdate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossTenantArchive.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossTenantRestore.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void crossTenantActorCannotCreateUpdateArchiveOrRestoreTenantDocument() {
        TenantResponse tenantA = createTenant("Knowledge Actor Tenant A", "knowledge-actor-tenant-a");
        TenantResponse tenantB = createTenant("Knowledge Actor Tenant B", "knowledge-actor-tenant-b");
        OperationalUserResponse actorA = createUser(tenantA.id(), "Tenant A Admin",
                "knowledge-actor-a@example.com", OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse actorB = createUser(tenantB.id(), "Tenant B Admin",
                "knowledge-actor-b@example.com", OperationalUserRole.TENANT_ADMIN);
        KnowledgeDocumentResponse tenantADocument = createDocument(tenantA.id(), actorA.id(), "Original FAQ");

        ResponseEntity<String> crossTenantCreate = restTemplate.postForEntity(
                url(documentsPath(tenantA.id())),
                new KnowledgeDocumentCreateRequest(KnowledgeDocumentType.FAQ, "Invalid actor FAQ",
                        "This create should fail.", "Support handbook", null, List.of("billing"),
                        null, null, actorB.id()),
                String.class);
        ResponseEntity<String> crossTenantUpdate = patchDocumentError(tenantA.id(), tenantADocument.id(),
                new KnowledgeDocumentUpdateRequest(null, "Updated by invalid actor",
                        "Mutated content", null, null, null, null, null, actorB.id()));
        ResponseEntity<String> crossTenantArchive = patchActorError(tenantA.id(), tenantADocument.id(),
                "/archive", actorB.id());
        ResponseEntity<String> crossTenantRestore = patchActorError(tenantA.id(), tenantADocument.id(),
                "/restore", actorB.id());
        ResponseEntity<KnowledgeDocumentResponse> detail = restTemplate.getForEntity(
                url(documentPath(tenantA.id(), tenantADocument.id())), KnowledgeDocumentResponse.class);
        ResponseEntity<KnowledgeDocumentResponse[]> list = restTemplate.getForEntity(
                url(documentsPath(tenantA.id())), KnowledgeDocumentResponse[].class);

        assertThat(crossTenantCreate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossTenantUpdate.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossTenantArchive.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossTenantRestore.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(detail.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody().title()).isEqualTo("Original FAQ");
        assertThat(detail.getBody().content()).isEqualTo("Customers can request a refund within 30 days.");
        assertThat(detail.getBody().status()).isEqualTo(KnowledgeDocumentStatus.ACTIVE);
        assertThat(detail.getBody().archivedAt()).isNull();
        assertThat(detail.getBody().archivedByUserId()).isNull();
        assertThat(list.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(list.getBody()).hasSize(1);
    }

    @Test
    void tenantKnowledgeDocumentListsDoNotMixDocuments() {
        TenantResponse tenantA = createTenant("Knowledge List Tenant A", "knowledge-list-tenant-a");
        TenantResponse tenantB = createTenant("Knowledge List Tenant B", "knowledge-list-tenant-b");
        OperationalUserResponse actorA = createUser(tenantA.id(), "Tenant A Admin",
                "knowledge-list-a@example.com", OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse actorB = createUser(tenantB.id(), "Tenant B Admin",
                "knowledge-list-b@example.com", OperationalUserRole.TENANT_ADMIN);
        KnowledgeDocumentResponse tenantADocument = createDocument(tenantA.id(), actorA.id(), "Tenant A FAQ");
        KnowledgeDocumentResponse tenantBDocument = createDocument(tenantB.id(), actorB.id(), "Tenant B FAQ");

        ResponseEntity<KnowledgeDocumentResponse[]> tenantAList = restTemplate.getForEntity(
                url(documentsPath(tenantA.id())), KnowledgeDocumentResponse[].class);
        ResponseEntity<KnowledgeDocumentResponse[]> tenantBList = restTemplate.getForEntity(
                url(documentsPath(tenantB.id())), KnowledgeDocumentResponse[].class);

        assertThat(tenantAList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tenantBList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tenantAList.getBody()).extracting(KnowledgeDocumentResponse::id)
                .containsExactly(tenantADocument.id())
                .doesNotContain(tenantBDocument.id());
        assertThat(tenantBList.getBody()).extracting(KnowledgeDocumentResponse::id)
                .containsExactly(tenantBDocument.id())
                .doesNotContain(tenantADocument.id());
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

    private KnowledgeDocumentResponse createDocument(String tenantId, String actorUserId, String title) {
        ResponseEntity<KnowledgeDocumentResponse> response = restTemplate.postForEntity(
                url(documentsPath(tenantId)),
                new KnowledgeDocumentCreateRequest(KnowledgeDocumentType.FAQ, title,
                        "Customers can request a refund within 30 days.", "Support handbook",
                        null, List.of("billing", "refunds"), null, null, actorUserId),
                KnowledgeDocumentResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    private ResponseEntity<String> patchDocumentError(String tenantId, String documentId,
            KnowledgeDocumentUpdateRequest request) {
        return restTemplate.exchange(
                url(documentPath(tenantId, documentId)),
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                String.class);
    }

    private ResponseEntity<String> patchActorError(String tenantId, String documentId, String suffix,
            String actorUserId) {
        return restTemplate.exchange(
                url(documentPath(tenantId, documentId) + suffix),
                HttpMethod.PATCH,
                new HttpEntity<>(new KnowledgeDocumentActorRequest(actorUserId)),
                String.class);
    }

    private String documentsPath(String tenantId) {
        return "/api/v1/tenants/" + tenantId + "/knowledge-documents";
    }

    private String documentPath(String tenantId, String documentId) {
        return documentsPath(tenantId) + "/" + documentId;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    record TenantCreateRequest(String name, String slug, String description) {
    }

    record TenantUpdateRequest(String name, String description, TenantStatus status) {
    }

    record TenantResponse(String id, TenantStatus status) {
    }

    record OperationalUserCreateRequest(String displayName, String email, OperationalUserRole role) {
    }

    record OperationalUserResponse(String id) {
    }

    record KnowledgeDocumentCreateRequest(KnowledgeDocumentType type, String title, String content,
            String sourceLabel, String sourceUrl, List<String> tags, Instant effectiveFrom, Instant effectiveTo,
            String actorUserId) {
    }

    record KnowledgeDocumentUpdateRequest(KnowledgeDocumentType type, String title, String content,
            String sourceLabel, String sourceUrl, List<String> tags, Instant effectiveFrom, Instant effectiveTo,
            String actorUserId) {
    }

    record KnowledgeDocumentActorRequest(String actorUserId) {
    }

    record KnowledgeDocumentResponse(String id, String tenantId, KnowledgeDocumentType type,
            KnowledgeDocumentStatus status, String title, String content, String sourceLabel, String sourceUrl,
            List<String> tags, Instant effectiveFrom, Instant effectiveTo, String contentHash,
            String createdByUserId, String updatedByUserId, Instant createdAt, Instant updatedAt, Instant archivedAt,
            String archivedByUserId) {
    }
}
