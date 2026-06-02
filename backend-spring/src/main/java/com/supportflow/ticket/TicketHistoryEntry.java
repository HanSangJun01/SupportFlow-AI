package com.supportflow.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TicketHistoryEntry {

    private TicketHistoryEventType eventType;
    private String actorUserId;
    private Instant occurredAt;
    private List<TicketFieldChange> changes = new ArrayList<>();

    public TicketHistoryEntry() {
    }

    public TicketHistoryEntry(TicketHistoryEventType eventType, String actorUserId, Instant occurredAt,
            List<TicketFieldChange> changes) {
        this.eventType = eventType;
        this.actorUserId = actorUserId;
        this.occurredAt = occurredAt;
        setChanges(changes);
    }

    public TicketHistoryEventType getEventType() {
        return eventType;
    }

    public void setEventType(TicketHistoryEventType eventType) {
        this.eventType = eventType;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public List<TicketFieldChange> getChanges() {
        if (changes == null) {
            changes = new ArrayList<>();
        }
        return changes;
    }

    public void setChanges(List<TicketFieldChange> changes) {
        this.changes = changes == null ? new ArrayList<>() : new ArrayList<>(changes);
    }
}
