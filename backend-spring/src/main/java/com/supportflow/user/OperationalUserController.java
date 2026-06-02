package com.supportflow.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/v1/tenants/{tenantId}/users")
@Tag(name = "Operational Users", description = "Tenant-local operational user metadata APIs")
public class OperationalUserController {

    private final OperationalUserService operationalUserService;

    public OperationalUserController(OperationalUserService operationalUserService) {
        this.operationalUserService = operationalUserService;
    }

    @PostMapping
    @Operation(summary = "Create tenant-local operational user metadata")
    public ResponseEntity<OperationalUserResponse> createUser(@PathVariable String tenantId,
            @Valid @RequestBody CreateOperationalUserRequest request) {
        OperationalUser user = operationalUserService.createUser(tenantId,
                new OperationalUserService.CreateOperationalUserCommand(
                        request.displayName(),
                        request.email(),
                        request.role()
                ));
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId + "/users/" + user.getId()))
                .body(OperationalUserResponse.from(user));
    }

    @GetMapping
    @Operation(summary = "List tenant-local operational users")
    public List<OperationalUserResponse> listUsers(@PathVariable String tenantId) {
        return operationalUserService.listUsers(tenantId).stream()
                .map(OperationalUserResponse::from)
                .toList();
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get tenant-local operational user metadata")
    public OperationalUserResponse getUser(@PathVariable String tenantId, @PathVariable String userId) {
        return OperationalUserResponse.from(operationalUserService.getUser(tenantId, userId));
    }

    @PatchMapping("/{userId}/status")
    @Operation(summary = "Update tenant-local operational user status")
    public OperationalUserResponse updateStatus(@PathVariable String tenantId, @PathVariable String userId,
            @Valid @RequestBody UpdateOperationalUserStatusRequest request) {
        return OperationalUserResponse.from(operationalUserService.updateStatus(tenantId, userId, request.status()));
    }

    public record CreateOperationalUserRequest(
            @NotBlank String displayName,
            @NotBlank @Email String email,
            @NotNull OperationalUserRole role
    ) {
    }

    public record UpdateOperationalUserStatusRequest(
            @NotNull OperationalUserStatus status
    ) {
    }

    public record OperationalUserResponse(
            String id,
            String tenantId,
            String displayName,
            String email,
            OperationalUserRole role,
            OperationalUserStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        static OperationalUserResponse from(OperationalUser user) {
            return new OperationalUserResponse(
                    user.getId(),
                    user.getTenantId(),
                    user.getDisplayName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
    }
}
