
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
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE v.id = :visitId
            ORDER BY f.followUpDate ASC
            """)
    List<PatientFollowUp> findByVisitIdWithDetails(
            @Param("visitId") Long visitId);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE p.id = :patientId
            ORDER BY f.followUpDate ASC
            """)
    List<PatientFollowUp> findByPatientIdWithDetails(
            @Param("patientId") Long patientId);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE f.status = :status
              AND f.followUpDate >= :date
            ORDER BY f.followUpDate ASC
            """)
    List<PatientFollowUp> findUpcomingWithDetails(
            @Param("status") String status,
            @Param("date") LocalDate date);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            WHERE f.id = :followUpId
            """)
    Optional<PatientFollowUp> findByIdWithDetails(
            @Param("followUpId") Long followUpId);
}
