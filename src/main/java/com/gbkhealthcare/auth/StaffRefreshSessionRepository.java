package com.gbkhealthcare.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StaffRefreshSessionRepository extends JpaRepository<StaffRefreshSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from StaffRefreshSession session where session.tokenHash = :hash")
    Optional<StaffRefreshSession> findByTokenHashForUpdate(@Param("hash") String hash);
    List<StaffRefreshSession> findAllByFamilyId(String familyId);
    List<StaffRefreshSession> findAllByStaffIdAndRevokedAtIsNull(Long staffId);
}
