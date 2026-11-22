package com.gbkhealthcare.patient;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class PatientFollowUpLifecycleStateTest {
    @Test void consultationAloneAdvancesAndCompletesTheFollowUpLifecycle() {
        PatientFollowUp followUp = new PatientFollowUp();
        LocalDateTime started = LocalDateTime.of(2026, 11, 17, 10, 0);
        LocalDateTime signed = started.plusMinutes(20);
        assertEquals("SCHEDULED", followUp.getStatus());
        followUp.markInProgress(started);
        assertEquals("IN_PROGRESS", followUp.getStatus()); assertEquals(started, followUp.getStartedAt());
        followUp.markCompleted(signed);
        assertEquals("COMPLETED", followUp.getStatus()); assertEquals(signed, followUp.getCompletedAt());
        followUp.markCompleted(signed.plusMinutes(1));
        assertEquals(signed, followUp.getCompletedAt(), "Duplicate completion must not change its original timestamp.");
        assertThrows(IllegalStateException.class, () -> followUp.markCancelled("DOCTOR", 6L, "reason", signed));
    }

    @Test void cancellationAndMissedStatesAreExplicitAndCannotBeReversed() {
        PatientFollowUp cancelled = new PatientFollowUp();
        LocalDateTime at = LocalDateTime.of(2026, 11, 17, 11, 0);
        cancelled.markCancelled("STAFF", 12L, "Patient requested cancellation", at);
        assertEquals("CANCELLED", cancelled.getStatus()); assertEquals("STAFF", cancelled.getCancelledByActorType());
        assertEquals(12L, cancelled.getCancelledByActorId()); assertEquals("Patient requested cancellation", cancelled.getCancellationReason());
        assertThrows(IllegalStateException.class, () -> cancelled.markInProgress(at.plusMinutes(1)));

        PatientFollowUp missed = new PatientFollowUp();
        missed.markMissed("STAFF", 13L, "Did not arrive", at);
        assertEquals("MISSED", missed.getStatus()); assertEquals(13L, missed.getMissedByActorId());
        assertEquals("Did not arrive", missed.getMissedReason());
        assertThrows(IllegalStateException.class, () -> missed.markCompleted(at.plusMinutes(1)));
    }
}
