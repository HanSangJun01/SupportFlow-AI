package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
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
