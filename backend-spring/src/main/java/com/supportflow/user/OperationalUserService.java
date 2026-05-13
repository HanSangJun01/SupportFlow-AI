package com.supportflow.user;

import com.supportflow.tenant.TenantService;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperationalUserService {

    private final OperationalUserRepository operationalUserRepository;
    private final TenantService tenantService;

    public OperationalUserService(OperationalUserRepository operationalUserRepository, TenantService tenantService) {
        this.operationalUserRepository = operationalUserRepository;
        this.tenantService = tenantService;
    }

    public OperationalUser createUser(String tenantId, CreateOperationalUserCommand command) {
        tenantService.getTenant(tenantId);
        Instant now = Instant.now();
        OperationalUser user = new OperationalUser();
        user.setTenantId(tenantId);
        user.setDisplayName(command.displayName());
        user.setEmail(command.email());
        user.setRole(command.role());
        user.setStatus(OperationalUserStatus.ACTIVE);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return operationalUserRepository.save(user);
    }

    public List<OperationalUser> listUsers(String tenantId) {
        tenantService.getTenant(tenantId);
        return operationalUserRepository.findByTenantId(tenantId);
    }

    public OperationalUser getUser(String tenantId, String userId) {
        tenantService.getTenant(tenantId);
        return findTenantUser(tenantId, userId);
    }

    public OperationalUser updateStatus(String tenantId, String userId, OperationalUserStatus status) {
        OperationalUser user = getUser(tenantId, userId);
        user.setStatus(status);
        user.setUpdatedAt(Instant.now());
        return operationalUserRepository.save(user);
    }

    public OperationalUser validateActiveActor(String tenantId, String actorUserId) {
        OperationalUser user = findTenantUser(tenantId, actorUserId);
        if (user.getStatus() != OperationalUserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Operational user is inactive");
        }
        return user;
    }

    public OperationalUser validateActiveSupportAgent(String tenantId, String assigneeId) {
        OperationalUser user = validateActiveActor(tenantId, assigneeId);
        if (user.getRole() != OperationalUserRole.SUPPORT_AGENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operational user is not a support agent");
        }
        return user;
    }

    private OperationalUser findTenantUser(String tenantId, String userId) {
        return operationalUserRepository.findByTenantIdAndId(tenantId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operational user not found"));
    }

    public record CreateOperationalUserCommand(
            String displayName,
            String email,
            OperationalUserRole role
    ) {
    }
}
