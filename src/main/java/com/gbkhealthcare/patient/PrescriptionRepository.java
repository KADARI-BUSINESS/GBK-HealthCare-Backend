package com.gbkhealthcare.patient;

import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;

/** Prescription history can be appended and queried; no delete operation is exposed. */
public interface PrescriptionRepository extends Repository<Prescription, Long> {
    Prescription save(Prescription prescription);
    Optional<Prescription> findById(Long id);
    List<Prescription> findAllByPatientVisit_IdOrderByRevisionDesc(Long visitId);
    int countByPatientVisit_Id(Long visitId);
}
