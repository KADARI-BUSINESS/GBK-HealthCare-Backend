package com.gbkhealthcare.patient;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
public interface InvestigationOrderRepository extends Repository<InvestigationOrder,Long>{ InvestigationOrder save(InvestigationOrder order); Optional<InvestigationOrder> findById(Long id); List<InvestigationOrder> findAllByPatientVisit_IdOrderByOrderedAtDesc(Long visitId); }
