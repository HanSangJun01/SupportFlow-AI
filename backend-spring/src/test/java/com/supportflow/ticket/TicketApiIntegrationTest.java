package com.supportflow.ticket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void updateStatusReturnsUpdatedTicket() throws Exception {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.TRIAGED);
        ticket.getHistory().add(new TicketHistoryEntry(
                TicketHistoryEventType.STATUS_CHANGED,
                "actor-1",
                Instant.parse("2026-05-11T01:00:00Z"),
                List.of(new TicketFieldChange("status", "NEW", "TRIAGED"))
        ));
        when(ticketService.updateStatus("tenant-1", "ticket-1", TicketStatus.TRIAGED, "actor-1"))
                .thenReturn(ticket);

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/tickets/ticket-1/status")
                        .contentType("application/json")
                        .content("""
                                { "status": "TRIAGED", "actorUserId": "actor-1" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TRIAGED"))
                .andExpect(jsonPath("$.history[0].eventType").value("STATUS_CHANGED"));
    }

    @Test
    void invalidStatusTransitionReturns400() throws Exception {
        when(ticketService.updateStatus("tenant-1", "ticket-1", TicketStatus.CLOSED, "actor-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ticket status transition"));

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/tickets/ticket-1/status")
                        .contentType("application/json")
                        .content("""
                                { "status": "CLOSED", "actorUserId": "actor-1" }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWorkflowReturnsUpdatedTicketAndHistory() throws Exception {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.TRIAGED);
        ticket.setPriority(TicketPriority.URGENT);
        ticket.setAssigneeId("agent-8");
        ticket.setCategory("technical");
        ticket.getHistory().add(new TicketHistoryEntry(
                TicketHistoryEventType.WORKFLOW_METADATA_CHANGED,
                "actor-1",
                Instant.parse("2026-05-11T01:00:00Z"),
                List.of(
                        new TicketFieldChange("assigneeId", "agent-7", "agent-8"),
                        new TicketFieldChange("priority", "HIGH", "URGENT"),
                        new TicketFieldChange("category", null, "technical")
                )
        ));
        when(ticketService.updateWorkflowMetadata(eq("tenant-1"), eq("ticket-1"),
                any(TicketService.UpdateWorkflowMetadataCommand.class))).thenReturn(ticket);

        mockMvc.perform(patch("/api/v1/tenants/tenant-1/tickets/ticket-1/workflow")
                        .contentType("application/json")
                        .content("""
                                {
                                  "actorUserId": "actor-1",
                                  "assigneeId": "agent-8",
                                  "priority": "URGENT",
                                  "category": "technical"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assigneeId").value("agent-8"))
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.category").value("technical"))
                .andExpect(jsonPath("$.history[0].eventType").value("WORKFLOW_METADATA_CHANGED"))
                .andExpect(jsonPath("$.history[0].changes[0].field").value("assigneeId"));
    }

    @Test
    void updateWorkflowRequiresActorUserId() throws Exception {
        mockMvc.perform(patch("/api/v1/tenants/tenant-1/tickets/ticket-1/workflow")
                        .contentType("application/json")
                        .content("""
                                { "priority": "HIGH" }
                                """))
                .andExpect(status().isBadRequest());
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
