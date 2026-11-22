package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

/** Append-only repository surface; no update or delete operations are exposed. */
public interface NursingIntakeRecordRepository extends Repository<NursingIntakeRecord, Long> {
    NursingIntakeRecord save(NursingIntakeRecord record);
    List<NursingIntakeRecord> findAllByQueueEntry_IdOrderByRevisionAsc(Long queueId);
    Optional<NursingIntakeRecord> findTopByQueueEntry_IdOrderByRevisionDesc(Long queueId);
    Optional<NursingIntakeRecord> findTopByQueueEntry_IdAndRecordStateOrderByRevisionDesc(Long queueId, String recordState);
    @Query("select coalesce(max(r.revision), 0) from NursingIntakeRecord r where r.queueEntry.id = :queueId")
    int findMaxRevision(@Param("queueId") Long queueId);
}
