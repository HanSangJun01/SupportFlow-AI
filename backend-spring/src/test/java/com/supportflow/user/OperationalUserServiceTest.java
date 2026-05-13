package com.supportflow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.supportflow.tenant.Tenant;
import com.supportflow.tenant.TenantService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OperationalUserServiceTest {

    @Mock
    private OperationalUserRepository operationalUserRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private OperationalUserService operationalUserService;

    @Test
    void createUserStoresTenantScopedActiveMetadata() {
        when(tenantService.getTenant("tenant-1")).thenReturn(new Tenant());
        when(operationalUserRepository.save(any(OperationalUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OperationalUser user = operationalUserService.createUser("tenant-1",
                new OperationalUserService.CreateOperationalUserCommand(
                        "Ada Lovelace",
                        "ada@example.com",
                        OperationalUserRole.SUPPORT_AGENT
                ));

        assertThat(user.getTenantId()).isEqualTo("tenant-1");
        assertThat(user.getDisplayName()).isEqualTo("Ada Lovelace");
        assertThat(user.getEmail()).isEqualTo("ada@example.com");
        assertThat(user.getRole()).isEqualTo(OperationalUserRole.SUPPORT_AGENT);
        assertThat(user.getStatus()).isEqualTo(OperationalUserStatus.ACTIVE);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getUpdatedAt()).isNotNull();
        verify(tenantService).getTenant("tenant-1");
    }

    @Test
    void listUsersUsesTenantScopedRepositoryQuery() {
        OperationalUser user = user("user-1", "tenant-1", OperationalUserRole.TENANT_ADMIN,
                OperationalUserStatus.ACTIVE);
        when(tenantService.getTenant("tenant-1")).thenReturn(new Tenant());
        when(operationalUserRepository.findByTenantId("tenant-1")).thenReturn(List.of(user));

        assertThat(operationalUserService.listUsers("tenant-1")).containsExactly(user);
    }

    @Test
    void getUserRejectsCrossTenantUserAsNotFound() {
        when(tenantService.getTenant("tenant-1")).thenReturn(new Tenant());
        when(operationalUserRepository.findByTenantIdAndId("tenant-1", "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operationalUserService.getUser("tenant-1", "user-2"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("Operational user not found");
    }

    @Test
    void updateStatusChangesStatusAndTimestamp() {
        OperationalUser user = user("user-1", "tenant-1", OperationalUserRole.SUPPORT_AGENT,
                OperationalUserStatus.ACTIVE);
        Instant originalUpdatedAt = user.getUpdatedAt();
        when(tenantService.getTenant("tenant-1")).thenReturn(new Tenant());
        when(operationalUserRepository.findByTenantIdAndId("tenant-1", "user-1")).thenReturn(Optional.of(user));
        when(operationalUserRepository.save(any(OperationalUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OperationalUser updated = operationalUserService.updateStatus("tenant-1", "user-1",
                OperationalUserStatus.INACTIVE);

        assertThat(updated.getStatus()).isEqualTo(OperationalUserStatus.INACTIVE);
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void validateActiveActorRejectsMissingOrInactiveUsers() {
        OperationalUser inactive = user("user-1", "tenant-1", OperationalUserRole.SUPPORT_AGENT,
                OperationalUserStatus.INACTIVE);
        when(operationalUserRepository.findByTenantIdAndId("tenant-1", "missing")).thenReturn(Optional.empty());
        when(operationalUserRepository.findByTenantIdAndId("tenant-1", "user-1")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> operationalUserService.validateActiveActor("tenant-1", "missing"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> operationalUserService.validateActiveActor("tenant-1", "user-1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Operational user is inactive");
    }

    @Test
    void validateActiveSupportAgentRequiresActiveSupportAgentInSameTenant() {
        OperationalUser admin = user("admin-1", "tenant-1", OperationalUserRole.TENANT_ADMIN,
                OperationalUserStatus.ACTIVE);
        OperationalUser agent = user("agent-1", "tenant-1", OperationalUserRole.SUPPORT_AGENT,
                OperationalUserStatus.ACTIVE);
        when(operationalUserRepository.findByTenantIdAndId("tenant-1", "admin-1")).thenReturn(Optional.of(admin));
        when(operationalUserRepository.findByTenantIdAndId("tenant-1", "agent-1")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> operationalUserService.validateActiveSupportAgent("tenant-1", "admin-1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("Operational user is not a support agent");

        assertThat(operationalUserService.validateActiveSupportAgent("tenant-1", "agent-1")).isEqualTo(agent);
    }

    private OperationalUser user(String id, String tenantId, OperationalUserRole role, OperationalUserStatus status) {
        OperationalUser user = new OperationalUser();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setDisplayName("Ada Lovelace");
        user.setEmail("ada@example.com");
        user.setRole(role);
        user.setStatus(status);
        user.setCreatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        user.setUpdatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        return user;
    }
}
