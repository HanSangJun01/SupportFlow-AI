package com.supportflow.knowledge;

import com.supportflow.tenant.TenantService;
import com.supportflow.user.OperationalUserService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class KnowledgeDocumentService {

    private static final int MAX_TAGS = 20;
    private static final int MAX_TAG_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_SOURCE_LABEL_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 50000;

    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final TenantService tenantService;
    private final OperationalUserService operationalUserService;

    public KnowledgeDocumentService(KnowledgeDocumentRepository knowledgeDocumentRepository,
            TenantService tenantService, OperationalUserService operationalUserService) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.tenantService = tenantService;
        this.operationalUserService = operationalUserService;
    }

    public KnowledgeDocument createDocument(String tenantId, CreateKnowledgeDocumentCommand command) {
        tenantService.requireActiveTenant(tenantId);
        operationalUserService.validateActiveActor(tenantId, command.actorUserId());
        validateEffectiveWindow(command.effectiveFrom(), command.effectiveTo());

        String title = requireNonBlank(command.title(), "Title must not be blank");
        String content = requireNonBlank(command.content(), "Content must not be blank");
        String sourceLabel = requireNonBlank(command.sourceLabel(), "Source label must not be blank");
        validateLength(title, MAX_TITLE_LENGTH, "Title must not exceed 200 characters");
        validateLength(sourceLabel, MAX_SOURCE_LABEL_LENGTH, "Source label must not exceed 200 characters");
        validateLength(content, MAX_CONTENT_LENGTH, "Content must not exceed 50000 characters");

        Instant now = Instant.now();
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTenantId(tenantId);
        document.setType(command.type());
        document.setStatus(KnowledgeDocumentStatus.ACTIVE);
        document.setTitle(title);
        document.setContent(normalizeContent(content));
        document.setSourceLabel(sourceLabel);
        document.setSourceUrl(command.sourceUrl());
        document.setTags(normalizeTags(command.tags()));
        document.setEffectiveFrom(command.effectiveFrom());
        document.setEffectiveTo(command.effectiveTo());
        document.setContentHash(hashContent(document.getContent()));
        document.setCreatedByUserId(command.actorUserId());
        document.setUpdatedByUserId(command.actorUserId());
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return knowledgeDocumentRepository.save(document);
    }

    public List<KnowledgeDocument> listDocuments(String tenantId, KnowledgeDocumentFilters filters) {
        tenantService.getTenant(tenantId);
        String normalizedTag = normalizeTag(filters.tag());
        List<KnowledgeDocument> documents = new ArrayList<>(knowledgeDocumentRepository.findByTenantId(tenantId));
        return documents.stream()
                .filter(document -> filters.type() == null || document.getType() == filters.type())
                .filter(document -> filters.status() == null
                        ? document.getStatus() != KnowledgeDocumentStatus.ARCHIVED
                        : document.getStatus() == filters.status())
                .filter(document -> normalizedTag == null || document.getTags().contains(normalizedTag))
                .filter(document -> filters.activeAt() == null || isActiveAt(document, filters.activeAt()))
                .sorted(Comparator.comparing(KnowledgeDocument::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public KnowledgeDocument getDocument(String tenantId, String documentId) {
        tenantService.getTenant(tenantId);
        return findTenantDocument(tenantId, documentId);
    }

    public KnowledgeDocument updateDocument(String tenantId, String documentId, UpdateKnowledgeDocumentCommand command) {
        tenantService.requireActiveTenant(tenantId);
        KnowledgeDocument document = findTenantDocument(tenantId, documentId);
        operationalUserService.validateActiveActor(tenantId, command.actorUserId());

        if (document.getStatus() == KnowledgeDocumentStatus.ARCHIVED && command.content() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archived documents cannot update content");
        }

        Instant effectiveFrom = command.effectiveFrom() == null ? document.getEffectiveFrom() : command.effectiveFrom();
        Instant effectiveTo = command.effectiveTo() == null ? document.getEffectiveTo() : command.effectiveTo();
        validateEffectiveWindow(effectiveFrom, effectiveTo);

        boolean changed = false;
        if (command.type() != null && document.getType() != command.type()) {
            document.setType(command.type());
            changed = true;
        }
        if (command.title() != null) {
            String title = requireNonBlank(command.title(), "Title must not be blank");
            validateLength(title, MAX_TITLE_LENGTH, "Title must not exceed 200 characters");
            if (!Objects.equals(document.getTitle(), title)) {
                document.setTitle(title);
                changed = true;
            }
        }
        if (command.content() != null) {
            String content = requireNonBlank(command.content(), "Content must not be blank");
            validateLength(content, MAX_CONTENT_LENGTH, "Content must not exceed 50000 characters");
            String normalizedContent = normalizeContent(content);
            if (!Objects.equals(document.getContent(), normalizedContent)) {
                document.setContent(normalizedContent);
                document.setContentHash(hashContent(normalizedContent));
                changed = true;
            }
        }
        if (command.sourceLabel() != null) {
            String sourceLabel = requireNonBlank(command.sourceLabel(), "Source label must not be blank");
            validateLength(sourceLabel, MAX_SOURCE_LABEL_LENGTH, "Source label must not exceed 200 characters");
            if (!Objects.equals(document.getSourceLabel(), sourceLabel)) {
                document.setSourceLabel(sourceLabel);
                changed = true;
            }
        }
        if (command.sourceUrl() != null && !Objects.equals(document.getSourceUrl(), command.sourceUrl())) {
            document.setSourceUrl(command.sourceUrl());
            changed = true;
        }
        if (command.tags() != null) {
            List<String> tags = normalizeTags(command.tags());
            if (!document.getTags().equals(tags)) {
                document.setTags(tags);
                changed = true;
            }
        }
        if (command.effectiveFrom() != null && !Objects.equals(document.getEffectiveFrom(), command.effectiveFrom())) {
            document.setEffectiveFrom(command.effectiveFrom());
            changed = true;
        }
        if (command.effectiveTo() != null && !Objects.equals(document.getEffectiveTo(), command.effectiveTo())) {
            document.setEffectiveTo(command.effectiveTo());
            changed = true;
        }

        if (!changed) {
            return document;
        }

        document.setUpdatedAt(Instant.now());
        document.setUpdatedByUserId(command.actorUserId());
        return knowledgeDocumentRepository.save(document);
    }

    public KnowledgeDocument archiveDocument(String tenantId, String documentId, ActorCommand command) {
        tenantService.requireActiveTenant(tenantId);
        KnowledgeDocument document = findTenantDocument(tenantId, documentId);
        operationalUserService.validateActiveActor(tenantId, command.actorUserId());
        if (document.getStatus() == KnowledgeDocumentStatus.ARCHIVED) {
            return document;
        }

        Instant archivedAt = Instant.now();
        String archivedByUserId = command.actorUserId();
        document.setStatus(KnowledgeDocumentStatus.ARCHIVED);
        document.setArchivedAt(archivedAt);
        document.setArchivedByUserId(archivedByUserId);
        document.setUpdatedAt(archivedAt);
        document.setUpdatedByUserId(archivedByUserId);
        return knowledgeDocumentRepository.save(document);
    }

    public KnowledgeDocument restoreDocument(String tenantId, String documentId, ActorCommand command) {
        tenantService.requireActiveTenant(tenantId);
        KnowledgeDocument document = findTenantDocument(tenantId, documentId);
        operationalUserService.validateActiveActor(tenantId, command.actorUserId());
        if (document.getStatus() == KnowledgeDocumentStatus.ACTIVE) {
            return document;
        }

        document.setStatus(KnowledgeDocumentStatus.ACTIVE);
        document.setArchivedAt(null);
        document.setArchivedByUserId(null);
        document.setUpdatedAt(Instant.now());
        document.setUpdatedByUserId(command.actorUserId());
        return knowledgeDocumentRepository.save(document);
    }

    private KnowledgeDocument findTenantDocument(String tenantId, String documentId) {
        return knowledgeDocumentRepository.findByTenantIdAndId(tenantId, documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge document not found"));
    }

    private boolean isActiveAt(KnowledgeDocument document, Instant activeAt) {
        return (document.getEffectiveFrom() == null || !document.getEffectiveFrom().isAfter(activeAt))
                && (document.getEffectiveTo() == null || !document.getEffectiveTo().isBefore(activeAt));
    }

    private void validateEffectiveWindow(Instant effectiveFrom, Instant effectiveTo) {
        if (effectiveFrom != null && effectiveTo != null && effectiveFrom.isAfter(effectiveTo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "effectiveFrom must not be after effectiveTo");
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private void validateLength(String value, int maxLength, String message) {
        if (value != null && value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalizedTag = normalizeTag(tag);
            if (normalizedTag != null) {
                if (normalizedTag.length() > MAX_TAG_LENGTH) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Tags must not exceed 50 characters");
                }
                normalized.add(normalizedTag);
            }
        }
        if (normalized.size() > MAX_TAGS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Documents must not have more than 20 tags");
        }
        return List.copyOf(normalized);
    }

    private String normalizeTag(String tag) {
        if (tag == null) {
            return null;
        }
        String normalizedTag = tag.trim().toLowerCase(Locale.ROOT);
        return normalizedTag.isEmpty() ? null : normalizedTag;
    }

    private String normalizeContent(String content) {
        return content.replace("\r\n", "\n").replace('\r', '\n');
    }

    private String hashContent(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest unavailable", exception);
        }
    }

    public record CreateKnowledgeDocumentCommand(
            KnowledgeDocumentType type,
            String title,
            String content,
            String sourceLabel,
            String sourceUrl,
            List<String> tags,
            Instant effectiveFrom,
            Instant effectiveTo,
            String actorUserId
    ) {
    }

    public record UpdateKnowledgeDocumentCommand(
            KnowledgeDocumentType type,
            String title,
            String content,
            String sourceLabel,
            String sourceUrl,
            List<String> tags,
            Instant effectiveFrom,
            Instant effectiveTo,
            String actorUserId
    ) {
    }

    public record KnowledgeDocumentFilters(
            KnowledgeDocumentType type,
            KnowledgeDocumentStatus status,
            String tag,
            Instant activeAt
    ) {
    }

    public record ActorCommand(String actorUserId) {
    }
}
