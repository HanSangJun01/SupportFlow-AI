package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TicketClassificationServiceTest {

    @Test
    void ticketClassificationAttemptsAreNullSafe() {
        Ticket ticket = new Ticket();

        ticket.setClassificationAttempts(null);

        assertThat(ticket.getClassificationAttempts()).isEmpty();

        TicketClassificationAttempt attempt = TicketClassificationAttempt.success(
                TicketClassificationTrigger.AUTO_ON_CREATE,
                null,
                Instant.parse("2026-06-11T08:00:00Z"),
                Instant.parse("2026-06-11T08:00:01Z"),
                "billing",
                TicketClassificationUrgency.NORMAL,
                TicketClassificationSentiment.NEUTRAL,
                TicketPriority.MEDIUM,
                0.74,
                "rules-v1"
        );
        ticket.setClassificationAttempts(List.of(attempt));

        assertThat(ticket.getClassificationAttempts()).singleElement()
                .satisfies(savedAttempt -> {
                    assertThat(savedAttempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.SUCCESS);
                    assertThat(savedAttempt.getTrigger()).isEqualTo(TicketClassificationTrigger.AUTO_ON_CREATE);
                    assertThat(savedAttempt.getClassifierVersion()).isEqualTo("rules-v1");
                });
    }

    @Test
    void ticketHistoryEntryCanLinkClassificationAttemptId() {
        TicketHistoryEntry entry = new TicketHistoryEntry(
                TicketHistoryEventType.AI_CLASSIFICATION_APPLIED,
                null,
                "attempt-1",
                Instant.parse("2026-06-11T08:00:01Z"),
                List.of(new TicketFieldChange("priority", "LOW", "HIGH"))
        );

        assertThat(entry.getClassificationAttemptId()).isEqualTo("attempt-1");
        assertThat(entry.getEventType()).isEqualTo(TicketHistoryEventType.AI_CLASSIFICATION_APPLIED);
        assertThat(entry.getChanges()).singleElement()
                .satisfies(change -> assertThat(change.getField()).isEqualTo("priority"));
    }

    @Test
    void failedClassificationAttemptStoresErrorMetadata() {
        TicketClassificationAttempt attempt = TicketClassificationAttempt.failed(
                TicketClassificationTrigger.MANUAL_REANALYSIS,
                "actor-1",
                Instant.parse("2026-06-11T08:00:00Z"),
                Instant.parse("2026-06-11T08:00:01Z"),
                "AI_CLASSIFICATION_UNAVAILABLE",
                "AI service unavailable"
        );

        assertThat(attempt.getStatus()).isEqualTo(TicketClassificationAttemptStatus.FAILED);
        assertThat(attempt.getTrigger()).isEqualTo(TicketClassificationTrigger.MANUAL_REANALYSIS);
        assertThat(attempt.getActorUserId()).isEqualTo("actor-1");
        assertThat(attempt.getErrorCode()).isEqualTo("AI_CLASSIFICATION_UNAVAILABLE");
        assertThat(attempt.getErrorMessage()).isEqualTo("AI service unavailable");
    }
}
