package com.gbkhealthcare.patient;

import java.time.LocalDateTime;

/** Deliberately excludes clinical text and other patient data. */
public record NursingAuditEventResponse(Long eventId, Long queueEntryId, Long intakeRecordId,
        Long actorStaffId, String actorRole, String actionType, Integer revision, LocalDateTime occurredAt) {
    public static NursingAuditEventResponse from(NursingIntakeAuditEvent e) {
        return new NursingAuditEventResponse(e.getId(), e.getQueueEntryId(), e.getIntakeRecordId(), e.getActorStaffId(),
                e.getActorRole(), e.getActionType(), e.getRevision(), e.getOccurredAt());
    }
}
