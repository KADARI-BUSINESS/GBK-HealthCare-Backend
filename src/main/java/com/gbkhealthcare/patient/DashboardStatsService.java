package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.DoctorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DashboardStatsService {
    private final PatientRepository patientRepository;
    private final PatientVisitRepository visitRepository;
    private final PatientFollowUpRepository followUpRepository;
    private final DoctorRepository doctorRepository;
    private final ZoneId applicationZone;

    public DashboardStatsService(
            PatientRepository patientRepository,
            PatientVisitRepository visitRepository,
            PatientFollowUpRepository followUpRepository,
            DoctorRepository doctorRepository,
            @Value("${app.timezone:Asia/Kolkata}") String timezone) {
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.followUpRepository = followUpRepository;
        this.doctorRepository = doctorRepository;
        this.applicationZone = ZoneId.of(timezone);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats(Long doctorId) {
        LocalDate today = LocalDate.now(applicationZone);
        long patients = patientRepository.countByDoctorAccount_Id(doctorId);
        return new DashboardStatsResponse(
                patients,
                visitRepository.countByPatient_DoctorAccount_IdAndVisitDate(doctorId, today),
                doctorRepository.countByEnabledTrue(),
                followUpRepository.countUpcomingPendingForDoctor(doctorId, today),
                patients,
                visitRepository.countWithClinicalDocumentationForDoctor(doctorId));
    }
}
