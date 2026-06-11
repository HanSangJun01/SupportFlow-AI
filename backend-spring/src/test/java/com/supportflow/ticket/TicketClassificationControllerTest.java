package com.supportflow.ticket;

import static org.mockito.Mockito.when;
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
class TicketClassificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void reanalyzeTicketCreatesClassificationAttemptResponse() throws Exception {
        Ticket ticket = ticket("ticket-1", "tenant-1");
        TicketClassificationAttempt attempt = successAttempt("attempt-1", "actor-1");
        ticket.getClassificationAttempts().add(attempt);
        ticket.getHistory().add(new TicketHistoryEntry(
                TicketHistoryEventType.AI_CLASSIFICATION_APPLIED,
                "actor-1",
                attempt.getId(),
                Instant.parse("2026-06-11T08:00:01Z"),
                List.of(new TicketFieldChange("priority", "LOW", "MEDIUM"))
        ));
        when(ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1")).thenReturn(ticket);

        mockMvc.perform(post("/api/v1/tenants/tenant-1/tickets/ticket-1/classification-attempts")
                        .contentType("application/json")
                        .content("""
                                { "actorUserId": "actor-1" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classificationAttempts[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$.classificationAttempts[0].trigger").value("MANUAL_REANALYSIS"))
                .andExpect(jsonPath("$.classificationAttempts[0].actorUserId").value("actor-1"))
                .andExpect(jsonPath("$.classificationAttempts[0].classifierVersion").value("rules-v1"))
                .andExpect(jsonPath("$.history[0].eventType").value("AI_CLASSIFICATION_APPLIED"))
                .andExpect(jsonPath("$.history[0].classificationAttemptId").value("attempt-1"));
    }

    @Test
    void reanalyzeTicketRequiresActorUserId() throws Exception {
        mockMvc.perform(post("/api/v1/tenants/tenant-1/tickets/ticket-1/classification-attempts")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void manualAiFailureReturns200WithFailedAttempt() throws Exception {
        Ticket ticket = ticket("ticket-1", "tenant-1");
        ticket.getClassificationAttempts().add(TicketClassificationAttempt.failed(
                TicketClassificationTrigger.MANUAL_REANALYSIS,
                "actor-1",
                Instant.parse("2026-06-11T08:00:00Z"),
                Instant.parse("2026-06-11T08:00:01Z"),
                "AI_CLASSIFICATION_UNAVAILABLE",
                "AI service down"
        ));
        when(ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1")).thenReturn(ticket);

        mockMvc.perform(post("/api/v1/tenants/tenant-1/tickets/ticket-1/classification-attempts")
                        .contentType("application/json")
                        .content("""
                                { "actorUserId": "actor-1" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classificationAttempts[0].status").value("FAILED"))
                .andExpect(jsonPath("$.classificationAttempts[0].errorCode").value("AI_CLASSIFICATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.history").isEmpty());
    }

    private Ticket ticket(String id, String tenantId) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTenantId(tenantId);
        ticket.setSubject("Cannot log in");
        ticket.setCustomerName("Ada Lovelace");
        ticket.setCustomerEmail("ada@example.com");
        ticket.setCustomerMessage("Login fails after reset");
        ticket.setStatus(TicketStatus.TRIAGED);
        ticket.setCategory("general");
        ticket.setPriority(TicketPriority.LOW);
        ticket.setCreatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        ticket.setUpdatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        return ticket;
    }

    private TicketClassificationAttempt successAttempt(String id, String actorUserId) {
        TicketClassificationAttempt attempt = TicketClassificationAttempt.success(
                TicketClassificationTrigger.MANUAL_REANALYSIS,
                actorUserId,
                Instant.parse("2026-06-11T08:00:00Z"),
                Instant.parse("2026-06-11T08:00:01Z"),
                "billing",
                TicketClassificationUrgency.NORMAL,
                TicketClassificationSentiment.NEUTRAL,
                TicketPriority.MEDIUM,
                0.74,
                "rules-v1"
        );
        attempt.setId(id);
        return attempt;
    }
}
