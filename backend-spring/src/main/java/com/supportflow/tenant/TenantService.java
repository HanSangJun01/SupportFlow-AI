package com.supportflow.tenant;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant createTenant(String name, String slug, String description) {
        if (tenantRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant slug already exists");
        }

        Instant now = Instant.now();
        Tenant tenant = new Tenant();
        tenant.setName(name);
        tenant.setSlug(slug);
        tenant.setDescription(description);
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        return tenantRepository.save(tenant);
    }

    public List<Tenant> listTenants() {
        return tenantRepository.findAllByOrderBySlugAsc();
    }

    public Tenant getTenant(String tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));
    }

    public Tenant updateTenant(String tenantId, UpdateTenantCommand command) {
        Tenant tenant = getTenant(tenantId);
        if (command.name() != null) {
            tenant.setName(command.name());
        }
        if (command.description() != null) {
            tenant.setDescription(command.description());
        }
        if (command.status() != null) {
            tenant.setStatus(command.status());
        }
        tenant.setUpdatedAt(Instant.now());
        return tenantRepository.save(tenant);
    }

    public Tenant requireActiveTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId);
        if (tenant.getStatus() == TenantStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Tenant is inactive");
        }
        return tenant;
    }

    public boolean tenantExists(String tenantId) {
        return tenantRepository.existsById(tenantId);
    }

    public record UpdateTenantCommand(
            String name,
            String description,
            TenantStatus status
    ) {
    }
}
