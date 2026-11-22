package com.gbkhealthcare.auth;

import java.time.LocalDateTime;

public record DoctorApprovalHistoryResponse(
        Long id,
        Long doctorId,
        String doctorName,
        String doctorEmail,
        Doctor.ApprovalStatus previousApprovalStatus,
        Doctor.ApprovalStatus newApprovalStatus,
        Long adminId,
        String adminEmail,
        LocalDateTime decisionAt,
        String rejectionReason) {

    public static DoctorApprovalHistoryResponse from(DoctorApprovalHistory history) {
        Doctor doctor = history.getDoctor();
        return new DoctorApprovalHistoryResponse(history.getId(), doctor.getId(), doctor.getFullName(),
                doctor.getEmail(), history.getPreviousApprovalStatus(), history.getNewApprovalStatus(),
                history.getAdminId(), history.getAdminEmail(), history.getDecisionAt(), history.getRejectionReason());
    }
}
