package com.gbkhealthcare.auth;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorApprovalHistoryRepository extends JpaRepository<DoctorApprovalHistory, Long> {
    Page<DoctorApprovalHistory> findAllByOrderByDecisionAtDescIdDesc(Pageable pageable);
}
