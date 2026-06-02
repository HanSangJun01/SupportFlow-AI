package com.supportflow.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OperationalUserRepository extends MongoRepository<OperationalUser, String> {

    List<OperationalUser> findByTenantId(String tenantId);

    Optional<OperationalUser> findByTenantIdAndId(String tenantId, String id);
}
