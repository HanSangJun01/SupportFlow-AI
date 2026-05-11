package com.supportflow.ticket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportflow.common.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {TicketController.class, GlobalExceptionHandler.class})
class TicketApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void createTicketReturns201AndNewStatus() throws Exception {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.NEW);
        when(ticketService.createTicket(eq("tenant-1"), any(TicketService.CreateTicketCommand.class)))
                .thenReturn(ticket);

        mockMvc.perform(post("/api/v1/tenants/tenant-1/tickets")
                        .contentType("application/json")
                        .content("""
                                {
                                  "subject": "Cannot log in",
                                  "customerName": "Ada Lovelace",
                                  "customerEmail": "ada@example.com",
                                  "customerMessage": "Login fails after reset",
                                  "priority": "HIGH",
                                  "assigneeId": "agent-7"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value("tenant-1"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void listTicketsSupportsStatusPriorityAssigneeAndCreatedDateFilters() throws Exception {
        when(ticketService.listTickets(eq("tenant-1"), any(TicketService.TicketFilters.class)))
                .thenReturn(List.of(ticket("ticket-1", "tenant-1", TicketStatus.NEW)));

        mockMvc.perform(get("/api/v1/tenants/tenant-1/tickets")
                        .param("status", "NEW")
                        .param("priority", "HIGH")
                        .param("assigneeId", "agent-7")
                        .param("createdFrom", "2026-05-01T00:00:00Z")
                        .param("createdTo", "2026-05-31T23:59:59Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ticket-1"));
    }

    @Test
    void getTicketUsesTenantScopedPath() throws Exception {
        when(ticketService.getTicket("tenant-1", "ticket-1"))
                .thenReturn(ticket("ticket-1", "tenant-1", TicketStatus.NEW));

        mockMvc.perform(get("/api/v1/tenants/tenant-1/tickets/ticket-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ticket-1"))
                .andExpect(jsonPath("$.tenantId").value("tenant-1"));
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
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setAssigneeId("agent-7");
        ticket.setCreatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        ticket.setUpdatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        return ticket;
    }
}
