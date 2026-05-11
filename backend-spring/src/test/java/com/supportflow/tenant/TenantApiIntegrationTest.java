package com.supportflow.tenant;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.common.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(controllers = {TenantController.class, GlobalExceptionHandler.class})
class TenantApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantService tenantService;

    @Test
    void createTenantReturns201AndActiveStatus() throws Exception {
        Tenant tenant = tenant("tenant-1", "Acme Support", "acme");
        when(tenantService.createTenant("Acme Support", "acme", "Priority support")).thenReturn(tenant);

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Acme Support",
                                  "slug": "acme",
                                  "description": "Priority support"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("tenant-1"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void listTenantsReturnsKnownTenants() throws Exception {
        when(tenantService.listTenants()).thenReturn(List.of(tenant("tenant-1", "Acme Support", "acme")));

        mockMvc.perform(get("/api/v1/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("acme"));
    }

    @Test
    void getMissingTenantReturns404() throws Exception {
        when(tenantService.getTenant(anyString()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found"));

        mockMvc.perform(get("/api/v1/tenants/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Tenant not found")));
    }

    @Test
    void createDuplicateTenantSlugReturns409() throws Exception {
        when(tenantService.createTenant("Acme Support", "acme", null))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Tenant slug already exists"));

        mockMvc.perform(post("/api/v1/tenants")
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Acme Support",
                                  "slug": "acme"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("Tenant slug already exists")));
    }

    private Tenant tenant(String id, String name, String slug) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setName(name);
        tenant.setSlug(slug);
        tenant.setDescription("Priority support");
        tenant.setStatus(TenantStatus.ACTIVE);
        tenant.setCreatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        tenant.setUpdatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        return tenant;
    }
}
