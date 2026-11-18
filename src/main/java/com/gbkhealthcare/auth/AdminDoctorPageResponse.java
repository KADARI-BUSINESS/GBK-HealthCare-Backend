package com.gbkhealthcare.auth;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminDoctorPageResponse(
        List<AdminDoctorResponse> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last) {

    public static AdminDoctorPageResponse from(Page<AdminDoctorResponse> page) {
        return new AdminDoctorPageResponse(page.getContent(), page.getTotalElements(), page.getTotalPages(),
                page.getSize(), page.getNumber(), page.isFirst(), page.isLast());
    }
}
