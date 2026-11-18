package com.gbkhealthcare.patient;
import org.springframework.data.repository.Repository;
import java.util.List;
import java.util.Optional;
public interface ReferralOrderRepository extends Repository<ReferralOrder,Long>{ ReferralOrder save(ReferralOrder order); Optional<ReferralOrder> findById(Long id); List<ReferralOrder> findAllByPatientVisit_IdOrderByCreatedAtDesc(Long visitId); }
