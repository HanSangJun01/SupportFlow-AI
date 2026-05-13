package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportflow.tenant.TenantStatus;
import com.supportflow.user.OperationalUserRole;
import com.supportflow.user.OperationalUserStatus;
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

    @Test
    void inactiveTenantAllowsReadsButRejectsTicketWorkflowMutations() {
        TenantResponse tenant = createTenant("Inactive Tenant", "inactive-tenant");
        OperationalUserResponse actor = createUser(tenant.id(), "Tenant Admin", "admin@inactive.example",
                OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse assignee = createUser(tenant.id(), "Support Agent", "agent@inactive.example",
                OperationalUserRole.SUPPORT_AGENT);
        TicketResponse ticket = createTicket(tenant.id());

        ResponseEntity<TenantResponse> inactiveResponse = restTemplate.exchange(
                url("/api/v1/tenants/" + tenant.id()),
                HttpMethod.PATCH,
                new HttpEntity<>(new TenantUpdateRequest(null, null, TenantStatus.INACTIVE)),
                TenantResponse.class);
        assertThat(inactiveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(inactiveResponse.getBody().status()).isEqualTo(TenantStatus.INACTIVE);

        ResponseEntity<TicketResponse[]> listResponse = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenant.id() + "/tickets"), TicketResponse[].class);
        ResponseEntity<TicketResponse> detailResponse = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenant.id() + "/tickets/" + ticket.id()), TicketResponse.class);
        ResponseEntity<TicketResponse> createResponse = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenant.id() + "/tickets"),
                new TicketCreateRequest("New issue", "Grace Hopper", "grace@example.com", "Cannot submit form",
                        null, null, null),
                TicketResponse.class);
        ResponseEntity<TicketResponse> statusResponse = patchStatus(tenant.id(), ticket.id(), TicketStatus.TRIAGED,
                actor.id());
        ResponseEntity<TicketResponse> workflowResponse = patchWorkflow(tenant.id(), ticket.id(),
                new TicketWorkflowUpdateRequest(actor.id(), assignee.id(), TicketPriority.HIGH, "technical"));

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).hasSize(1);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody().id()).isEqualTo(ticket.id());
        assertThat(createResponse.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST);
        assertThat(statusResponse.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST);
        assertThat(workflowResponse.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST);
    }

    @Test
    void sameTenantSupportAgentAssignmentAppendsWorkflowHistory() {
        TenantResponse tenant = createTenant("Workflow Tenant", "workflow-tenant");
        OperationalUserResponse actor = createUser(tenant.id(), "Tenant Admin", "admin@workflow.example",
                OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse assignee = createUser(tenant.id(), "Support Agent", "agent@workflow.example",
                OperationalUserRole.SUPPORT_AGENT);
        TicketResponse ticket = createTicket(tenant.id());

        ResponseEntity<TicketResponse> response = patchWorkflow(tenant.id(), ticket.id(),
                new TicketWorkflowUpdateRequest(actor.id(), assignee.id(), TicketPriority.HIGH, "technical"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().assigneeId()).isEqualTo(assignee.id());
        assertThat(response.getBody().priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(response.getBody().category()).isEqualTo("technical");
        assertThat(response.getBody().history()).hasSize(1);
        assertThat(response.getBody().history().getFirst().eventType())
                .isEqualTo(TicketHistoryEventType.WORKFLOW_METADATA_CHANGED);
        assertThat(response.getBody().history().getFirst().actorUserId()).isEqualTo(actor.id());
        assertThat(response.getBody().history().getFirst().changes())
                .extracting(TicketFieldChangeResponse::field)
                .containsExactly("assigneeId", "priority", "category");
    }

    @Test
    void ticketCreationValidatesTenantLocalSupportAgentAssignee() {
        TenantResponse tenantA = createTenant("Create Tenant A", "create-tenant-a");
        TenantResponse tenantB = createTenant("Create Tenant B", "create-tenant-b");
        OperationalUserResponse sameTenantAgent = createUser(tenantA.id(), "Tenant A Agent",
                "create-agent@tenant-a.example", OperationalUserRole.SUPPORT_AGENT);
        OperationalUserResponse crossTenantAgent = createUser(tenantB.id(), "Tenant B Agent",
                "create-agent@tenant-b.example", OperationalUserRole.SUPPORT_AGENT);
        OperationalUserResponse tenantAdmin = createUser(tenantA.id(), "Tenant A Admin",
                "create-admin@tenant-a.example", OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse inactiveAgent = createUser(tenantA.id(), "Inactive Create Agent",
                "inactive-create@tenant-a.example", OperationalUserRole.SUPPORT_AGENT);
        updateUserStatus(tenantA.id(), inactiveAgent.id(), OperationalUserStatus.INACTIVE);

        ResponseEntity<TicketResponse> validResponse = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantA.id() + "/tickets"),
                new TicketCreateRequest("Assigned issue", "Ada Lovelace", "ada@example.com", "Cannot log in",
                        null, null, sameTenantAgent.id()),
                TicketResponse.class);
        ResponseEntity<TicketResponse> crossTenantResponse = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantA.id() + "/tickets"),
                new TicketCreateRequest("Cross tenant issue", "Grace Hopper", "grace@example.com", "Cannot submit",
                        null, null, crossTenantAgent.id()),
                TicketResponse.class);
        ResponseEntity<TicketResponse> wrongRoleResponse = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantA.id() + "/tickets"),
                new TicketCreateRequest("Wrong role issue", "Katherine Johnson", "katherine@example.com",
                        "Cannot export", null, null, tenantAdmin.id()),
                TicketResponse.class);
        ResponseEntity<TicketResponse> inactiveResponse = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantA.id() + "/tickets"),
                new TicketCreateRequest("Inactive assignee issue", "Dorothy Vaughan", "dorothy@example.com",
                        "Cannot upload", null, null, inactiveAgent.id()),
                TicketResponse.class);

        assertThat(validResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(validResponse.getBody().assigneeId()).isEqualTo(sameTenantAgent.id());
        assertThat(crossTenantResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(wrongRoleResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(inactiveResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void crossTenantActorFailsStatusAndWorkflowUpdatesWithoutMutation() {
        TenantResponse tenantA = createTenant("Actor Tenant A", "actor-tenant-a");
        TenantResponse tenantB = createTenant("Actor Tenant B", "actor-tenant-b");
        OperationalUserResponse crossTenantActor = createUser(tenantB.id(), "Other Actor",
                "actor@tenant-b.example", OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse sameTenantAssignee = createUser(tenantA.id(), "Tenant A Agent",
                "agent@tenant-a.example", OperationalUserRole.SUPPORT_AGENT);
        TicketResponse ticket = createTicket(tenantA.id());

        ResponseEntity<TicketResponse> crossTenantStatusResponse = patchStatus(tenantA.id(), ticket.id(),
                TicketStatus.TRIAGED, crossTenantActor.id());
        ResponseEntity<TicketResponse> crossTenantWorkflowResponse = patchWorkflow(tenantA.id(), ticket.id(),
                new TicketWorkflowUpdateRequest(crossTenantActor.id(), sameTenantAssignee.id(), TicketPriority.URGENT,
                        "technical"));
        ResponseEntity<TicketResponse> detailResponse = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenantA.id() + "/tickets/" + ticket.id()), TicketResponse.class);

        assertThat(crossTenantStatusResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossTenantWorkflowResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody().status()).isEqualTo(TicketStatus.NEW);
        assertThat(detailResponse.getBody().assigneeId()).isNull();
        assertThat(detailResponse.getBody().history()).isEmpty();
    }

    @Test
    void invalidAssigneeRoleStatusOrTenantFailsWorkflowUpdateWithoutMutation() {
        TenantResponse tenantA = createTenant("Assignee Tenant A", "assignee-tenant-a");
        TenantResponse tenantB = createTenant("Assignee Tenant B", "assignee-tenant-b");
        OperationalUserResponse actor = createUser(tenantA.id(), "Tenant A Admin", "admin@tenant-a.example",
                OperationalUserRole.TENANT_ADMIN);
        OperationalUserResponse crossTenantAssignee = createUser(tenantB.id(), "Tenant B Agent",
                "agent@tenant-b.example", OperationalUserRole.SUPPORT_AGENT);
        OperationalUserResponse inactiveAssignee = createUser(tenantA.id(), "Inactive Agent",
                "inactive@tenant-a.example", OperationalUserRole.SUPPORT_AGENT);
        OperationalUserResponse tenantAdminAssignee = createUser(tenantA.id(), "Tenant A Manager",
                "manager@tenant-a.example", OperationalUserRole.TENANT_ADMIN);
        updateUserStatus(tenantA.id(), inactiveAssignee.id(), OperationalUserStatus.INACTIVE);
        TicketResponse ticket = createTicket(tenantA.id());

        ResponseEntity<TicketResponse> crossTenantResponse = patchWorkflow(tenantA.id(), ticket.id(),
                new TicketWorkflowUpdateRequest(actor.id(), crossTenantAssignee.id(), TicketPriority.HIGH, null));
        ResponseEntity<TicketResponse> inactiveResponse = patchWorkflow(tenantA.id(), ticket.id(),
                new TicketWorkflowUpdateRequest(actor.id(), inactiveAssignee.id(), TicketPriority.HIGH, null));
        ResponseEntity<TicketResponse> wrongRoleResponse = patchWorkflow(tenantA.id(), ticket.id(),
                new TicketWorkflowUpdateRequest(actor.id(), tenantAdminAssignee.id(), TicketPriority.HIGH, null));
        ResponseEntity<TicketResponse> detailResponse = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenantA.id() + "/tickets/" + ticket.id()), TicketResponse.class);

        assertThat(crossTenantResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(inactiveResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(wrongRoleResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(detailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailResponse.getBody().assigneeId()).isNull();
        assertThat(detailResponse.getBody().priority()).isNull();
        assertThat(detailResponse.getBody().history()).isEmpty();
    }

    private TenantResponse createTenant(String name, String slug) {
        ResponseEntity<TenantResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants"),
                new TenantCreateRequest(name, slug, null),
                TenantResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private OperationalUserResponse createUser(String tenantId, String displayName, String email,
            OperationalUserRole role) {
        ResponseEntity<OperationalUserResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantId + "/users"),
                new OperationalUserCreateRequest(displayName, email, role),
                OperationalUserResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private OperationalUserResponse updateUserStatus(String tenantId, String userId, OperationalUserStatus status) {
        ResponseEntity<OperationalUserResponse> response = restTemplate.exchange(
                url("/api/v1/tenants/" + tenantId + "/users/" + userId + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(new OperationalUserStatusUpdateRequest(status)),
                OperationalUserResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    private TicketResponse createTicket(String tenantId) {
        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantId + "/tickets"),
                new TicketCreateRequest("Login issue", "Ada Lovelace", "ada@example.com", "Cannot log in",
                        null, null, null),
                TicketResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<TicketResponse> patchStatus(String tenantId, String ticketId, TicketStatus status,
            String actorUserId) {
        return restTemplate.exchange(
                url("/api/v1/tenants/" + tenantId + "/tickets/" + ticketId + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(new TicketStatusUpdateRequest(status, actorUserId)),
                TicketResponse.class);
    }

    private ResponseEntity<TicketResponse> patchWorkflow(String tenantId, String ticketId,
            TicketWorkflowUpdateRequest request) {
        return restTemplate.exchange(
                url("/api/v1/tenants/" + tenantId + "/tickets/" + ticketId + "/workflow"),
                HttpMethod.PATCH,
                new HttpEntity<>(request),
                TicketResponse.class);
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

    record OperationalUserStatusUpdateRequest(OperationalUserStatus status) {
    }

    record OperationalUserResponse(String id) {
    }

    record TicketCreateRequest(String subject, String customerName, String customerEmail, String customerMessage,
            String category, TicketPriority priority, String assigneeId) {
    }

    record TicketStatusUpdateRequest(TicketStatus status, String actorUserId) {
    }

    record TicketWorkflowUpdateRequest(String actorUserId, String assigneeId, TicketPriority priority,
            String category) {
    }

    record TicketResponse(String id, TicketStatus status, String category, TicketPriority priority, String assigneeId,
            List<TicketHistoryEntryResponse> history) {
    }

    record TicketHistoryEntryResponse(TicketHistoryEventType eventType, String actorUserId, Instant occurredAt,
            List<TicketFieldChangeResponse> changes) {
    }

    record TicketFieldChangeResponse(String field, String oldValue, String newValue) {
    }
}
