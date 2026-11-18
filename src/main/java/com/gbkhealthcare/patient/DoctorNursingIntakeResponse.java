package com.gbkhealthcare.patient;

import java.time.LocalDateTime;

public record DoctorNursingIntakeResponse(NursingIntakeResponse intake, Long nurseStaffId,
        String nurseName, LocalDateTime handoffAt) {}
