package com.gbkhealthcare.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface DoctorRefreshSessionRepository extends JpaRepository<DoctorRefreshSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from DoctorRefreshSession session where session.tokenHash = :hash")
    Optional<DoctorRefreshSession> findByTokenHashForUpdate(@Param("hash") String hash);

    List<DoctorRefreshSession> findAllByFamilyId(String familyId);
}
