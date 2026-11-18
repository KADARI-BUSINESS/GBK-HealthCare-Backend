package com.gbkhealthcare.patient;

import org.springframework.data.repository.Repository;
import java.util.List;

/** Append-only audit repository; audit rows cannot be edited or deleted through this API. */
public interface NursingIntakeAuditEventRepository extends Repository<NursingIntakeAuditEvent, Long> {
    NursingIntakeAuditEvent save(NursingIntakeAuditEvent event);
    List<NursingIntakeAuditEvent> findAllByQueueEntryIdOrderByOccurredAtAscIdAsc(Long queueId);
}
