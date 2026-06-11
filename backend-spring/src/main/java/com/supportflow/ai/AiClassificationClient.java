package com.supportflow.ai;

public interface AiClassificationClient {

    TicketClassificationResponse classify(TicketClassificationRequest request);
}
