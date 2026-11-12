package com.gbkhealthcare.patient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardStatsServiceTest {
    @Mock private PatientVisitRepository visitRepository;

    @Test
    void returnsScopedAggregatesAndUsesSamePatientCountForRecords() {
        DashboardStatsService service = new DashboardStatsService(
                visitRepository, "Asia/Kolkata");
        when(visitRepository.findDashboardStats(17L, LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))))
                .thenReturn(List.<Object[]>of(new Object[] { 6L, 3L, 2L, 4L, 6L, 5L }));

        DashboardStatsResponse response = service.getStats(17L);

        assertEquals(new DashboardStatsResponse(6, 3, 2, 4, 6, 5), response);
        verify(visitRepository).findDashboardStats(17L, LocalDate.now(java.time.ZoneId.of("Asia/Kolkata")));
    }

    @Test
    void emptyTablesProduceZeroCounts() {
        DashboardStatsService service = new DashboardStatsService(
                visitRepository, "Asia/Kolkata");
        when(visitRepository.findDashboardStats(99L, LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"))))
                .thenReturn(List.<Object[]>of(new Object[] { 0L, 0L, 0L, 0L, 0L, 0L }));

        assertEquals(new DashboardStatsResponse(0, 0, 0, 0, 0, 0), service.getStats(99L));
    }
}
