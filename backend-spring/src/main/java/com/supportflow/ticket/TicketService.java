package com.supportflow.ticket;

import com.supportflow.tenant.TenantService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TenantService tenantService;
    private final TicketStatusTransitionPolicy transitionPolicy;

    public TicketService(TicketRepository ticketRepository, TenantService tenantService,
            TicketStatusTransitionPolicy transitionPolicy) {
        this.ticketRepository = ticketRepository;
        this.tenantService = tenantService;
        this.transitionPolicy = transitionPolicy;
    }

    public Ticket createTicket(String tenantId, CreateTicketCommand command) {
        tenantService.getTenant(tenantId);
        Instant now = Instant.now();
        Ticket ticket = new Ticket();
        ticket.setTenantId(tenantId);
        ticket.setSubject(command.subject());
        ticket.setCustomerName(command.customerName());
        ticket.setCustomerEmail(command.customerEmail());
        ticket.setCustomerMessage(command.customerMessage());
        ticket.setCategory(command.category());
        ticket.setPriority(command.priority());
        ticket.setAssigneeId(command.assigneeId());
        ticket.setStatus(TicketStatus.NEW);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        return ticketRepository.save(ticket);
    }

    public List<Ticket> listTickets(String tenantId, TicketFilters filters) {
        tenantService.getTenant(tenantId);
        List<Ticket> tickets = new ArrayList<>(ticketRepository.findByTenantId(tenantId));
        return tickets.stream()
                .filter(ticket -> filters.status() == null || ticket.getStatus() == filters.status())
                .filter(ticket -> filters.priority() == null || ticket.getPriority() == filters.priority())
                .filter(ticket -> filters.assigneeId() == null || filters.assigneeId().equals(ticket.getAssigneeId()))
                .filter(ticket -> filters.createdFrom() == null || !ticket.getCreatedAt().isBefore(filters.createdFrom()))
                .filter(ticket -> filters.createdTo() == null || !ticket.getCreatedAt().isAfter(filters.createdTo()))
                .sorted(Comparator.comparing(Ticket::getCreatedAt))
                .toList();
    }

    public Ticket getTicket(String tenantId, String ticketId) {
        tenantService.getTenant(tenantId);
        return ticketRepository.findByTenantIdAndId(tenantId, ticketId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
    }

    public Ticket updateStatus(String tenantId, String ticketId, TicketStatus status) {
        Ticket ticket = getTicket(tenantId, ticketId);
        transitionPolicy.validateTransition(ticket.getStatus(), status);
        ticket.setStatus(status);
        ticket.setUpdatedAt(Instant.now());
        return ticketRepository.save(ticket);
    }

    public record CreateTicketCommand(
            String subject,
            String customerName,
            String customerEmail,
            String customerMessage,
            String category,
            TicketPriority priority,
            String assigneeId
    ) {
    }

    public record TicketFilters(
            TicketStatus status,
            TicketPriority priority,
            String assigneeId,
            Instant createdFrom,
            Instant createdTo
    ) {
    }
}
