
package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;

public interface PatientFollowUpRepository
        extends JpaRepository<PatientFollowUp, Long> {

    @Query("""
            SELECT COUNT(f)
            FROM PatientFollowUp f
            JOIN f.visit v JOIN v.patient p
            LEFT JOIN p.doctorAccount owner LEFT JOIN v.assignedDoctor originalDoctor
            LEFT JOIN f.linkedVisit linked LEFT JOIN linked.assignedDoctor linkedDoctor
            WHERE f.status = 'SCHEDULED'
              AND f.followUpDate >= :today
              AND (owner.id = :doctorAccountId OR originalDoctor.id = :doctorAccountId OR linkedDoctor.id = :doctorAccountId)
            """)
    long countUpcomingPendingForDoctor(
            @Param("doctorAccountId") Long doctorAccountId,
            @Param("today") LocalDate today);

    @Query("""
            SELECT f
            FROM PatientFollowUp f
            JOIN FETCH f.visit v
            JOIN FETCH v.patient p
            LEFT JOIN p.doctorAccount owner LEFT JOIN v.assignedDoctor originalDoctor
            LEFT JOIN f.linkedVisit linked LEFT JOIN linked.assignedDoctor linkedDoctor
            WHERE v.id = :visitId
              AND (owner.id = :doctorAccountId OR originalDoctor.id = :doctorAccountId OR linkedDoctor.id = :doctorAccountId)
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
            LEFT JOIN p.doctorAccount owner LEFT JOIN v.assignedDoctor originalDoctor
            LEFT JOIN f.linkedVisit linked LEFT JOIN linked.assignedDoctor linkedDoctor
            WHERE p.id = :patientId
              AND (owner.id = :doctorAccountId OR originalDoctor.id = :doctorAccountId OR linkedDoctor.id = :doctorAccountId)
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
            LEFT JOIN p.doctorAccount owner LEFT JOIN v.assignedDoctor originalDoctor
            LEFT JOIN f.linkedVisit linked LEFT JOIN linked.assignedDoctor linkedDoctor
            WHERE f.status = :status
              AND f.followUpDate >= :date
              AND (owner.id = :doctorAccountId OR originalDoctor.id = :doctorAccountId OR linkedDoctor.id = :doctorAccountId)
            ORDER BY f.followUpDate ASC
            """)
    List<PatientFollowUp> findUpcomingWithDetailsForDoctor(
            @Param("status") String status,
            @Param("date") LocalDate date,
            @Param("doctorAccountId") Long doctorAccountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM PatientFollowUp f JOIN FETCH f.visit v JOIN FETCH v.patient WHERE f.id = :followUpId")
    Optional<PatientFollowUp> findByIdForReceptionCheckIn(@Param("followUpId") Long followUpId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM PatientFollowUp f WHERE f.id = :followUpId")
    Optional<PatientFollowUp> findByIdForUpdate(@Param("followUpId") Long followUpId);

    @Query("SELECT f FROM PatientFollowUp f JOIN FETCH f.visit v JOIN FETCH v.patient p LEFT JOIN FETCH f.linkedVisit lv LEFT JOIN FETCH lv.assignedDoctor WHERE (:status IS NULL OR f.status = :status) ORDER BY f.followUpDate ASC, f.id ASC")
    List<PatientFollowUp> findAllForReception(@Param("status") String status);
    boolean existsById(Long followUpId);

    @Query("SELECT f FROM PatientFollowUp f JOIN FETCH f.visit v JOIN FETCH v.patient p LEFT JOIN p.doctorAccount owner LEFT JOIN v.assignedDoctor originalDoctor LEFT JOIN FETCH f.linkedVisit lv LEFT JOIN FETCH lv.assignedDoctor WHERE f.id = :followUpId AND (owner.id = :doctorId OR originalDoctor.id = :doctorId OR lv.assignedDoctor.id = :doctorId)")
    Optional<PatientFollowUp> findByIdAndDoctorAccountIdWithDetails(@Param("followUpId") Long followUpId, @Param("doctorId") Long doctorId);

    @Query("""
            SELECT f FROM PatientFollowUp f
            JOIN FETCH f.visit v
            WHERE v.patient.id = :patientId
              AND f.status = 'SCHEDULED'
              AND NOT EXISTS (SELECT pv.id FROM PatientVisit pv WHERE pv.sourceFollowUp = f)
            ORDER BY f.followUpDate ASC, f.id ASC
            """)
    List<PatientFollowUp> findUnlinkedScheduledForPatient(@Param("patientId") Long patientId);
    @Query("""
            SELECT f.id, v.id, p.id, p.fullName, f.followUpDate, v.visitDate,
                   f.followUpNotes, f.status, f.createdAt, f.completedAt, lv.id
            FROM PatientFollowUp f
            JOIN f.visit v
            JOIN v.patient p
            LEFT JOIN p.doctorAccount owner LEFT JOIN v.assignedDoctor originalDoctor
            LEFT JOIN f.linkedVisit lv
            WHERE owner.id = :doctorAccountId OR originalDoctor.id = :doctorAccountId OR lv.assignedDoctor.id = :doctorAccountId
            ORDER BY f.followUpDate DESC, f.id DESC
            """)
    List<Object[]> findAllWithDetailsForDoctor(
            @Param("doctorAccountId") Long doctorAccountId);
}

