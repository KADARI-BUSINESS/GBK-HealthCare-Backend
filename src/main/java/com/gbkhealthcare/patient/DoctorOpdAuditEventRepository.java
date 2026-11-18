package com.gbkhealthcare.patient;

import org.springframework.data.repository.Repository;
import java.util.List;

public interface DoctorOpdAuditEventRepository extends Repository<DoctorOpdAuditEvent, Long> {
    DoctorOpdAuditEvent save(DoctorOpdAuditEvent event);
    List<DoctorOpdAuditEvent> findAllByPatientVisitIdOrderByOccurredAtAscIdAsc(Long visitId);
}
