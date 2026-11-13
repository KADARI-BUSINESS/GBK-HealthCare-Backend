package com.gbkhealthcare.patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class PatientServicePaginationTest {

    private PatientRepository patientRepository;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        patientService = new PatientService(patientRepository, mock(PatientVisitRepository.class),
                mock(DoctorActivityService.class));
    }

    @Test
    void appliesPageSizeAndCombinedFiltersForRepositoryPagination() {
        Patient patient = new Patient();
        patient.setId(25L);
        when(patientRepository.findPageByDoctorAndFilters(
                eq(7L), eq("Ravi"), eq("Male"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(patient),
                        PageRequest.of(1, 20), 45));

        PatientPageResponse result = patientService.findPageByDoctorAccountId(
                7L, 1, 20, " Ravi ", "Male");

        assertEquals(List.of(patient), result.content());
        assertEquals(45, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(20, result.size());
        assertEquals(1, result.number());
        assertFalse(result.first());
        assertFalse(result.last());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(patientRepository).findPageByDoctorAndFilters(
                eq(7L), eq("Ravi"), eq("Male"), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(20, pageable.getValue().getPageSize());
        assertEquals(Sort.unsorted(), pageable.getValue().getSort());
    }

    @Test
    void firstPageReportsCorrectPageMetadata() {
        when(patientRepository.findPageByDoctorAndFilters(
                eq(7L), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new Patient()), PageRequest.of(0, 10), 25));

        PatientPageResponse result = patientService.findPageByDoctorAccountId(
                7L, 0, 10, null, null);

        assertEquals(25, result.totalElements());
        assertEquals(3, result.totalPages());
        assertEquals(0, result.number());
        assertTrue(result.first());
        assertFalse(result.last());
    }

    @Test
    void convertsBlankSearchAndGenderToNoFilters() {
        when(patientRepository.findPageByDoctorAndFilters(
                eq(7L), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        PatientPageResponse result = patientService.findPageByDoctorAccountId(
                7L, 0, 10, "  ", "");

        assertEquals(0, result.totalElements());
        assertEquals(0, result.number());
        assertTrue(result.first());
        assertTrue(result.last());
    }
}
