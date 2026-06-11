package com.supportflow.ticket;

import java.time.Instant;
import java.util.UUID;

public class TicketClassificationAttempt {

    private String id;
    private TicketClassificationAttemptStatus status;
    private TicketClassificationTrigger trigger;
    private String actorUserId;
    private Instant requestedAt;
    private Instant completedAt;
    private String category;
    private TicketClassificationUrgency urgency;
    private TicketClassificationSentiment sentiment;
    private TicketPriority priority;
    private Double confidence;
    private String classifierVersion;
    private String errorCode;
    private String errorMessage;

    public static TicketClassificationAttempt success(TicketClassificationTrigger trigger, String actorUserId,
            Instant requestedAt, Instant completedAt, String category, TicketClassificationUrgency urgency,
            TicketClassificationSentiment sentiment, TicketPriority priority, double confidence,
            String classifierVersion) {
        TicketClassificationAttempt attempt = new TicketClassificationAttempt();
        attempt.setId(UUID.randomUUID().toString());
        attempt.setStatus(TicketClassificationAttemptStatus.SUCCESS);
        attempt.setTrigger(trigger);
        attempt.setActorUserId(actorUserId);
        attempt.setRequestedAt(requestedAt);
        attempt.setCompletedAt(completedAt);
        attempt.setCategory(category);
        attempt.setUrgency(urgency);
        attempt.setSentiment(sentiment);
        attempt.setPriority(priority);
        attempt.setConfidence(confidence);
        attempt.setClassifierVersion(classifierVersion);
        return attempt;
    }

    public static TicketClassificationAttempt failed(TicketClassificationTrigger trigger, String actorUserId,
            Instant requestedAt, Instant completedAt, String errorCode, String errorMessage) {
        TicketClassificationAttempt attempt = new TicketClassificationAttempt();
        attempt.setId(UUID.randomUUID().toString());
        attempt.setStatus(TicketClassificationAttemptStatus.FAILED);
        attempt.setTrigger(trigger);
        attempt.setActorUserId(actorUserId);
        attempt.setRequestedAt(requestedAt);
        attempt.setCompletedAt(completedAt);
        attempt.setErrorCode(errorCode);
        attempt.setErrorMessage(errorMessage);
        return attempt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TicketClassificationAttemptStatus getStatus() {
        return status;
    }

    public void setStatus(TicketClassificationAttemptStatus status) {
        this.status = status;
    }

    public TicketClassificationTrigger getTrigger() {
        return trigger;
    }

    public void setTrigger(TicketClassificationTrigger trigger) {
        this.trigger = trigger;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public void setActorUserId(String actorUserId) {
        this.actorUserId = actorUserId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public TicketClassificationUrgency getUrgency() {
        return urgency;
    }

    public void setUrgency(TicketClassificationUrgency urgency) {
        this.urgency = urgency;
    }

    public TicketClassificationSentiment getSentiment() {
        return sentiment;
    }

    public void setSentiment(TicketClassificationSentiment sentiment) {
        this.sentiment = sentiment;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getClassifierVersion() {
        return classifierVersion;
    }

    public void setClassifierVersion(String classifierVersion) {
        this.classifierVersion = classifierVersion;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
