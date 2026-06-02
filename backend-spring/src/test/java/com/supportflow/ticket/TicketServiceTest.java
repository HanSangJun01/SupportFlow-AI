package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.supportflow.tenant.TenantService;
import com.supportflow.user.OperationalUserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TenantService tenantService;

    @Mock
    private OperationalUserService operationalUserService;

    @Mock
    private TicketStatusTransitionPolicy transitionPolicy;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicketValidatesNonNullAssigneeBeforeSaving() {
        String tenantId = "tenant-1";
        Ticket commandBackedTicket = ticket("ticket-1", tenantId, TicketStatus.NEW, TicketPriority.HIGH, "agent-7",
                "2026-05-11T12:00:00Z");
        when(ticketRepository.save(any(Ticket.class))).thenReturn(commandBackedTicket);

        Ticket created = ticketService.createTicket(tenantId, new TicketService.CreateTicketCommand(
                "Cannot log in",
                "Ada Lovelace",
                "ada@example.com",
                "Login fails after reset",
                "billing",
                TicketPriority.HIGH,
                "agent-7"
        ));

        assertThat(created.getAssigneeId()).isEqualTo("agent-7");
        verify(tenantService).requireActiveTenant(tenantId);
        verify(operationalUserService).validateActiveSupportAgent(tenantId, "agent-7");
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void createTicketRejectsInvalidAssigneeBeforeSaving() {
        String tenantId = "tenant-1";
        when(operationalUserService.validateActiveSupportAgent(tenantId, "agent-7"))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Operational user not found"));

        assertThatThrownBy(() -> ticketService.createTicket(tenantId, new TicketService.CreateTicketCommand(
                "Cannot log in",
                "Ada Lovelace",
                "ada@example.com",
                "Login fails after reset",
                "billing",
                TicketPriority.HIGH,
                "agent-7"
        )))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(tenantService).requireActiveTenant(tenantId);
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    @Test
    void listTicketsAppliesStatusPriorityAssigneeAndCreatedDateFilters() {
        String tenantId = "tenant-1";
        when(ticketRepository.findByTenantId(tenantId)).thenReturn(List.of(
                ticket("match", tenantId, TicketStatus.NEW, TicketPriority.HIGH, "agent-7",
                        "2026-05-11T12:00:00Z"),
                ticket("wrong-status", tenantId, TicketStatus.TRIAGED, TicketPriority.HIGH, "agent-7",
                        "2026-05-11T12:00:00Z"),
                ticket("wrong-priority", tenantId, TicketStatus.NEW, TicketPriority.NORMAL, "agent-7",
                        "2026-05-11T12:00:00Z"),
                ticket("wrong-assignee", tenantId, TicketStatus.NEW, TicketPriority.HIGH, "agent-8",
                        "2026-05-11T12:00:00Z"),
                ticket("too-old", tenantId, TicketStatus.NEW, TicketPriority.HIGH, "agent-7",
                        "2026-05-10T23:59:59Z"),
                ticket("too-new", tenantId, TicketStatus.NEW, TicketPriority.HIGH, "agent-7",
                        "2026-05-12T00:00:01Z")
        ));

        List<Ticket> tickets = ticketService.listTickets(tenantId, new TicketService.TicketFilters(
                TicketStatus.NEW,
                TicketPriority.HIGH,
                "agent-7",
                Instant.parse("2026-05-11T00:00:00Z"),
                Instant.parse("2026-05-12T00:00:00Z")
        ));

        assertThat(tickets).extracting(Ticket::getId).containsExactly("match");
        verify(tenantService).getTenant(tenantId);
        verify(ticketRepository).findByTenantId(tenantId);
    }

    @Test
    void updateStatusRequiresActiveActorAndAppendsStatusHistory() {
        String tenantId = "tenant-1";
        String ticketId = "ticket-1";
        Ticket ticket = ticket(ticketId, tenantId, TicketStatus.NEW, TicketPriority.HIGH, "agent-7",
                "2026-05-11T12:00:00Z");
        when(ticketRepository.findByTenantIdAndId(tenantId, ticketId)).thenReturn(java.util.Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket updated = ticketService.updateStatus(tenantId, ticketId, TicketStatus.TRIAGED, "actor-1");

        assertThat(updated.getStatus()).isEqualTo(TicketStatus.TRIAGED);
        assertThat(updated.getHistory()).hasSize(1);
        TicketHistoryEntry entry = updated.getHistory().getFirst();
        assertThat(entry.getEventType()).isEqualTo(TicketHistoryEventType.STATUS_CHANGED);
        assertThat(entry.getActorUserId()).isEqualTo("actor-1");
        assertThat(entry.getChanges()).singleElement()
                .satisfies(change -> {
                    assertThat(change.getField()).isEqualTo("status");
                    assertThat(change.getOldValue()).isEqualTo("NEW");
                    assertThat(change.getNewValue()).isEqualTo("TRIAGED");
                });
        verify(tenantService).requireActiveTenant(tenantId);
        verify(tenantService).getTenant(tenantId);
        verify(operationalUserService).validateActiveActor(tenantId, "actor-1");
        verify(transitionPolicy).validateTransition(TicketStatus.NEW, TicketStatus.TRIAGED);
    }

    @Test
    void updateWorkflowMetadataValidatesActorAndAssigneeThenAppendsChangedFields() {
        String tenantId = "tenant-1";
        String ticketId = "ticket-1";
        Ticket ticket = ticket(ticketId, tenantId, TicketStatus.TRIAGED, TicketPriority.NORMAL, "agent-7",
                "2026-05-11T12:00:00Z");
        ticket.setCategory("billing");
        when(ticketRepository.findByTenantIdAndId(tenantId, ticketId)).thenReturn(java.util.Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);

        Ticket updated = ticketService.updateWorkflowMetadata(tenantId, ticketId,
                new TicketService.UpdateWorkflowMetadataCommand("actor-1", "agent-8", TicketPriority.HIGH,
                        "technical"));

        assertThat(updated.getAssigneeId()).isEqualTo("agent-8");
        assertThat(updated.getPriority()).isEqualTo(TicketPriority.HIGH);
        assertThat(updated.getCategory()).isEqualTo("technical");
        assertThat(updated.getHistory()).hasSize(1);
        TicketHistoryEntry entry = updated.getHistory().getFirst();
        assertThat(entry.getEventType()).isEqualTo(TicketHistoryEventType.WORKFLOW_METADATA_CHANGED);
        assertThat(entry.getActorUserId()).isEqualTo("actor-1");
        assertThat(entry.getChanges()).extracting(TicketFieldChange::getField)
                .containsExactly("assigneeId", "priority", "category");
        assertThat(entry.getChanges()).extracting(TicketFieldChange::getOldValue)
                .containsExactly("agent-7", "NORMAL", "billing");
        assertThat(entry.getChanges()).extracting(TicketFieldChange::getNewValue)
                .containsExactly("agent-8", "HIGH", "technical");
        verify(tenantService).requireActiveTenant(tenantId);
        verify(operationalUserService).validateActiveActor(tenantId, "actor-1");
        verify(operationalUserService).validateActiveSupportAgent(tenantId, "agent-8");
    }

    @Test
    void updateWorkflowMetadataRejectsClosedTickets() {
        String tenantId = "tenant-1";
        String ticketId = "ticket-1";
        Ticket ticket = ticket(ticketId, tenantId, TicketStatus.CLOSED, TicketPriority.NORMAL, "agent-7",
                "2026-05-11T12:00:00Z");
        when(ticketRepository.findByTenantIdAndId(tenantId, ticketId)).thenReturn(java.util.Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateWorkflowMetadata(tenantId, ticketId,
                new TicketService.UpdateWorkflowMetadataCommand("actor-1", "agent-8", TicketPriority.HIGH,
                        "technical")))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(exception -> ((ResponseStatusException) exception).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        verify(operationalUserService, never()).validateActiveActor(tenantId, "actor-1");
        verify(ticketRepository, never()).save(ticket);
    }

    @Test
    void updateWorkflowMetadataDoesNotAppendHistoryWhenValuesMatch() {
        String tenantId = "tenant-1";
        String ticketId = "ticket-1";
        Ticket ticket = ticket(ticketId, tenantId, TicketStatus.TRIAGED, TicketPriority.NORMAL, "agent-7",
                "2026-05-11T12:00:00Z");
        ticket.setCategory("billing");
        when(ticketRepository.findByTenantIdAndId(tenantId, ticketId)).thenReturn(java.util.Optional.of(ticket));

        Ticket updated = ticketService.updateWorkflowMetadata(tenantId, ticketId,
                new TicketService.UpdateWorkflowMetadataCommand("actor-1", "agent-7", TicketPriority.NORMAL,
                        "billing"));

        assertThat(updated.getHistory()).isEmpty();
        verify(operationalUserService).validateActiveActor(tenantId, "actor-1");
        verify(operationalUserService).validateActiveSupportAgent(tenantId, "agent-7");
        verify(ticketRepository, never()).save(ticket);
    }

    private Ticket ticket(String id, String tenantId, TicketStatus status, TicketPriority priority, String assigneeId,
            String createdAt) {
        Ticket ticket = new Ticket();
        ticket.setId(id);
        ticket.setTenantId(tenantId);
        ticket.setSubject("Cannot log in");
        ticket.setCustomerName("Ada Lovelace");
        ticket.setCustomerEmail("ada@example.com");
        ticket.setCustomerMessage("Login fails after reset");
        ticket.setStatus(status);
        ticket.setPriority(priority);
        ticket.setAssigneeId(assigneeId);
        ticket.setCreatedAt(Instant.parse(createdAt));
        ticket.setUpdatedAt(Instant.parse(createdAt));
        return ticket;
    }
}
