package com.gbkhealthcare.patient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DashboardStatsService {
    private final PatientVisitRepository visitRepository;
    private final ZoneId applicationZone;

    public DashboardStatsService(
            PatientVisitRepository visitRepository,
            @Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.visitRepository = visitRepository;
        this.applicationZone = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(Long doctorId) {
        LocalDate today = LocalDate.now(applicationZone);
        java.util.List<Object[]> rows = visitRepository.findDashboardStats(doctorId, today);
        if (rows == null || rows.isEmpty() || rows.get(0).length < 6) {
            throw new IllegalStateException("Dashboard statistics query returned an incomplete result.");
        }
        Object[] counts = rows.get(0);
        return new DashboardStatsResponse(
                countValue(counts[0]), countValue(counts[1]), countValue(counts[2]),
                countValue(counts[3]), countValue(counts[4]), countValue(counts[5]));
    }

    private static long countValue(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }
}
