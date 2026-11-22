package com.gbkhealthcare.patient;

import org.springframework.data.repository.Repository;
import java.util.List;

public interface ConsultationAddendumRepository extends Repository<ConsultationAddendum, Long> {
    ConsultationAddendum save(ConsultationAddendum addendum);
    List<ConsultationAddendum> findAllByPatientVisit_IdOrderByCreatedAtAscIdAsc(Long visitId);
}
