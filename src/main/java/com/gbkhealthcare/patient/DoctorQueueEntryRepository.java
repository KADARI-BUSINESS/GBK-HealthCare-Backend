package com.gbkhealthcare.patient;

import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface DoctorQueueEntryRepository extends Repository<DoctorQueueEntry, Long> {
    DoctorQueueEntry save(DoctorQueueEntry entry);
    Optional<DoctorQueueEntry> findByNursingQueueEntry_Id(Long queueId);
    Optional<DoctorQueueEntry> findById(Long id);

    @Query("select q from DoctorQueueEntry q join fetch q.patientVisit v join fetch v.patient p where q.id = :id and q.assignedDoctor.id = :doctorId")
    Optional<DoctorQueueEntry> findAssigned(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select q from DoctorQueueEntry q where q.id = :id and q.assignedDoctor.id = :doctorId")
    Optional<DoctorQueueEntry> findAssignedForUpdate(@Param("id") Long id, @Param("doctorId") Long doctorId);

    @Query("""
            select q from DoctorQueueEntry q
            join fetch q.patientVisit v join fetch v.patient p
            where q.assignedDoctor.id = :doctorId
              and q.status in ('WAITING', 'CLAIMED', 'IN_CONSULTATION', 'ON_HOLD', 'CONSULTATION_SIGNED')
            order by v.emergencyPriority desc, q.createdAt asc, q.id asc
            """)
    List<DoctorQueueEntry> findActiveForDoctor(@Param("doctorId") Long doctorId);
}
