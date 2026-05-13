package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportflow.user.OperationalUserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class TenantIsolationMongoIntegrationTest {

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
    void crossTenantTicketReadAndStatusMutationReturn404() {
        TenantCreateResponse tenantA = createTenant("Tenant A", "tenant-a");
        TenantCreateResponse tenantB = createTenant("Tenant B", "tenant-b");
        OperationalUserResponse tenantBActor = createUser(tenantB.id(), "Tenant B Admin", "admin@tenant-b.example",
                OperationalUserRole.TENANT_ADMIN);
        TicketResponse ticketA = createTicket(tenantA.id());

        ResponseEntity<TicketResponse> ownRead = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenantA.id() + "/tickets/" + ticketA.id()), TicketResponse.class);
        ResponseEntity<TicketResponse> crossRead = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenantB.id() + "/tickets/" + ticketA.id()), TicketResponse.class);
        ResponseEntity<String> crossPatch = restTemplate.exchange(
                url("/api/v1/tenants/" + tenantB.id() + "/tickets/" + ticketA.id() + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(new StatusUpdateRequest(TicketStatus.TRIAGED, tenantBActor.id())),
                String.class);
        ResponseEntity<TicketResponse[]> tenantBList = restTemplate.getForEntity(
                url("/api/v1/tenants/" + tenantB.id() + "/tickets"), TicketResponse[].class);

        assertThat(ownRead.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(crossRead.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(crossPatch.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(tenantBList.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tenantBList.getBody()).isEmpty();
    }

    private TenantCreateResponse createTenant(String name, String slug) {
        ResponseEntity<TenantCreateResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants"),
                new TenantCreateRequest(name, slug, null),
                TenantCreateResponse.class);
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

    private TicketResponse createTicket(String tenantId) {
        ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
                url("/api/v1/tenants/" + tenantId + "/tickets"),
                new TicketCreateRequest("Login issue", "Ada Lovelace", "ada@example.com", "Cannot log in"),
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

    record TenantCreateResponse(String id) {
    }

    record OperationalUserCreateRequest(String displayName, String email, OperationalUserRole role) {
    }

    record OperationalUserResponse(String id) {
    }

    record TicketCreateRequest(String subject, String customerName, String customerEmail, String customerMessage) {
    }

    record StatusUpdateRequest(TicketStatus status, String actorUserId) {
    }

    record TicketResponse(String id) {
    }
}
