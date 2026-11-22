package com.gbkhealthcare.patient;
import org.springframework.data.repository.Repository;
import java.util.List;
public interface ClinicalOrderAuditEventRepository extends Repository<ClinicalOrderAuditEvent,Long>{ ClinicalOrderAuditEvent save(ClinicalOrderAuditEvent event); List<ClinicalOrderAuditEvent> findAllByPatientVisitIdOrderByOccurredAtAscIdAsc(Long patientVisitId); }
