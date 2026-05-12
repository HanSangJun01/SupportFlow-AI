package com.supportflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TicketStatusTransitionPolicyTest {

    private final TicketStatusTransitionPolicy policy = new TicketStatusTransitionPolicy();

    @Test
    void allowsConfiguredForwardAndReworkTransitions() {
        assertThat(policy.canTransition(TicketStatus.NEW, TicketStatus.TRIAGED)).isTrue();
        assertThat(policy.canTransition(TicketStatus.TRIAGED, TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(policy.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.ANSWERED)).isTrue();
        assertThat(policy.canTransition(TicketStatus.ANSWERED, TicketStatus.CLOSED)).isTrue();
        assertThat(policy.canTransition(TicketStatus.ANSWERED, TicketStatus.IN_PROGRESS)).isTrue();
    }

    @Test
    void rejectsClosedTicketReopen() {
        assertThat(policy.canTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS)).isFalse();
        assertThatThrownBy(() -> policy.validateTransition(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid ticket status transition");
    }

    @Test
    void rejectsSkippedTransitions() {
        assertThat(policy.canTransition(TicketStatus.NEW, TicketStatus.ANSWERED)).isFalse();
        assertThat(policy.canTransition(TicketStatus.TRIAGED, TicketStatus.ANSWERED)).isFalse();
        assertThat(policy.canTransition(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED)).isFalse();
    }
}
