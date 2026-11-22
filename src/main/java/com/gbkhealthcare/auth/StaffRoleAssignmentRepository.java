package com.gbkhealthcare.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface StaffRoleAssignmentRepository extends JpaRepository<StaffRoleAssignment, Long> {
    List<StaffRoleAssignment> findAllByStaff_IdOrderByRoleAsc(Long staffId);
    List<StaffRoleAssignment> findAllByStaff_IdAndActiveTrueOrderByRoleAsc(Long staffId);
    Optional<StaffRoleAssignment> findByStaff_IdAndRole(Long staffId, StaffRole role);
}
