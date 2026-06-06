package com.supportflow.common;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.knowledge.KnowledgeDocumentService;
import com.supportflow.tenant.TenantService;
import com.supportflow.ticket.TicketService;
import com.supportflow.user.OperationalUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private OperationalUserService operationalUserService;

    @MockitoBean
    private KnowledgeDocumentService knowledgeDocumentService;

    @Test
    void apiDocsContainPhaseOneTwoAndThreeRoutes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("SupportFlow AI Backend API"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/tenants")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/tenants/{tenantId}")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/tenants/{tenantId}/tickets")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/tenants/{tenantId}/tickets/{ticketId}")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/tickets/{ticketId}/status")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/users")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/users/{userId}")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/users/{userId}/status")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/tickets/{ticketId}/workflow")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/knowledge-documents")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/archive")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "/api/v1/tenants/{tenantId}/knowledge-documents/{documentId}/restore")));
    }
}
