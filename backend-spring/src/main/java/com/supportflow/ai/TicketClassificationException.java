package com.supportflow.ai;

public class TicketClassificationException extends RuntimeException {

    private final String errorCode;

    public TicketClassificationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TicketClassificationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
