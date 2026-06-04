package com.supportflow.knowledge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/knowledge-documents")
@Tag(name = "Knowledge Documents", description = "Tenant-scoped knowledge document APIs")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(KnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @PostMapping
    @Operation(summary = "Create a tenant-scoped knowledge document")
    public ResponseEntity<KnowledgeDocumentResponse> createDocument(@PathVariable String tenantId,
            @Valid @RequestBody CreateKnowledgeDocumentRequest request) {
        KnowledgeDocument document = knowledgeDocumentService.createDocument(tenantId,
                new KnowledgeDocumentService.CreateKnowledgeDocumentCommand(
                        request.type(),
                        request.title(),
                        request.content(),
                        request.sourceLabel(),
                        request.sourceUrl(),
                        request.tags(),
                        request.effectiveFrom(),
                        request.effectiveTo(),
                        request.actorUserId()
                ));
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId
                        + "/knowledge-documents/" + document.getId()))
                .body(KnowledgeDocumentResponse.from(document));
    }

    @GetMapping
    @Operation(summary = "List tenant-scoped knowledge documents")
    public List<KnowledgeDocumentResponse> listDocuments(
            @PathVariable String tenantId,
            @RequestParam(required = false) KnowledgeDocumentType type,
            @RequestParam(required = false) KnowledgeDocumentStatus status,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant activeAt
    ) {
        return knowledgeDocumentService.listDocuments(tenantId,
                        new KnowledgeDocumentService.KnowledgeDocumentFilters(type, status, tag, activeAt))
                .stream()
                .map(KnowledgeDocumentResponse::from)
                .toList();
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get a tenant-scoped knowledge document")
    public KnowledgeDocumentResponse getDocument(@PathVariable String tenantId, @PathVariable String documentId) {
        return KnowledgeDocumentResponse.from(knowledgeDocumentService.getDocument(tenantId, documentId));
    }

    @PatchMapping("/{documentId}")
    @Operation(summary = "Update a tenant-scoped knowledge document")
    public KnowledgeDocumentResponse updateDocument(@PathVariable String tenantId, @PathVariable String documentId,
            @Valid @RequestBody UpdateKnowledgeDocumentRequest request) {
        return KnowledgeDocumentResponse.from(knowledgeDocumentService.updateDocument(tenantId, documentId,
                new KnowledgeDocumentService.UpdateKnowledgeDocumentCommand(
                        request.type(),
                        request.title(),
                        request.content(),
                        request.sourceLabel(),
                        request.sourceUrl(),
                        request.tags(),
                        request.effectiveFrom(),
                        request.effectiveTo(),
                        request.actorUserId()
                )));
    }

    public record CreateKnowledgeDocumentRequest(
            @NotNull KnowledgeDocumentType type,
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String sourceLabel,
            String sourceUrl,
            List<String> tags,
            Instant effectiveFrom,
            Instant effectiveTo,
            @NotBlank String actorUserId
    ) {
    }

    public record UpdateKnowledgeDocumentRequest(
            KnowledgeDocumentType type,
            String title,
            String content,
            String sourceLabel,
            String sourceUrl,
            List<String> tags,
            Instant effectiveFrom,
            Instant effectiveTo,
            @NotBlank String actorUserId
    ) {
    }

    public record KnowledgeDocumentResponse(
            String id,
            String tenantId,
            KnowledgeDocumentType type,
            KnowledgeDocumentStatus status,
            String title,
            String content,
            String sourceLabel,
            String sourceUrl,
            List<String> tags,
            Instant effectiveFrom,
            Instant effectiveTo,
            String contentHash,
            String createdByUserId,
            String updatedByUserId,
            Instant createdAt,
            Instant updatedAt,
            Instant archivedAt,
            String archivedByUserId
    ) {
        static KnowledgeDocumentResponse from(KnowledgeDocument document) {
            return new KnowledgeDocumentResponse(
                    document.getId(),
                    document.getTenantId(),
                    document.getType(),
                    document.getStatus(),
                    document.getTitle(),
                    document.getContent(),
                    document.getSourceLabel(),
                    document.getSourceUrl(),
                    document.getTags(),
                    document.getEffectiveFrom(),
                    document.getEffectiveTo(),
                    document.getContentHash(),
                    document.getCreatedByUserId(),
                    document.getUpdatedByUserId(),
                    document.getCreatedAt(),
                    document.getUpdatedAt(),
                    document.getArchivedAt(),
                    document.getArchivedByUserId()
            );
        }
    }
}
