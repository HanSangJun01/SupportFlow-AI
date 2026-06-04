package com.supportflow.knowledge;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface KnowledgeDocumentRepository extends MongoRepository<KnowledgeDocument, String> {

    List<KnowledgeDocument> findByTenantId(String tenantId);

    Optional<KnowledgeDocument> findByTenantIdAndId(String tenantId, String id);
}
