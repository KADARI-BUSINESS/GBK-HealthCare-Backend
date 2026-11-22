package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

public interface NursingQueueEntryRepository extends JpaRepository<NursingQueueEntry, Long> {
    Optional<NursingQueueEntry> findByVisit_Id(Long visitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM NursingQueueEntry q WHERE q.id = :id")
    Optional<NursingQueueEntry> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT q FROM NursingQueueEntry q
            JOIN FETCH q.visit v
            JOIN FETCH v.patient p
            LEFT JOIN FETCH v.assignedDoctor d
            WHERE q.status IN ('WAITING', 'CLAIMED')
            ORDER BY v.emergencyPriority DESC, q.createdAt ASC, q.id ASC
            """)
    List<NursingQueueEntry> findActiveQueue();

    @Query("""
            SELECT COUNT(q) FROM NursingQueueEntry q
            WHERE q.status = 'WAITING'
              AND q.createdAt >= :start AND q.createdAt < :end
            """)
    long countTodayWaiting(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(q) FROM NursingQueueEntry q
            WHERE q.status = 'WAITING' AND q.visit.emergencyPriority = true
              AND q.createdAt >= :start AND q.createdAt < :end
            """)
    long countTodayEmergencyWaiting(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
