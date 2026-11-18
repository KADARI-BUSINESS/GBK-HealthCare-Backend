package com.gbkhealthcare.auth;

import java.util.List;

public record DoctorApprovalHistoryPageResponse(
        List<DoctorApprovalHistoryResponse> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext) {
}
