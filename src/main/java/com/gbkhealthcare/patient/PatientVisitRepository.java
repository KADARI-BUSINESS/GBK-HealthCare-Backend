
package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientVisitRepository
        extends JpaRepository<PatientVisit, Long> {

    @Query(value = """
            SELECT
              (SELECT COUNT(*) FROM patients p WHERE p.doctor_account_id = :doctorId) AS total_patients,
              (SELECT COUNT(*) FROM patient_visits v JOIN patients p ON p.id = v.patient_id
               WHERE p.doctor_account_id = :doctorId AND v.visit_date = :today) AS todays_visits,
              (SELECT COUNT(*) FROM doctors d WHERE d.enabled = TRUE) AS available_doctors,
              (SELECT COUNT(*) FROM patient_follow_ups f
               JOIN patient_visits v ON v.id = f.visit_id
               JOIN patients p ON p.id = v.patient_id
               WHERE p.doctor_account_id = :doctorId
                 AND f.status = 'SCHEDULED' AND f.follow_up_date >= :today) AS pending_follow_ups,
              (SELECT COUNT(*) FROM patients p WHERE p.doctor_account_id = :doctorId) AS registered_records,
              (SELECT COUNT(*) FROM patient_visits v JOIN patients p ON p.id = v.patient_id
               WHERE p.doctor_account_id = :doctorId AND (
                 (v.chief_complaints IS NOT NULL AND TRIM(v.chief_complaints) <> '') OR
                 (v.present_illness IS NOT NULL AND TRIM(v.present_illness) <> '') OR
                 (v.general_examination IS NOT NULL AND TRIM(v.general_examination) <> '') OR
                 (v.heart_examination IS NOT NULL AND TRIM(v.heart_examination) <> '') OR
                 (v.lungs_examination IS NOT NULL AND TRIM(v.lungs_examination) <> '') OR
                 (v.abdomen_examination IS NOT NULL AND TRIM(v.abdomen_examination) <> '') OR
                 (v.rectal_examination IS NOT NULL AND TRIM(v.rectal_examination) <> '') OR
                 v.temperature IS NOT NULL OR v.pulse_rate IS NOT NULL OR
                 v.spo2 IS NOT NULL OR v.bp_systolic IS NOT NULL OR v.bp_diastolic IS NOT NULL OR
                 (v.diagnosis IS NOT NULL AND TRIM(v.diagnosis) <> '') OR
                 (v.treatment_plan IS NOT NULL AND TRIM(v.treatment_plan) <> '') OR
                 (v.follow_up_notes IS NOT NULL AND TRIM(v.follow_up_notes) <> '')
               )) AS clinical_notes
            """, nativeQuery = true)
    List<Object[]> findDashboardStats(
            @Param("doctorId") Long doctorId,
            @Param("today") LocalDate today);

    @Query("""
            SELECT v.id, p.id, p.fullName, v.visitDate, v.doctor, v.chiefComplaints
            FROM PatientVisit v
            JOIN v.patient p
            WHERE p.doctorAccount.id = :doctorAccountId
              AND v.visitDate = :visitDate
            ORDER BY v.createdAt DESC, v.id DESC
            """)
    List<Object[]> findVisitSummariesForDoctorOnDate(
            @Param("doctorAccountId") Long doctorAccountId,
            @Param("visitDate") LocalDate visitDate);
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

