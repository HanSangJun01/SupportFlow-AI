package com.supportflow.ticket;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TicketRepository extends MongoRepository<Ticket, String> {

    List<Ticket> findByTenantId(String tenantId);

    List<Ticket> findAllByTenantId(String tenantId);

    Optional<Ticket> findByTenantIdAndId(String tenantId, String id);

    List<Ticket> findByTenantIdAndStatus(String tenantId, TicketStatus status);

    List<Ticket> findByTenantIdAndPriority(String tenantId, TicketPriority priority);

    List<Ticket> findByTenantIdAndAssigneeId(String tenantId, String assigneeId);

    List<Ticket> findByTenantIdAndCreatedAtBetween(String tenantId, Instant createdFrom, Instant createdTo);
}
