package com.supportflow.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Tenant workspace foundation APIs")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @Operation(summary = "Create a tenant workspace")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request.name(), request.slug(), request.description());
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenant.getId()))
                .body(TenantResponse.from(tenant));
    }

    @GetMapping
    @Operation(summary = "List tenant workspaces")
    public List<TenantResponse> listTenants() {
        return tenantService.listTenants().stream()
                .map(TenantResponse::from)
                .toList();
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Get a tenant workspace")
    public TenantResponse getTenant(@PathVariable String tenantId) {
        return TenantResponse.from(tenantService.getTenant(tenantId));
    }

    @PatchMapping("/{tenantId}")
    @Operation(summary = "Update tenant workspace metadata")
    public TenantResponse updateTenant(@PathVariable String tenantId, @RequestBody UpdateTenantRequest request) {
        return TenantResponse.from(tenantService.updateTenant(tenantId, new TenantService.UpdateTenantCommand(
                request.name(),
                request.description(),
                request.status()
        )));
    }

    public record CreateTenantRequest(
            @NotBlank String name,
            @NotBlank String slug,
            String description
    ) {
    }

    public record UpdateTenantRequest(
            String name,
            String description,
            TenantStatus status
    ) {
    }

    public record TenantResponse(
            String id,
            String name,
            String slug,
            String description,
            TenantStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        static TenantResponse from(Tenant tenant) {
            return new TenantResponse(
                    tenant.getId(),
                    tenant.getName(),
                    tenant.getSlug(),
                    tenant.getDescription(),
                    tenant.getStatus(),
                    tenant.getCreatedAt(),
                    tenant.getUpdatedAt()
            );
        }
    }
}
