package com.supportflow.ticket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/tickets")
@Tag(name = "Tickets", description = "Tenant-scoped ticket foundation APIs")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Create a tenant-scoped ticket")
    public ResponseEntity<TicketResponse> createTicket(@PathVariable String tenantId,
            @Valid @RequestBody CreateTicketRequest request) {
        Ticket ticket = ticketService.createTicket(tenantId, new TicketService.CreateTicketCommand(
                request.subject(),
                request.customerName(),
                request.customerEmail(),
                request.customerMessage(),
                request.category(),
                request.priority(),
                request.assigneeId()
        ));
        return ResponseEntity.created(URI.create("/api/v1/tenants/" + tenantId + "/tickets/" + ticket.getId()))
                .body(TicketResponse.from(ticket));
    }

    @GetMapping
    @Operation(summary = "List tenant-scoped tickets")
    public List<TicketResponse> listTickets(
            @PathVariable String tenantId,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String assigneeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo
    ) {
        return ticketService.listTickets(tenantId,
                        new TicketService.TicketFilters(status, priority, assigneeId, createdFrom, createdTo))
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Get a tenant-scoped ticket")
    public TicketResponse getTicket(@PathVariable String tenantId, @PathVariable String ticketId) {
        return TicketResponse.from(ticketService.getTicket(tenantId, ticketId));
    }

    @PatchMapping("/{ticketId}/status")
    @Operation(summary = "Update a tenant-scoped ticket status")
    public TicketResponse updateStatus(@PathVariable String tenantId, @PathVariable String ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        return TicketResponse.from(ticketService.updateStatus(tenantId, ticketId, request.status()));
    }

    public record CreateTicketRequest(
            @NotBlank String subject,
            @NotBlank String customerName,
            @NotBlank @Email String customerEmail,
            @NotBlank String customerMessage,
            String category,
            TicketPriority priority,
            String assigneeId
    ) {
    }

    public record UpdateTicketStatusRequest(
            @jakarta.validation.constraints.NotNull TicketStatus status
    ) {
    }

    public record TicketResponse(
            String id,
            String tenantId,
            String subject,
            String customerName,
            String customerEmail,
            String customerMessage,
            TicketStatus status,
            String category,
            TicketPriority priority,
            String assigneeId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(
                    ticket.getId(),
                    ticket.getTenantId(),
                    ticket.getSubject(),
                    ticket.getCustomerName(),
                    ticket.getCustomerEmail(),
                    ticket.getCustomerMessage(),
                    ticket.getStatus(),
                    ticket.getCategory(),
                    ticket.getPriority(),
                    ticket.getAssigneeId(),
                    ticket.getCreatedAt(),
                    ticket.getUpdatedAt()
            );
        }
    }
}
