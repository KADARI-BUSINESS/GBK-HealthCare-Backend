package com.gbkhealthcare.patient;

import org.springframework.data.repository.Repository;
import java.util.List;

public interface PatientFollowUpAuditEventRepository extends Repository<PatientFollowUpAuditEvent, Long> {
    PatientFollowUpAuditEvent save(PatientFollowUpAuditEvent event);
    List<PatientFollowUpAuditEvent> findAllByFollowUpIdOrderByOccurredAtAscIdAsc(Long followUpId);
}
