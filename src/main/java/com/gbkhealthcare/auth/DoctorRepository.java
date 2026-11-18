package com.gbkhealthcare.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import java.util.Optional;

public interface DoctorRepository
        extends JpaRepository<Doctor, Long>, JpaSpecificationExecutor<Doctor> {

    @Query("SELECT new com.gbkhealthcare.auth.DoctorDirectoryEntry(d.id, d.fullName, d.email, d.enabled) FROM Doctor d ORDER BY d.fullName")
    List<DoctorDirectoryEntry> findAllDirectoryEntries();
    Optional<Doctor> findByEmailIgnoreCase(String email);

    List<Doctor> findAllByApprovalStatusOrderByCreatedAtAsc(Doctor.ApprovalStatus approvalStatus);

    List<Doctor> findAllByApprovalStatusAndEnabledTrueOrderByFullNameAsc(Doctor.ApprovalStatus approvalStatus);

    List<Doctor> findAllByOrderByCreatedAtAsc();

    boolean existsByEmailIgnoreCase(String email);

    long countByEnabledTrue();
}

