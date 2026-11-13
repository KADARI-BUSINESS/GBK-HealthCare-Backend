package com.gbkhealthcare.patient;

import java.time.Instant;

public record DoctorActivityResponse(
        Long id,
        DoctorActivity.ActionType actionType,
        DoctorActivity.EntityType entityType,
        Long entityId,
        Instant occurredAt,
        String displayMessage) {}
