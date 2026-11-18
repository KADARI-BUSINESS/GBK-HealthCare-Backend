package com.gbkhealthcare.auth;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AdminRefreshSessionRepository extends JpaRepository<AdminRefreshSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from AdminRefreshSession session where session.tokenHash = :hash")
    Optional<AdminRefreshSession> findByTokenHashForUpdate(@Param("hash") String hash);
    List<AdminRefreshSession> findAllByFamilyId(String familyId);
}
