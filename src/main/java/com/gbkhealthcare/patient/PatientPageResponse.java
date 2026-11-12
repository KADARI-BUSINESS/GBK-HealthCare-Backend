package com.gbkhealthcare.patient;

import org.springframework.data.domain.Page;

import java.util.List;

public record PatientPageResponse(
        List<Patient> content,
        long totalElements,
        int totalPages,
        int size,
        int number,
        boolean first,
        boolean last) {

    public static PatientPageResponse from(Page<Patient> page) {
        return new PatientPageResponse(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast());
    }
}
