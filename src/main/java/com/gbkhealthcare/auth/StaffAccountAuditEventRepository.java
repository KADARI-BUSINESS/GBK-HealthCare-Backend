package com.gbkhealthcare.auth;

import org.springframework.data.repository.Repository;
import java.util.List;

/** Narrow repository API: audit events can be appended and queried, but not updated or deleted. */
public interface StaffAccountAuditEventRepository extends Repository<StaffAccountAuditEvent, Long> {
    StaffAccountAuditEvent save(StaffAccountAuditEvent event);

    List<StaffAccountAuditEvent> findByTargetStaffIdOrderByOccurredAtDescIdDesc(Long targetStaffId);

    List<StaffAccountAuditEvent> findByActorAdminIdOrderByOccurredAtDescIdDesc(Long actorAdminId);
}
