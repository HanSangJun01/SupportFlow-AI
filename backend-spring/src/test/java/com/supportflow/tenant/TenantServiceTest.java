package com.supportflow.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void createTenantRejectsDuplicateSlugBeforeSaving() {
        when(tenantRepository.existsBySlug("acme")).thenReturn(true);

        assertThatThrownBy(() -> tenantService.createTenant("Acme Support", "acme", null))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Tenant slug already exists");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void tenantStatusIncludesInactive() {
        assertThat(Arrays.asList(TenantStatus.values())).contains(TenantStatus.INACTIVE);
    }

    @Test
    void updateTenantChangesSuppliedMetadataOnlyAndKeepsSlug() {
        Tenant tenant = tenant();
        Instant originalUpdatedAt = tenant.getUpdatedAt();
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant updated = tenantService.updateTenant("tenant-1", new TenantService.UpdateTenantCommand(
                "Acme Operations",
                "Internal support",
                TenantStatus.INACTIVE
        ));

        assertThat(updated.getName()).isEqualTo("Acme Operations");
        assertThat(updated.getDescription()).isEqualTo("Internal support");
        assertThat(updated.getStatus()).isEqualTo(TenantStatus.INACTIVE);
        assertThat(updated.getSlug()).isEqualTo("acme");
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void updateTenantCommandDoesNotExposeSlug() {
        assertThat(TenantService.UpdateTenantCommand.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("slug");
    }

    @Test
    void updateTenantRejectsBlankName() {
        Tenant tenant = tenant();
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.updateTenant("tenant-1", new TenantService.UpdateTenantCommand(
                "   ",
                null,
                null
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Tenant name must not be blank");

        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    void requireActiveTenantRejectsInactiveTenant() {
        Tenant tenant = tenant();
        tenant.setStatus(TenantStatus.INACTIVE);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.requireActiveTenant("tenant-1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Tenant is inactive");
    }

    private Tenant tenant() {
        Tenant tenant = new Tenant();
        tenant.setId("tenant-1");
        tenant.setName("Acme Support");
        tenant.setSlug("acme");
        tenant.setDescription("Priority support");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        tenant.setUpdatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        return tenant;
    }
}
