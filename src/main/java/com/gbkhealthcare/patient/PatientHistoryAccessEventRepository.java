package com.gbkhealthcare.patient;

import org.springframework.data.repository.Repository;
import java.util.List;

/** Historical-access audit is append-only. */
public interface PatientHistoryAccessEventRepository extends Repository<PatientHistoryAccessEvent, Long> {
    PatientHistoryAccessEvent save(PatientHistoryAccessEvent event);
    List<PatientHistoryAccessEvent> findAllByEncounterIdOrderByAccessedAtAscIdAsc(Long encounterId);
}
