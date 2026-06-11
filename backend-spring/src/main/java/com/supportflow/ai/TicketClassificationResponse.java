package com.supportflow.ai;

import com.supportflow.ticket.TicketClassificationSentiment;
import com.supportflow.ticket.TicketClassificationUrgency;
import com.supportflow.ticket.TicketPriority;

public record TicketClassificationResponse(
        String category,
        TicketClassificationUrgency urgency,
        TicketClassificationSentiment sentiment,
        TicketPriority priority,
        double confidence,
        String classifierVersion
) {
}
