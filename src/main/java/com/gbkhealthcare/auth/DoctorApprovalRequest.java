package com.gbkhealthcare.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DoctorApprovalRequest(
        @NotBlank(message = "approvalStatus is required")
        @Pattern(regexp = "APPROVED|REJECTED", message = "approvalStatus must be APPROVED or REJECTED")
        String approvalStatus,

        @Size(max = 500, message = "Rejection reason is too long")
        String rejectionReason) {

    public DoctorApprovalRequest(String approvalStatus) {
        this(approvalStatus, null);
    }
}
