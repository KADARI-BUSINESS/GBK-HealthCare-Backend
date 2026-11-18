package com.gbkhealthcare.auth;

import java.time.Instant;
import java.util.Set;

public record StaffAccountResponse(Long id, String fullName, String email, String mobileNumber,
        Set<StaffRole> roles, boolean enabled, Instant createdAt) {
    static StaffAccountResponse from(StaffAccount account, Set<StaffRole> roles) {
        return new StaffAccountResponse(account.getId(), account.getFullName(), account.getEmail(),
                account.getMobileNumber(), roles, account.isEnabled(), account.getCreatedAt());
    }
}
