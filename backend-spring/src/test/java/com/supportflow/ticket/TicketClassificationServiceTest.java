package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.supportflow.ai.AiClassificationClient;
import com.supportflow.ai.TicketClassificationException;
import com.supportflow.ai.TicketClassificationResponse;
import com.supportflow.tenant.TenantService;
import com.supportflow.user.OperationalUserService;
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
class TicketClassificationServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private OperationalUserService operationalUserService;

    @Mock
    private TicketStatusTransitionPolicy transitionPolicy;

    @Mock
    private AiClassificationClient aiClassificationClient;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void ticketClassificationAttemptsAreNullSafe() {
        Ticket ticket = new Ticket();

        ticket.setClassificationAttempts(null);

        assertThat(ticket.getClassificationAttempts()).isEmpty();

        TicketClassificationAttempt attempt = TicketClassificationAttempt.success(
                TicketClassificationTrigger.AUTO_ON_CREATE,
                null,
                Instant.parse("2026-06-11T08:00:00Z"),
                Instant.parse("2026-06-11T08:00:01Z"),
                "billing",
                TicketClassificationUrgency.NORMAL,
                TicketClassificationSentiment.NEUTRAL,
                TicketPriority.MEDIUM,
                0.74,
                "rules-v1"
        );
        ticket.setClassificationAttempts(List.of(attempt));

        assertThat(ticket.getClassificationAttempts()).singleElement()
                .satisfies(savedAttempt -> {
                    assertThat(savedAttempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.SUCCESS);
                    assertThat(savedAttempt.getTrigger()).isEqualTo(TicketClassificationTrigger.AUTO_ON_CREATE);
                    assertThat(savedAttempt.getClassifierVersion()).isEqualTo("rules-v1");
                });
    }

    @Test
    void ticketHistoryEntryCanLinkClassificationAttemptId() {
        TicketHistoryEntry entry = new TicketHistoryEntry(
                TicketHistoryEventType.AI_CLASSIFICATION_APPLIED,
                null,
                "attempt-1",
                Instant.parse("2026-06-11T08:00:01Z"),
                List.of(new TicketFieldChange("priority", "LOW", "HIGH"))
        );

        assertThat(entry.getClassificationAttemptId()).isEqualTo("attempt-1");
        assertThat(entry.getEventType()).isEqualTo(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED);
        assertThat(entry.getChanges()).singleElement()
                .satisfies(change -> assertThat(change.getField()).isEqualTo("priority"));
    }

    @Test
    void failedClassificationAttemptStoresErrorMetadata() {
        TicketClassificationAttempt attempt = TicketClassificationAttempt.failed(
                TicketClassificationTrigger.MANUAL_REANALYSIS,
                "actor-1",
                Instant.parse("2026-06-11T08:00:00Z"),
                Instant.parse("2026-06-11T08:00:01Z"),
                "AI_CLASSIFICATION_UNAVAILABLE",
                "AI service unavailable"
        );

        assertThat(attempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.FAILED);
        assertThat(attempt.getTrigger()).isEqualTo(TicketClassificationTrigger.MANUAL_REANALYSIS);
        assertThat(attempt.getActorUserId()).isEqualTo("actor-1");
        assertThat(attempt.getErrorCode()).isEqualTo("AI_CLASSIFICATION_UNAVAILABLE");
        assertThat(attempt.getErrorMessage()).isEqualTo("AI service unavailable");
    }

    @Test
    void automaticSuccessAppliesCategoryPriorityAndLinkedAiHistory() {
        String tenantId = "tenant-1";
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId("ticket-1");
            }
            return ticket;
        });
        when(aiClassificationClient.classify(any())).thenReturn(response("technical", TicketPriority.HIGH));

        Ticket created = ticketService.createTicket(tenantId, new TicketService.CreateTicketCommand(
                "Production outage",
                "Ada Lovelace",
                "ada@example.com",
                "The app is down",
                "general",
                TicketPriority.LOW,
                null
        ));

        assertThat(created.getCategory()).isEqualTo("technical");
        assertThat(created.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(created.getClassificationAttempts()).singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.SUCCESS);
                    assertThat(attempt.getTrigger()).isEqualTo(TicketClassificationTrigger.AUTO_ON_CREATE);
                    assertThat(attempt.getActorUserId()).isNull();
                    assertThat(attempt.getClassifierVersion()).isEqualTo("rules-v1");
                });
        TicketClassificationAttempt attempt = created.getClassificationAttempts().getFirst();
        assertThat(created.getHistory()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.getEventType()).isEqualTo(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED);
                    assertThat(entry.getActorUserId()).isNull();
                    assertThat(entry.getClassificationAttemptId()).isEqualTo(attempt.getId());
                    assertThat(entry.getChanges()).extracting(TicketFieldChange::getField)
                            .containsExactly("category", "priority");
                });
    }

    @Test
    void automaticFailureStoresFailedAttemptWithoutMutatingFieldsOrHistory() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId("ticket-1");
            }
            return ticket;
        });
        when(aiClassificationClient.classify(any()))
                .thenThrow(new TicketClassificationException("AI_CLASSIFICATION_UNAVAILABLE", "AI service down"));

        Ticket created = ticketService.createTicket("tenant-1", new TicketService.CreateTicketCommand(
                "Need help",
                "Ada Lovelace",
                "ada@example.com",
                "Please help",
                "general",
                TicketPriority.LOW,
                null
        ));

        assertThat(created.getCategory()).isEqualTo("general");
        assertThat(created.getPriority()).isEqualTo(TicketPriority.LOW);
        assertThat(created.getHistory()).isEmpty();
        assertThat(created.getHistory()).extracting(TicketHistoryEntry::getEventType)
                .doesNotContain(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED);
        assertThat(created.getClassificationAttempts()).singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.FAILED);
                    assertThat(attempt.getTrigger()).isEqualTo(TicketClassificationTrigger.AUTO_ON_CREATE);
                    assertThat(attempt.getErrorCode()).isEqualTo("AI_CLASSIFICATION_UNAVAILABLE");
                    assertThat(attempt.getErrorMessage()).isEqualTo("AI service down");
                });
    }

    @Test
    void manualSuccessRequiresActorAndAppendsAttemptAndLinkedHistory() {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.TRIAGED, "general", TicketPriority.LOW);
        when(ticketRepository.findByTenantIdAndId("tenant-1", "ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(aiClassificationClient.classify(any())).thenReturn(response("billing", TicketPriority.MEDIUM));

        Ticket updated = ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1");

        assertThat(updated.getCategory()).isEqualTo("billing");
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(updated.getClassificationAttempts()).singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.SUCCESS);
                    assertThat(attempt.getTrigger()).isEqualTo(TicketClassificationTrigger.MANUAL_REANALYSIS);
                    assertThat(attempt.getActorUserId()).isEqualTo("actor-1");
                });
        TicketClassificationAttempt attempt = updated.getClassificationAttempts().getFirst();
        assertThat(updated.getHistory()).singleElement()
                .satisfies(entry -> {
                    assertThat(entry.getEventType()).isEqualTo(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED);
                    assertThat(entry.getActorUserId()).isEqualTo("actor-1");
                    assertThat(entry.getClassificationAttemptId()).isEqualTo(attempt.getId());
                });
        verify(tenantService).requireActiveTenant("tenant-1");
        verify(operationalUserService).validateActiveActor("tenant-1", "actor-1");
    }

    @Test
    void manualFailureStoresFailedAttemptWithoutMutatingFieldsOrHistory() {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.TRIAGED, "account", TicketPriority.HIGH);
        when(ticketRepository.findByTenantIdAndId("tenant-1", "ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(aiClassificationClient.classify(any()))
                .thenThrow(new TicketClassificationException("AI_CLASSIFICATION_UNAVAILABLE", "AI service down"));

        Ticket updated = ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1");

        assertThat(updated.getCategory()).isEqualTo("account");
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(updated.getHistory()).isEmpty();
        assertThat(updated.getHistory()).extracting(TicketHistoryEntry::getEventType)
                .doesNotContain(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED);
        assertThat(updated.getClassificationAttempts()).singleElement()
                .satisfies(attempt -> {
                    assertThat(attempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.FAILED);
                    assertThat(attempt.getTrigger()).isEqualTo(TicketClassificationTrigger.MANUAL_REANALYSIS);
                    assertThat(attempt.getActorUserId()).isEqualTo("actor-1");
                });
    }

    @Test
    void manualReanalysisRejectsInactiveTenantBeforeCallingAi() {
        when(tenantService.requireActiveTenant("tenant-1"))
                .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Tenant is inactive"));

        assertThatThrownBy(() -> ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(ticketRepository, never()).findByTenantIdAndId("tenant-1", "ticket-1");
        verify(operationalUserService, never()).validateActiveActor("tenant-1", "actor-1");
        verify(aiClassificationClient, never()).classify(any());
    }

    @Test
    void manualReanalysisRejectsMissingTicketBeforeCallingAi() {
        when(ticketRepository.findByTenantIdAndId("tenant-1", "missing-ticket")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.reanalyzeTicket("tenant-1", "missing-ticket", "actor-1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(operationalUserService, never()).validateActiveActor("tenant-1", "actor-1");
        verify(aiClassificationClient, never()).classify(any());
    }

    @Test
    void manualReanalysisRejectsCrossTenantActorBeforeCallingAi() {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.TRIAGED, "account", TicketPriority.HIGH);
        when(ticketRepository.findByTenantIdAndId("tenant-1", "ticket-1")).thenReturn(Optional.of(ticket));
        when(operationalUserService.validateActiveActor("tenant-1", "actor-other-tenant"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Operational user not found"));

        assertThatThrownBy(() -> ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-other-tenant"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(((ResponseStatusException) exception).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(aiClassificationClient, never()).classify(any());
        verify(ticketRepository, never()).save(ticket);
    }

    @Test
    void manualReanalysisRejectsClosedTicketsBeforeCallingAi() {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.CLOSED, "account", TicketPriority.HIGH);
        when(ticketRepository.findByTenantIdAndId("tenant-1", "ticket-1")).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    ResponseStatusException responseStatusException = (ResponseStatusException) exception;
                    assertThat(responseStatusException.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(responseStatusException.getReason()).isEqualTo("Closed tickets cannot be classified");
                });

        verify(operationalUserService, never()).validateActiveActor("tenant-1", "actor-1");
        verify(aiClassificationClient, never()).classify(any());
    }

    @Test
    void repeatedManualReanalysisAppendsAttemptsWithoutReplacingHistory() {
        Ticket ticket = ticket("ticket-1", "tenant-1", TicketStatus.TRIAGED, "general", TicketPriority.LOW);
        when(ticketRepository.findByTenantIdAndId("tenant-1", "ticket-1")).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(aiClassificationClient.classify(any()))
                .thenReturn(response("billing", TicketPriority.MEDIUM))
                .thenReturn(response("technical", TicketPriority.HIGH));

        ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1");
        Ticket updated = ticketService.reanalyzeTicket("tenant-1", "ticket-1", "actor-1");

        assertThat(updated.getClassificationAttempts()).hasSize(2);
        assertThat(updated.getClassificationAttempts()).extracting(TicketClassificationAttempt::getCategory)
                .containsExactly("billing", "technical");
        assertThat(updated.getHistory()).hasSize(2);
        assertThat(updated.getHistory()).extracting(TicketHistoryEntry::getClassificationAttemptId)
                .containsExactly(
                        updated.getClassificationAttempts().get(0).getId(),
                        updated.getClassificationAttempts().get(1).getId()
                );
    }

    private TicketClassificationResponse response(String category, TicketPriority priority) {
        return new TicketClassificationResponse(
                category,
                TicketClassificationUrgency.NORMAL,
                TicketClassificationSentiment.NEUTRAL,
                priority,
                0.74,
                "rules-v1"
        );
    }

    private Ticket ticket(String id, String tenantId, TicketStatus status, String category, TicketPriority priority) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTenantId(tenantId);
        ticket.setSubject("Cannot log in");
        ticket.setCustomerName("Ada Lovelace");
        ticket.setCustomerEmail("ada@example.com");
        ticket.setCustomerMessage("Login fails after reset");
        ticket.setStatus(status);
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setCreatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        ticket.setUpdatedAt(Instant.parse("2026-05-11T00:00:00Z"));
        return ticket;
    }
}
