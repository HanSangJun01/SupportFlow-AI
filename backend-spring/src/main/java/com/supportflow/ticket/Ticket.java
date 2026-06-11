package com.supportflow.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tickets")
public class Ticket {

    @Id
    private String id;

    @Indexed
    private String tenantId;

    private String subject;
    private String customerName;
    private String customerEmail;
    private String customerMessage;
    private TicketStatus status;
    private String category;
    private TicketPriority priority;
    private String assigneeId;
    private Instant createdAt;
    private Instant updatedAt;
    private List<TicketHistoryEntry> history = new ArrayList<>();
    private List<TicketClassificationAttempt> classificationAttempts = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerMessage() {
        return customerMessage;
    }

    public void setCustomerMessage(String customerMessage) {
        this.customerMessage = customerMessage;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public void setPriority(TicketPriority priority) {
        this.priority = priority;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TicketHistoryEntry> getHistory() {
        if (history == null) {
            history = new ArrayList<>();
        }
        return history;
    }

    public void setHistory(List<TicketHistoryEntry> history) {
        this.history = history == null ? new ArrayList<>() : new ArrayList<>(history);
    }

    public List<TicketClassificationAttempt> getClassificationAttempts() {
        if (classificationAttempts == null) {
            classificationAttempts = new ArrayList<>();
        }
        return classificationAttempts;
    }

    public void setClassificationAttempts(List<TicketClassificationAttempt> classificationAttempts) {
        this.classificationAttempts = classificationAttempts == null
                ? new ArrayList<>()
                : new ArrayList<>(classificationAttempts);
    }
}
