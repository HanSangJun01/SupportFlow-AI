package com.supportflow.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.supportflow.tenant.TenantService;
import com.supportflow.user.OperationalUserService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class KnowledgeDocumentServiceTest {

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private OperationalUserService operationalUserService;

    @InjectMocks
    private KnowledgeDocumentService knowledgeDocumentService;

    @Test
    @DisplayName("create hash generation normalizes content, tags, audit fields, and active status")
    void createDocumentGeneratesContentHashAndDefaults() {
        String tenantId = "tenant-1";
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocument created = knowledgeDocumentService.createDocument(tenantId,
                new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                        KnowledgeDocumentType.FAQ,
                        " Refund policy ",
                        "Line one\r\nLine two\rLine three",
                        " Handbook ",
                        "https://example.com/policies/refunds",
                        List.of(" Billing ", "billing", "Refunds"),
                        Instant.parse("2026-06-01T00:00:00Z"),
                        null,
                        "actor-1"
                ));

        assertThat(created.getTenantId()).isEqualTo(tenantId);
        assertThat(created.getType()).isEqualTo(KnowledgeDocumentType.FAQ);
        assertThat(created.getStatus()).isEqualTo(KnowledgeDocumentStatus.ACTIVE);
        assertThat(created.getTitle()).isEqualTo("Refund policy");
        assertThat(created.getContent()).isEqualTo("Line one\nLine two\nLine three");
        assertThat(created.getSourceLabel()).isEqualTo("Handbook");
        assertThat(created.getTags()).containsExactly("billing", "refunds");
        assertThat(created.getContentHash())
                .isEqualTo("46c949c0b79bc3c1eef1d2970222427b7a0e8d70233d4cc2429535f0f41a0e9a");
        assertThat(created.getCreatedByUserId()).isEqualTo("actor-1");
        assertThat(created.getUpdatedByUserId()).isEqualTo("actor-1");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isNotNull();
        verify(tenantService).requireActiveTenant(tenantId);
        verify(operationalUserService).validateActiveActor(tenantId, "actor-1");
        verify(knowledgeDocumentRepository).save(any(KnowledgeDocument.class));
    }

    @Test
    void createDocumentRejectsMoreThan20Tags() {
        List<String> tags = java.util.stream.IntStream.rangeClosed(1, 21)
                .mapToObj(index -> "tag-" + index)
                .toList();

        assertThatThrownBy(() -> knowledgeDocumentService.createDocument("tenant-1",
                new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                        KnowledgeDocumentType.FAQ,
                        "Refund policy",
                        "Customers can request a refund within 30 days.",
                        "Support handbook",
                        null,
                        tags,
                        null,
                        null,
                        "actor-1"
                )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Documents must not have more than 20 tags");
                });

        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    void createDocumentRejectsTagsLongerThan50Characters() {
        assertThatThrownBy(() -> knowledgeDocumentService.createDocument("tenant-1",
                new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                        KnowledgeDocumentType.FAQ,
                        "Refund policy",
                        "Customers can request a refund within 30 days.",
                        "Support handbook",
                        null,
                        List.of("x".repeat(51)),
                        null,
                        null,
                        "actor-1"
                )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Tags must not exceed 50 characters");
                });

        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    void createDocumentRejectsInvalidEffectiveWindowAndOversizedFields() {
        assertBadRequestOnCreate(new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                KnowledgeDocumentType.FAQ,
                "Refund policy",
                "Customers can request a refund within 30 days.",
                "Support handbook",
                null,
                List.of(),
                Instant.parse("2026-12-31T23:59:59Z"),
                Instant.parse("2026-06-01T00:00:00Z"),
                "actor-1"
        ));
        assertBadRequestOnCreate(new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                KnowledgeDocumentType.FAQ,
                "x".repeat(201),
                "Customers can request a refund within 30 days.",
                "Support handbook",
                null,
                List.of(),
                null,
                null,
                "actor-1"
        ));
        assertBadRequestOnCreate(new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                KnowledgeDocumentType.FAQ,
                "Refund policy",
                "Customers can request a refund within 30 days.",
                "x".repeat(201),
                null,
                List.of(),
                null,
                null,
                "actor-1"
        ));
        assertBadRequestOnCreate(new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                KnowledgeDocumentType.FAQ,
                "Refund policy",
                "x".repeat(50001),
                "Support handbook",
                null,
                List.of(),
                null,
                null,
                "actor-1"
        ));
        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("default active list filtering excludes archived documents")
    void listDocumentsExcludesArchivedByDefault() {
        String tenantId = "tenant-1";
        when(knowledgeDocumentRepository.findByTenantId(tenantId)).thenReturn(List.of(
                document("active", tenantId, KnowledgeDocumentType.FAQ, KnowledgeDocumentStatus.ACTIVE,
                        List.of("billing"), null, null),
                document("archived", tenantId, KnowledgeDocumentType.POLICY, KnowledgeDocumentStatus.ARCHIVED,
                        List.of("policy"), null, null)
        ));

        List<KnowledgeDocument> documents = knowledgeDocumentService.listDocuments(tenantId,
                new KnowledgeDocumentService.KnowledgeDocumentFilters(null, null, null, null));

        assertThat(documents).extracting(KnowledgeDocument::getId).containsExactly("active");
        verify(tenantService).getTenant(tenantId);
    }

    @Test
    @DisplayName("activeAt filtering includes documents within effective date windows")
    void listDocumentsAppliesTypeStatusTagAndActiveAtFilters() {
        String tenantId = "tenant-1";
        when(knowledgeDocumentRepository.findByTenantId(tenantId)).thenReturn(List.of(
                document("match", tenantId, KnowledgeDocumentType.FAQ, KnowledgeDocumentStatus.ARCHIVED,
                        List.of("billing"), "2026-01-01T00:00:00Z", "2026-12-31T23:59:59Z"),
                document("wrong-type", tenantId, KnowledgeDocumentType.POLICY, KnowledgeDocumentStatus.ARCHIVED,
                        List.of("billing"), "2026-01-01T00:00:00Z", "2026-12-31T23:59:59Z"),
                document("wrong-status", tenantId, KnowledgeDocumentType.FAQ, KnowledgeDocumentStatus.ACTIVE,
                        List.of("billing"), "2026-01-01T00:00:00Z", "2026-12-31T23:59:59Z"),
                document("wrong-tag", tenantId, KnowledgeDocumentType.FAQ, KnowledgeDocumentStatus.ARCHIVED,
                        List.of("technical"), "2026-01-01T00:00:00Z", "2026-12-31T23:59:59Z"),
                document("outside-window", tenantId, KnowledgeDocumentType.FAQ, KnowledgeDocumentStatus.ARCHIVED,
                        List.of("billing"), "2026-06-02T00:00:00Z", null)
        ));

        List<KnowledgeDocument> documents = knowledgeDocumentService.listDocuments(tenantId,
                new KnowledgeDocumentService.KnowledgeDocumentFilters(
                        KnowledgeDocumentType.FAQ,
                        KnowledgeDocumentStatus.ARCHIVED,
                        " Billing ",
                        Instant.parse("2026-06-01T00:00:00Z")
                ));

        assertThat(documents).extracting(KnowledgeDocument::getId).containsExactly("match");
    }

    @Test
    @DisplayName("archived content rejection blocks content updates while archived")
    void updateDocumentRejectsContentUpdateWhileArchived() {
        String tenantId = "tenant-1";
        KnowledgeDocument archived = document("doc-1", tenantId, KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ARCHIVED, List.of("billing"), null, null);
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(archived));

        assertThatThrownBy(() -> knowledgeDocumentService.updateDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.UpdateKnowledgeDocumentCommand(
                        null,
                        null,
                        "New content",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "actor-1"
                )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).isEqualTo("Archived documents cannot update content");
                });

        verify(tenantService).requireActiveTenant(tenantId);
        verify(operationalUserService).validateActiveActor(tenantId, "actor-1");
        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("metadata update while archived allows type, title, source, tags, and effective dates")
    void updateDocumentAllowsMetadataUpdateWhileArchived() {
        String tenantId = "tenant-1";
        KnowledgeDocument archived = document("doc-1", tenantId, KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ARCHIVED, List.of("billing"), null, null);
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(archived));
        when(knowledgeDocumentRepository.save(archived)).thenReturn(archived);

        KnowledgeDocument updated = knowledgeDocumentService.updateDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.UpdateKnowledgeDocumentCommand(
                        KnowledgeDocumentType.POLICY,
                        "Updated policy",
                        null,
                        "Updated handbook",
                        "https://example.com/policy",
                        List.of("Policy", "Billing"),
                        Instant.parse("2026-06-01T00:00:00Z"),
                        Instant.parse("2026-12-31T23:59:59Z"),
                        "actor-1"
                ));

        assertThat(updated.getType()).isEqualTo(KnowledgeDocumentType.POLICY);
        assertThat(updated.getTitle()).isEqualTo("Updated policy");
        assertThat(updated.getSourceLabel()).isEqualTo("Updated handbook");
        assertThat(updated.getTags()).containsExactly("policy", "billing");
        assertThat(updated.getEffectiveFrom()).isEqualTo("2026-06-01T00:00:00Z");
        assertThat(updated.getEffectiveTo()).isEqualTo("2026-12-31T23:59:59Z");
        assertThat(updated.getUpdatedByUserId()).isEqualTo("actor-1");
        verify(knowledgeDocumentRepository).save(archived);
    }

    @Test
    void metadataOnlyUpdateKeepsContentHashStable() {
        String tenantId = "tenant-1";
        KnowledgeDocument active = document("doc-1", tenantId, KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ACTIVE, List.of("billing"), null, null);
        active.setContentHash("stable-hash");
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(active));
        when(knowledgeDocumentRepository.save(active)).thenReturn(active);

        KnowledgeDocument updated = knowledgeDocumentService.updateDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.UpdateKnowledgeDocumentCommand(
                        null,
                        "Updated title",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "actor-1"
                ));

        assertThat(updated.getTitle()).isEqualTo("Updated title");
        assertThat(updated.getContentHash()).isEqualTo("stable-hash");
        verify(knowledgeDocumentRepository).save(active);
    }

    @Test
    void emptyUpdateReturnsUnchangedDocumentWithoutSaving() {
        String tenantId = "tenant-1";
        KnowledgeDocument active = document("doc-1", tenantId, KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ACTIVE, List.of("billing"), null, null);
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(active));

        KnowledgeDocument updated = knowledgeDocumentService.updateDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.UpdateKnowledgeDocumentCommand(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "actor-1"
                ));

        assertThat(updated).isSameAs(active);
        assertThat(updated.getContentHash()).isEqualTo("hash");
        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("idempotent archive leaves archive metadata unchanged when already archived")
    void archiveDocumentIsIdempotent() {
        String tenantId = "tenant-1";
        KnowledgeDocument archived = document("doc-1", tenantId, KnowledgeDocumentType.POLICY,
                KnowledgeDocumentStatus.ARCHIVED, List.of(), null, null);
        archived.setArchivedAt(Instant.parse("2026-06-01T00:00:00Z"));
        archived.setArchivedByUserId("actor-original");
        archived.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        archived.setUpdatedByUserId("actor-original");
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(archived));

        KnowledgeDocument result = knowledgeDocumentService.archiveDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.ActorCommand("actor-2"));

        assertThat(result.getStatus()).isEqualTo(KnowledgeDocumentStatus.ARCHIVED);
        assertThat(result.getArchivedAt()).isEqualTo("2026-06-01T00:00:00Z");
        assertThat(result.getArchivedByUserId()).isEqualTo("actor-original");
        assertThat(result.getUpdatedAt()).isEqualTo("2026-06-01T00:00:00Z");
        assertThat(result.getUpdatedByUserId()).isEqualTo("actor-original");
        verify(tenantService).requireActiveTenant(tenantId);
        verify(operationalUserService).validateActiveActor(tenantId, "actor-2");
        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("idempotent restore leaves active document unchanged")
    void restoreDocumentIsIdempotentWhenAlreadyActive() {
        String tenantId = "tenant-1";
        KnowledgeDocument active = document("doc-1", tenantId, KnowledgeDocumentType.FAQ,
                KnowledgeDocumentStatus.ACTIVE, List.of(), null, null);
        active.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        active.setUpdatedByUserId("actor-original");
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(active));

        KnowledgeDocument result = knowledgeDocumentService.restoreDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.ActorCommand("actor-2"));

        assertThat(result.getStatus()).isEqualTo(KnowledgeDocumentStatus.ACTIVE);
        assertThat(result.getUpdatedAt()).isEqualTo("2026-06-01T00:00:00Z");
        assertThat(result.getUpdatedByUserId()).isEqualTo("actor-original");
        verify(tenantService).requireActiveTenant(tenantId);
        verify(operationalUserService).validateActiveActor(tenantId, "actor-2");
        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("restore clearing archive metadata sets active state and clears archive fields")
    void restoreDocumentClearsArchiveMetadata() {
        String tenantId = "tenant-1";
        KnowledgeDocument archived = document("doc-1", tenantId, KnowledgeDocumentType.POLICY,
                KnowledgeDocumentStatus.ARCHIVED, List.of(), null, null);
        archived.setArchivedAt(Instant.parse("2026-06-01T00:00:00Z"));
        archived.setArchivedByUserId("actor-original");
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(archived));
        when(knowledgeDocumentRepository.save(archived)).thenReturn(archived);

        KnowledgeDocument result = knowledgeDocumentService.restoreDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.ActorCommand("actor-2"));

        assertThat(result.getStatus()).isEqualTo(KnowledgeDocumentStatus.ACTIVE);
        assertThat(result.getArchivedAt()).isNull();
        assertThat(result.getArchivedByUserId()).isNull();
        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getUpdatedByUserId()).isEqualTo("actor-2");
        verify(knowledgeDocumentRepository).save(archived);
    }

    @Test
    void updateDocumentValidatesEffectiveWindowBeforeSaving() {
        String tenantId = "tenant-1";
        KnowledgeDocument active = document("doc-1", tenantId, KnowledgeDocumentType.POLICY,
                KnowledgeDocumentStatus.ACTIVE, List.of(), null, null);
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> knowledgeDocumentService.updateDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.UpdateKnowledgeDocumentCommand(
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Instant.parse("2026-12-31T23:59:59Z"),
                        Instant.parse("2026-06-01T00:00:00Z"),
                        "actor-1"
                )))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(knowledgeDocumentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    void archiveDocumentSetsCurrentArchiveMetadata() {
        String tenantId = "tenant-1";
        KnowledgeDocument active = document("doc-1", tenantId, KnowledgeDocumentType.POLICY,
                KnowledgeDocumentStatus.ACTIVE, List.of(), null, null);
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(active));
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocument archived = knowledgeDocumentService.archiveDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.ActorCommand("actor-1"));

        assertThat(archived.getStatus()).isEqualTo(KnowledgeDocumentStatus.ARCHIVED);
        assertThat(archived.getArchivedAt()).isNotNull();
        assertThat(archived.getArchivedByUserId()).isEqualTo("actor-1");
        assertThat(archived.getUpdatedAt()).isEqualTo(archived.getArchivedAt());
        assertThat(archived.getUpdatedByUserId()).isEqualTo("actor-1");
    }

    @Test
    void updateDocumentRecomputesHashOnlyWhenContentChanges() {
        String tenantId = "tenant-1";
        KnowledgeDocument active = document("doc-1", tenantId, KnowledgeDocumentType.POLICY,
                KnowledgeDocumentStatus.ACTIVE, List.of(), null, null);
        active.setContent("Old content");
        active.setContentHash("old-hash");
        when(knowledgeDocumentRepository.findByTenantIdAndId(tenantId, "doc-1")).thenReturn(Optional.of(active));
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeDocument updated = knowledgeDocumentService.updateDocument(tenantId, "doc-1",
                new KnowledgeDocumentService.UpdateKnowledgeDocumentCommand(
                        null,
                        null,
                        "New\r\ncontent",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "actor-1"
                ));

        assertThat(updated.getContent()).isEqualTo("New\ncontent");
        assertThat(updated.getContentHash()).isNotEqualTo("old-hash");
        ArgumentCaptor<KnowledgeDocument> documentCaptor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(knowledgeDocumentRepository).save(documentCaptor.capture());
        assertThat(documentCaptor.getValue().getUpdatedByUserId()).isEqualTo("actor-1");
    }

    private void assertBadRequestOnCreate(KnowledgeDocumentService.CreateKnowledgeDocumentCommand command) {
        assertThatThrownBy(() -> knowledgeDocumentService.createDocument("tenant-1", command))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private KnowledgeDocument document(String id, String tenantId, KnowledgeDocumentType type,
            KnowledgeDocumentStatus status, List<String> tags, String effectiveFrom, String effectiveTo) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setTenantId(tenantId);
        document.setType(type);
        document.setStatus(status);
        document.setTitle("Refund policy");
        document.setContent("Customers can request a refund within 30 days.");
        document.setSourceLabel("Support handbook");
        document.setTags(tags);
        document.setContentHash("hash");
        document.setCreatedByUserId("actor-1");
        document.setUpdatedByUserId("actor-1");
        document.setCreatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        document.setUpdatedAt(Instant.parse("2026-06-01T00:00:00Z"));
        document.setEffectiveFrom(effectiveFrom == null ? null : Instant.parse(effectiveFrom));
        document.setEffectiveTo(effectiveTo == null ? null : Instant.parse(effectiveTo));
        return document;
    }
}
