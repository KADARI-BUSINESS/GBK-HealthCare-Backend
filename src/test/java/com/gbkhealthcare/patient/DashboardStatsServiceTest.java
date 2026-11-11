package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceTest {
    @Mock private PatientRepository patientRepository;
    @Mock private PatientVisitRepository visitRepository;
    @Mock private PatientFollowUpRepository followUpRepository;
    @Mock private DoctorRepository doctorRepository;

    @Test
    void returnsScopedAggregatesAndUsesSamePatientCountForRecords() {
        DashboardStatsService service = new DashboardStatsService(
                patientRepository, visitRepository, followUpRepository,
                doctorRepository, "Asia/Kolkata");
        when(patientRepository.countByDoctorAccount_Id(17L)).thenReturn(6L);
        when(visitRepository.countByPatient_DoctorAccount_IdAndVisitDate(eq(17L), any(LocalDate.class))).thenReturn(3L);
        when(doctorRepository.countByEnabledTrue()).thenReturn(2L);
        when(followUpRepository.countUpcomingPendingForDoctor(eq(17L), any(LocalDate.class))).thenReturn(4L);
        when(visitRepository.countWithClinicalDocumentationForDoctor(17L)).thenReturn(5L);

        DashboardStatsResponse response = service.getStats(17L);

        assertEquals(new DashboardStatsResponse(6, 3, 2, 4, 6, 5), response);
        verify(patientRepository).countByDoctorAccount_Id(17L);
    }

    @Test
    void emptyTablesProduceZeroCounts() {
        DashboardStatsService service = new DashboardStatsService(
                patientRepository, visitRepository, followUpRepository,
                doctorRepository, "Asia/Kolkata");

        assertEquals(new DashboardStatsResponse(0, 0, 0, 0, 0, 0), service.getStats(99L));
    }
}
