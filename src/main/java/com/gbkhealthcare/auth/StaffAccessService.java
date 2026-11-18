package com.gbkhealthcare.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
public class StaffAccessService {
    private final StaffAccountRepository accounts;
    private final StaffRoleAssignmentRepository roles;
    public StaffAccessService(StaffAccountRepository accounts, StaffRoleAssignmentRepository roles) {
        this.accounts = accounts; this.roles = roles;
    }

    @Transactional(readOnly = true)
    public StaffAccount requireActiveStaff(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) throw inactiveStaff();
        StaffAccount account = accounts.findByEmailIgnoreCase(jwt.getSubject()).filter(StaffAccount::isEnabled)
                .orElseThrow(StaffAccessService::inactiveStaff);
        if (roles.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(account.getId()).isEmpty()) throw inactiveStaff();
        return account;
    }

    @Transactional(readOnly = true)
    public StaffAccount requireRole(Jwt jwt, StaffRole requiredRole) {
        StaffAccount account = requireActiveStaff(jwt);
        boolean granted = roles.findByStaff_IdAndRole(account.getId(), requiredRole)
                .filter(StaffRoleAssignment::isActive).isPresent();
        if (!granted) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This staff role is not assigned to the account.");
        return account;
    }

    @Transactional(readOnly = true)
    public Set<StaffRole> currentRoles(Long staffId) {
        return roles.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(staffId).stream().map(StaffRoleAssignment::getRole).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    static ResponseStatusException inactiveStaff() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Staff account is inactive or has no active roles.");
    }
}
