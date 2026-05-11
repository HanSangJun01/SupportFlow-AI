package com.supportflow.tenant;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TenantRepository extends MongoRepository<Tenant, String> {

    boolean existsBySlug(String slug);

    List<Tenant> findAllByOrderBySlugAsc();
}
