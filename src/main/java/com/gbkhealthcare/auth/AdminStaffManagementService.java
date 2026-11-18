package com.gbkhealthcare.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminStaffManagementService {
    private final StaffAccountRepository accounts;
    private final StaffRoleAssignmentRepository assignments;
    private final StaffAccountAuditEventRepository audit;
    private final StaffRefreshSessionService sessions;
    private final PasswordEncoder passwords;

    public AdminStaffManagementService(StaffAccountRepository accounts, StaffRoleAssignmentRepository assignments,
            StaffAccountAuditEventRepository audit, StaffRefreshSessionService sessions, PasswordEncoder passwords) {
        this.accounts = accounts; this.assignments = assignments; this.audit = audit; this.sessions = sessions; this.passwords = passwords;
    }

    @Transactional(readOnly = true)
    public List<StaffAccountResponse> list() {
        return accounts.findAll().stream().sorted(Comparator.comparing(StaffAccount::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(account -> response(account, activeRoles(account.getId()))).toList();
    }

    @Transactional
    public StaffAccountResponse create(StaffCreateRequest request, Long adminId, String adminEmail) {
        requireAdmin(adminId, adminEmail);
        if (request.temporaryPassword().getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Temporary password exceeds the BCrypt input limit.");
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (accounts.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A staff account with this email already exists.");
        StaffAccount account = new StaffAccount(); account.setEmail(email); account.setFullName(request.fullName().trim());
        account.setMobileNumber(request.mobileNumber() == null || request.mobileNumber().isBlank() ? null : request.mobileNumber().trim());
        account.setPasswordHash(passwords.encode(request.temporaryPassword())); account.setEnabled(true);
        try { accounts.saveAndFlush(account); }
        catch (DataIntegrityViolationException exception) { throw new ResponseStatusException(HttpStatus.CONFLICT, "A staff account with this email already exists."); }
        for (StaffRole role : request.roles()) {
            assignments.save(new StaffRoleAssignment(account, role, adminId));
            record(adminId, adminEmail, account.getId(), "ROLE_ASSIGNED", role.name());
        }
        record(adminId, adminEmail, account.getId(), "ACCOUNT_CREATED", "Staff account created with assigned roles");
        return response(account, request.roles());
    }

    @Transactional
    public StaffAccountResponse setRoles(Long staffId, StaffRolesRequest request, Long adminId, String adminEmail) {
        requireAdmin(adminId, adminEmail);
        StaffAccount account = requireAccountForUpdate(staffId);
        Set<StaffRole> desired = request.roles();
        try {
            for (StaffRole role : StaffRole.values()) {
                StaffRoleAssignment row = assignments.findByStaff_IdAndRole(staffId, role).orElse(null);
                boolean currently = row != null && row.isActive(), wanted = desired.contains(role);
                if (wanted && !currently) {
                    if (row == null) assignments.save(new StaffRoleAssignment(account, role, adminId));
                    else { row.assign(adminId); assignments.save(row); }
                    record(adminId, adminEmail, staffId, "ROLE_ASSIGNED", role.name());
                } else if (currently && !wanted) {
                    row.revoke(); assignments.save(row);
                    record(adminId, adminEmail, staffId, "ROLE_REVOKED", role.name());
                }
            }
            // Surface deferred unique-constraint failures inside this method so they can be
            // translated to a safe conflict response rather than a raw persistence exception.
            assignments.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Staff roles changed concurrently. Reload the account and retry.");
        }
        sessions.revokeAll(staffId);
        return response(account, activeRoles(staffId));
    }

    @Transactional
    public StaffAccountResponse setEnabled(Long staffId, StaffStatusRequest request, Long adminId, String adminEmail) {
        requireAdmin(adminId, adminEmail);
        StaffAccount account = requireAccount(staffId);
        boolean enabled = request.enabled();
        if (account.isEnabled() != enabled) {
            account.setEnabled(enabled); accounts.save(account);
            record(adminId, adminEmail, staffId, enabled ? "ACCOUNT_ENABLED" : "ACCOUNT_DISABLED", "enabled=" + enabled);
            if (!enabled) sessions.revokeAll(staffId);
        }
        return response(account, activeRoles(staffId));
    }

    private StaffAccount requireAccount(Long id) {
        if (id == null || id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID must be positive.");
        return accounts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff account not found."));
    }
    private StaffAccount requireAccountForUpdate(Long id) {
        if (id == null || id < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Staff ID must be positive.");
        return accounts.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff account not found."));
    }
    private Set<StaffRole> activeRoles(Long id) {
        return assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(id).stream().map(StaffRoleAssignment::getRole).collect(Collectors.toUnmodifiableSet());
    }
    private static StaffAccountResponse response(StaffAccount account, Set<StaffRole> roles) { return StaffAccountResponse.from(account, roles); }
    private void record(Long adminId, String email, Long staffId, String action, String detail) {
        audit.save(new StaffAccountAuditEvent(adminId, email.trim(), staffId, action, detail));
    }
    private static void requireAdmin(Long id, String email) {
        if (id == null || id < 1 || email == null || email.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin identity is required.");
    }
}
