package com.supportflow.ticket;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

@WebMvcTest(controllers = {TicketController.class, GlobalExceptionHandler.class})
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void tenantACanReadOwnTicketButTenantBCannotReadOrMutateIt() throws Exception {
        String tenantA = "tenantA";
        String tenantB = "tenantB";
        String ticketA = "ticketA";
        when(ticketService.getTicket(tenantA, ticketA)).thenReturn(ticket(ticketA, tenantA, TicketStatus.NEW));
        when(ticketService.getTicket(tenantB, ticketA))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        when(ticketService.updateStatus(tenantB, ticketA, TicketStatus.TRIAGED))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        when(ticketService.listTickets(tenantB, new TicketService.TicketFilters(null, null, null, null, null)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/tickets/{ticketId}", tenantA, ticketA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(tenantA));

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/tickets/{ticketId}", tenantB, ticketA))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/tenants/{tenantId}/tickets/{ticketId}/status", tenantB, ticketA)
                        .contentType("application/json")
                        .content("""
                                { "status": "TRIAGED" }
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/tenants/{tenantId}/tickets", tenantB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    private Ticket ticket(String id, String tenantId, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTenantId(tenantId);
        ticket.setSubject("Cannot log in");
        ticket.setCustomerName("Ada Lovelace");
        ticket.setCustomerEmail("ada@example.com");
        ticket.setCustomerMessage("Login fails after reset");
        ticket.setStatus(status);
        ticket.setCreatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        ticket.setUpdatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        return ticket;
    }
}
