package com.supportflow.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.common.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {KnowledgeDocumentController.class, GlobalExceptionHandler.class})
class KnowledgeDocumentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeDocumentService knowledgeDocumentService;

    @Test
    void createDocumentReturns201LocationAndResponseShape() throws Exception {
        KnowledgeDocument document = document("doc-1", "tenant-1", KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ACTIVE);
        when(knowledgeDocumentService.createDocument(eq("tenant-1"),
                any(KnowledgeDocumentService.CreateKnowledgeDocumentCommand.class))).thenReturn(document);

        mockMvc.perform(post("/api/v1/tenants/tenant-1/knowledge-documents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "FAQ",
                                  "title": "Refund policy",
                                  "content": "Customers can request a refund within 30 days.",
                                  "sourceLabel": "Support handbook",
                                  "sourceUrl": "https://example.com/policies/refunds",
                                  "tags": ["billing", "refunds"],
                                  "effectiveFrom": "2026-06-01T00:00:00Z",
                                  "actorUserId": "actor-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        "/api/v1/tenants/tenant-1/knowledge-documents/doc-1"))
                .andExpect(jsonPath("$.id").value("doc-1"))
                .andExpect(jsonPath("$.tenantId").value("tenant-1"))
                .andExpect(jsonPath("$.type").value("FAQ"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.sourceLabel").value("Support handbook"))
                .andExpect(jsonPath("$.contentHash").value("hash"))
                .andExpect(jsonPath("$.createdByUserId").value("actor-1"))
                .andExpect(jsonPath("$.updatedByUserId").value("actor-1"));

        ArgumentCaptor<KnowledgeDocumentService.CreateKnowledgeDocumentCommand> commandCaptor =
                ArgumentCaptor.forClass(KnowledgeDocumentService.CreateKnowledgeDocumentCommand.class);
        verify(knowledgeDocumentService).createDocument(eq("tenant-1"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().type()).isEqualTo(KnowledgeDocumentType.FAQ);
        assertThat(commandCaptor.getValue().actorUserId()).isEqualTo("actor-1");
    }

    @Test
    void listDocumentsBindsTypeStatusTagAndActiveAtQueryParameters() throws Exception {
        when(knowledgeDocumentService.listDocuments(eq("tenant-1"),
                any(KnowledgeDocumentService.KnowledgeDocumentFilters.class)))
                .thenReturn(List.of(document("doc-1", "tenant-1", KnowledgeDocumentType.FAQ,
                        KnowledgeDocumentStatus.ARCHIVED)));

        mockMvc.perform(get("/api/v1/tenants/tenant-1/knowledge-documents")
                        .param("type", "FAQ")
                        .param("status", "ARCHIVED")
                        .param("tag", "billing")
                        .param("activeAt", "2026-06-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("doc-1"))
                .andExpect(jsonPath("$[0].status").value("ARCHIVED"));

        ArgumentCaptor<KnowledgeDocumentService.KnowledgeDocumentFilters> filtersCaptor =
                ArgumentCaptor.forClass(KnowledgeDocumentService.KnowledgeDocumentFilters.class);
        verify(knowledgeDocumentService).listDocuments(eq("tenant-1"), filtersCaptor.capture());
        assertThat(filtersCaptor.getValue().type()).isEqualTo(KnowledgeDocumentType.FAQ);
        assertThat(filtersCaptor.getValue().status()).isEqualTo(KnowledgeDocumentStatus.ARCHIVED);
        assertThat(filtersCaptor.getValue().tag()).isEqualTo("billing");
        assertThat(filtersCaptor.getValue().activeAt()).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
    }

    @Test
    void getDocumentUsesTenantScopedRoute() throws Exception {
        when(knowledgeDocumentService.getDocument("tenant-1", "doc-1"))
                .thenReturn(document("doc-1", "tenant-1", KnowledgeDocumentType.POLICY,
                        KnowledgeDocumentStatus.ACTIVE));

        mockMvc.perform(get("/api/v1/tenants/tenant-1/knowledge-documents/doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("doc-1"))
                .andExpect(jsonPath("$.tenantId").value("tenant-1"))
                .andExpect(jsonPath("$.type").value("POLICY"));
    }

    @Test
    void updateDocumentBindsTypeAndMutableMetadata() throws Exception {
        KnowledgeDocument document = document("doc-1", "tenant-1", KnowledgeDocumentType.POLICY,
                KnowledgeDocumentStatus.ACTIVE);
        document.setTitle("Updated policy");
        when(knowledgeDocumentService.updateDocument(eq("tenant-1"), eq("doc-1"),
                any(KnowledgeDocumentService.UpdateKnowledgeDocumentCommand.class))).thenReturn(document);

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/knowledge-documents/doc-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "POLICY",
                                  "title": "Updated policy",
                                  "sourceLabel": "Updated handbook",
                                  "tags": ["policy"],
                                  "actorUserId": "actor-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("POLICY"))
                .andExpect(jsonPath("$.title").value("Updated policy"));

        ArgumentCaptor<KnowledgeDocumentService.UpdateKnowledgeDocumentCommand> commandCaptor =
                ArgumentCaptor.forClass(KnowledgeDocumentService.UpdateKnowledgeDocumentCommand.class);
        verify(knowledgeDocumentService).updateDocument(eq("tenant-1"), eq("doc-1"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().type()).isEqualTo(KnowledgeDocumentType.POLICY);
        assertThat(commandCaptor.getValue().actorUserId()).isEqualTo("actor-1");
    }

    @Test
    void archiveDocumentReturnsArchiveMetadata() throws Exception {
        KnowledgeDocument document = document("doc-1", "tenant-1", KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ARCHIVED);
        document.setArchivedAt(Instant.parse("2026-06-04T00:00:00Z"));
        document.setArchivedByUserId("actor-1");
        when(knowledgeDocumentService.archiveDocument(eq("tenant-1"), eq("doc-1"),
                any(KnowledgeDocumentService.ActorCommand.class))).thenReturn(document);

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/knowledge-documents/doc-1/archive")
                        .contentType("application/json")
                        .content("""
                                { "actorUserId": "actor-1" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedAt").value("2026-06-04T00:00:00Z"))
                .andExpect(jsonPath("$.archivedByUserId").value("actor-1"));

        ArgumentCaptor<KnowledgeDocumentService.ActorCommand> commandCaptor =
                ArgumentCaptor.forClass(KnowledgeDocumentService.ActorCommand.class);
        verify(knowledgeDocumentService).archiveDocument(eq("tenant-1"), eq("doc-1"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().actorUserId()).isEqualTo("actor-1");
    }

    @Test
    void restoreDocumentReturnsActiveDocumentWithClearedArchiveMetadata() throws Exception {
        KnowledgeDocument document = document("doc-1", "tenant-1", KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ACTIVE);
        when(knowledgeDocumentService.restoreDocument(eq("tenant-1"), eq("doc-1"),
                any(KnowledgeDocumentService.ActorCommand.class))).thenReturn(document);

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/knowledge-documents/doc-1/restore")
                        .contentType("application/json")
                        .content("""
                                { "actorUserId": "actor-1" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.archivedAt").doesNotExist())
                .andExpect(jsonPath("$.archivedByUserId").doesNotExist());

        ArgumentCaptor<KnowledgeDocumentService.ActorCommand> commandCaptor =
                ArgumentCaptor.forClass(KnowledgeDocumentService.ActorCommand.class);
        verify(knowledgeDocumentService).restoreDocument(eq("tenant-1"), eq("doc-1"), commandCaptor.capture());
        assertThat(commandCaptor.getValue().actorUserId()).isEqualTo("actor-1");
    }

    @Test
    void archiveDocumentRequiresActorUserId() throws Exception {
        mockMvc.perform(patch("/api/v1/tenants/tenant-1/knowledge-documents/doc-1/archive")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restoreDocumentRequiresActorUserId() throws Exception {
        mockMvc.perform(patch("/api/v1/tenants/tenant-1/knowledge-documents/doc-1/restore")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void archiveDocumentReturns200ForAlreadyArchivedDocument() throws Exception {
        KnowledgeDocument document = document("doc-1", "tenant-1", KnowledgeDocumentType.POLICY,
                KnowledgeDocumentStatus.ARCHIVED);
        document.setArchivedAt(Instant.parse("2026-06-04T00:00:00Z"));
        document.setArchivedByUserId("actor-original");
        when(knowledgeDocumentService.archiveDocument(eq("tenant-1"), eq("doc-1"),
                any(KnowledgeDocumentService.ActorCommand.class))).thenReturn(document);

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/knowledge-documents/doc-1/archive")
                        .contentType("application/json")
                        .content("""
                                { "actorUserId": "actor-2" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"))
                .andExpect(jsonPath("$.archivedByUserId").value("actor-original"));
    }

    @Test
    void restoreDocumentReturns200ForAlreadyActiveDocument() throws Exception {
        when(knowledgeDocumentService.restoreDocument(eq("tenant-1"), eq("doc-1"),
                any(KnowledgeDocumentService.ActorCommand.class)))
                .thenReturn(document("doc-1", "tenant-1", KnowledgeDocumentType.FAQ,
                        KnowledgeDocumentStatus.ACTIVE));

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/knowledge-documents/doc-1/restore")
                        .contentType("application/json")
                        .content("""
                                { "actorUserId": "actor-2" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createDocumentRequiresActorUserId() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/tenant-1/knowledge-documents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "FAQ",
                                  "title": "Refund policy",
                                  "content": "Customers can request a refund within 30 days.",
                                  "sourceLabel": "Support handbook"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentRequiresType() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/tenant-1/knowledge-documents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "title": "Refund policy",
                                  "content": "Customers can request a refund within 30 days.",
                                  "sourceLabel": "Support handbook",
                                  "actorUserId": "actor-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentRequiresTitle() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/tenant-1/knowledge-documents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "FAQ",
                                  "content": "Customers can request a refund within 30 days.",
                                  "sourceLabel": "Support handbook",
                                  "actorUserId": "actor-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentRequiresContent() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/tenant-1/knowledge-documents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "FAQ",
                                  "title": "Refund policy",
                                  "sourceLabel": "Support handbook",
                                  "actorUserId": "actor-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDocumentRequiresSourceLabel() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/tenant-1/knowledge-documents")
                        .contentType("application/json")
                        .content("""
                                {
                                  "type": "FAQ",
                                  "title": "Refund policy",
                                  "content": "Customers can request a refund within 30 days.",
                                  "actorUserId": "actor-1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private KnowledgeDocument document(String id, String tenantId, KnowledgeDocumentType type,
            KnowledgeDocumentStatus status) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setTenantId(tenantId);
        document.setType(type);
        document.setStatus(status);
        document.setTitle("Refund policy");
        document.setContent("Customers can request a refund within 30 days.");
        document.setSourceLabel("Support handbook");
        document.setSourceUrl("https://example.com/policies/refunds");
        document.setTags(List.of("billing", "refunds"));
        document.setEffectiveFrom(Instant.parse("2026-06-01T00:00:00Z"));
        document.setContentHash("hash");
        document.setCreatedByUserId("actor-1");
        document.setUpdatedByUserId("actor-1");
        document.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        document.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        return document;
    }
}
