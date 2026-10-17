package com.gbkhealthcare.patient;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PatientReportDeliveryRepository
        extends JpaRepository<PatientReportDelivery, Long> {

    List<PatientReportDelivery> findByPatient_IdOrderByCreatedAtDesc(Long patientId);

    List<PatientReportDelivery> findByVisit_IdOrderByCreatedAtDesc(Long visitId);

    Optional<PatientReportDelivery> findByIdAndPatient_Id(Long deliveryId, Long patientId);
}