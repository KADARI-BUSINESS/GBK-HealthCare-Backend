
package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientVisitRepository
        extends JpaRepository<PatientVisit, Long> {

    long countByPatient_DoctorAccount_IdAndVisitDate(Long doctorAccountId, LocalDate visitDate);

    @Query("""
            SELECT COUNT(v)
            FROM PatientVisit v
            WHERE v.patient.doctorAccount.id = :doctorAccountId
              AND (
                (v.chiefComplaints IS NOT NULL AND TRIM(v.chiefComplaints) <> '') OR
                (v.presentIllness IS NOT NULL AND TRIM(v.presentIllness) <> '') OR
                (v.generalExamination IS NOT NULL AND TRIM(v.generalExamination) <> '') OR
                (v.heartExamination IS NOT NULL AND TRIM(v.heartExamination) <> '') OR
                (v.lungsExamination IS NOT NULL AND TRIM(v.lungsExamination) <> '') OR
                (v.abdomenExamination IS NOT NULL AND TRIM(v.abdomenExamination) <> '') OR
                (v.rectalExamination IS NOT NULL AND TRIM(v.rectalExamination) <> '') OR
                v.temperature IS NOT NULL OR v.pulseRate IS NOT NULL OR
                v.spo2 IS NOT NULL OR v.bpSystolic IS NOT NULL OR v.bpDiastolic IS NOT NULL OR
                (v.diagnosis IS NOT NULL AND TRIM(v.diagnosis) <> '') OR
                (v.treatmentPlan IS NOT NULL AND TRIM(v.treatmentPlan) <> '') OR
                (v.followUpNotes IS NOT NULL AND TRIM(v.followUpNotes) <> '')
              )
            """)
    long countWithClinicalDocumentationForDoctor(@Param("doctorAccountId") Long doctorAccountId);

    List<PatientVisit> findByPatient_IdAndPatient_DoctorAccount_IdOrderByVisitDateDesc(
            Long patientId,
            Long doctorAccountId);

    Optional<PatientVisit> findByIdAndPatient_DoctorAccount_Id(
            Long visitId,
            Long doctorAccountId);

    @Query("""
            SELECT COALESCE(MAX(v.visitNumber), 0)
            FROM PatientVisit v
            WHERE v.patient.id = :patientId
            """)
    Integer findMaxVisitNumberByPatientId(
            @Param("patientId") Long patientId);
}
