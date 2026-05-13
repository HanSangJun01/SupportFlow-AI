package com.supportflow.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.common.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = {OperationalUserController.class, GlobalExceptionHandler.class})
class OperationalUserApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OperationalUserService operationalUserService;

    @Test
    void createUserReturns201AndActiveMetadata() throws Exception {
        OperationalUser user = user("user-1", "tenant-1", OperationalUserRole.SUPPORT_AGENT,
                OperationalUserStatus.ACTIVE);
        when(operationalUserService.createUser(any(), any())).thenReturn(user);

        mockMvc.perform(post("/api/v1/tenants/{tenantId}/users", "tenant-1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "displayName": "Ada Lovelace",
                                  "email": "ada@example.com",
                                  "role": "SUPPORT_AGENT"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.tenantId").value("tenant-1"))
                .andExpect(jsonPath("$.role").value("SUPPORT_AGENT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(operationalUserService).createUser("tenant-1",
                new OperationalUserService.CreateOperationalUserCommand(
                        "Ada Lovelace",
                        "ada@example.com",
                        OperationalUserRole.SUPPORT_AGENT
                ));
    }

    @Test
    void listUsersReturnsTenantUsers() throws Exception {
        when(operationalUserService.listUsers("tenant-1")).thenReturn(List.of(user("user-1", "tenant-1",
                OperationalUserRole.TENANT_ADMIN, OperationalUserStatus.ACTIVE)));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/users", "tenant-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tenantId").value("tenant-1"))
                .andExpect(jsonPath("$[0].role").value("TENANT_ADMIN"));
    }

    @Test
    void getCrossTenantUserReturns404() throws Exception {
        when(operationalUserService.getUser("tenant-1", "user-2"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Operational user not found"));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/users/{userId}", "tenant-1", "user-2"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Operational user not found")));
    }

    @Test
    void updateUserStatusReturnsInactiveUser() throws Exception {
        OperationalUser user = user("user-1", "tenant-1", OperationalUserRole.SUPPORT_AGENT,
                OperationalUserStatus.INACTIVE);
        when(operationalUserService.updateStatus("tenant-1", "user-1", OperationalUserStatus.INACTIVE))
                .thenReturn(user);

        mockMvc.perform(patch("/api/v1/tenants/{tenantId}/users/{userId}/status", "tenant-1", "user-1")
                        .contentType("application/json")
                        .content("""
                                { "status": "INACTIVE" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void requestRecordsDoNotExposeAuthenticationFields() {
        assertThat(Arrays.asList(OperationalUserController.CreateOperationalUserRequest.class.getRecordComponents()))
                .extracting(component -> component.getName())
                .doesNotContain("password", "credential", "session", "auth");
        assertThat(Arrays.asList(OperationalUserController.UpdateOperationalUserStatusRequest.class.getRecordComponents()))
                .extracting(component -> component.getName())
                .doesNotContain("password", "credential", "session", "auth");
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
