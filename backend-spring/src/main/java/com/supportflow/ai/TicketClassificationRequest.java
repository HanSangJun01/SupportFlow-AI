package com.supportflow.ai;

public record TicketClassificationRequest(
        String tenantId,
        String ticketId,
        String subject,
        String customerMessage
) {
}
