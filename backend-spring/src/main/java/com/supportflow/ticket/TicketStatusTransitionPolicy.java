package com.supportflow.ticket;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class TicketStatusTransitionPolicy {

    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TicketStatus.NEW, EnumSet.of(TicketStatus.TRIAGED));
        ALLOWED_TRANSITIONS.put(TicketStatus.TRIAGED, EnumSet.of(TicketStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.ANSWERED));
        ALLOWED_TRANSITIONS.put(TicketStatus.ANSWERED, EnumSet.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class));
    }

    public boolean canTransition(TicketStatus from, TicketStatus to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    public void validateTransition(TicketStatus from, TicketStatus to) {
        if (!canTransition(from, to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid ticket status transition from " + from + " to " + to);
        }
    }
}
