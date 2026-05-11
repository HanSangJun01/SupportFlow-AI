package com.supportflow.tenant;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TenantRepository extends MongoRepository<Tenant, String> {

    List<Tenant> findAllByOrderBySlugAsc();
}
