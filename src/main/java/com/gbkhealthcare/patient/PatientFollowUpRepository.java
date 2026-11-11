
package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PatientFollowUpRepository
        extends JpaRepository<PatientFollowUp, Long> {

    @Query("""
            SELECT COUNT(f)
            FROM PatientFollowUp f
            WHERE f.status = 'SCHEDULED'
              AND f.followUpDate >= :today
              AND f.visit.patient.doctorAccount.id = :doctorAccountId
            """)
    long countUpcomingPendingForDoctor(
            @Param("doctorAccountId") Long doctorAccountId,
            @Param("today") LocalDate today);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE v.id = :visitId
              AND p.doctorAccount.id = :doctorAccountId
            ORDER BY f.followUpDate ASC
            """)
    List<PatientFollowUp> findByVisitIdAndDoctorAccountIdWithDetails(
            @Param("visitId") Long visitId,
            @Param("doctorAccountId") Long doctorAccountId);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE p.id = :patientId
              AND p.doctorAccount.id = :doctorAccountId
            ORDER BY f.followUpDate ASC
            """)
    List<PatientFollowUp> findByPatientIdAndDoctorAccountIdWithDetails(
            @Param("patientId") Long patientId,
            @Param("doctorAccountId") Long doctorAccountId);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE f.status = :status
              AND f.followUpDate >= :date
              AND p.doctorAccount.id = :doctorAccountId
            ORDER BY f.followUpDate ASC
            """)
    List<PatientFollowUp> findUpcomingWithDetailsForDoctor(
            @Param("status") String status,
            @Param("date") LocalDate date,
            @Param("doctorAccountId") Long doctorAccountId);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE f.id = :followUpId
              AND p.doctorAccount.id = :doctorAccountId
            """)
    Optional<PatientFollowUp> findByIdAndDoctorAccountIdWithDetails(
            @Param("followUpId") Long followUpId,
            @Param("doctorAccountId") Long doctorAccountId);
}
