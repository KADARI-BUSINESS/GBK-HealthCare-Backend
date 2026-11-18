package com.gbkhealthcare.auth;

import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminStaffManagementServiceTest {
    private final StaffAccountRepository accounts = mock(StaffAccountRepository.class);
    private final StaffRoleAssignmentRepository assignments = mock(StaffRoleAssignmentRepository.class);
    private final StaffAccountAuditEventRepository audit = mock(StaffAccountAuditEventRepository.class);
    private final StaffRefreshSessionService sessions = mock(StaffRefreshSessionService.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final AdminStaffManagementService service = new AdminStaffManagementService(accounts, assignments, audit, sessions, passwords);

    @Test void createHashesIndividualPasswordAndAuditsAccountAndEveryRole() {
        when(accounts.existsByEmailIgnoreCase("staff@example.test")).thenReturn(false);
        when(passwords.encode("unique-temporary-secret")).thenReturn("$2a$12$hash");
        when(accounts.saveAndFlush(any(StaffAccount.class))).thenAnswer(invocation -> {
            StaffAccount saved = invocation.getArgument(0); saved.setId(31L); return saved;
        });
        List<StaffRoleAssignment> savedRoles = new ArrayList<>();
        when(assignments.save(any(StaffRoleAssignment.class))).thenAnswer(invocation -> {
            StaffRoleAssignment row = invocation.getArgument(0); savedRoles.add(row); return row;
        });
        StaffCreateRequest request = new StaffCreateRequest(" Test Staff ", " Staff@Example.Test ", "9876543210",
                "unique-temporary-secret", Set.of(StaffRole.RECEPTION, StaffRole.NURSE));

        StaffAccountResponse response = service.create(request, 88L, "admin@example.test");

        assertEquals(31L, response.id());
        assertEquals("staff@example.test", response.email());
        assertEquals(Set.of(StaffRole.RECEPTION, StaffRole.NURSE), response.roles());
        assertEquals(Set.of(StaffRole.RECEPTION, StaffRole.NURSE), savedRoles.stream()
                .filter(StaffRoleAssignment::isActive).map(StaffRoleAssignment::getRole).collect(java.util.stream.Collectors.toSet()));
        verify(assignments, times(2)).save(any(StaffRoleAssignment.class));
        verify(audit, times(3)).save(any(StaffAccountAuditEvent.class));
        verify(passwords).encode("unique-temporary-secret");
        assertFalse(response.toString().contains("unique-temporary-secret"));
    }

    @Test void firstRoleAssignmentForExistingAccountCreatesOneActiveRowAndAudit() {
        StaffAccount account = account(9L);
        when(accounts.findByIdForUpdate(9L)).thenReturn(Optional.of(account));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.RECEPTION)).thenReturn(Optional.empty());
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.NURSE)).thenReturn(Optional.empty());
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.CHECKOUT)).thenReturn(Optional.empty());
        when(assignments.save(any(StaffRoleAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(9L)).thenAnswer(invocation ->
                List.of(new StaffRoleAssignment(account, StaffRole.RECEPTION, 88L)));

        StaffAccountResponse response = service.setRoles(9L, new StaffRolesRequest(Set.of(StaffRole.RECEPTION)), 88L, "admin@example.test");

        assertEquals(Set.of(StaffRole.RECEPTION), response.roles());
        verify(assignments, times(1)).save(argThat(row -> row.getRole() == StaffRole.RECEPTION && row.isActive()));
        verify(audit, times(1)).save(any(StaffAccountAuditEvent.class));
        verify(assignments).flush();
        verify(accounts).findByIdForUpdate(9L);
    }

    @Test void roleRevocationSetsInactiveAndRecordsOneAuditEvent() throws Exception {
        StaffAccount account = account(9L);
        StaffRoleAssignment reception = new StaffRoleAssignment(account, StaffRole.RECEPTION, 88L);
        when(accounts.findByIdForUpdate(9L)).thenReturn(Optional.of(account));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.RECEPTION)).thenReturn(Optional.of(reception));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.NURSE)).thenReturn(Optional.empty());
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.CHECKOUT)).thenReturn(Optional.empty());
        when(assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(9L)).thenReturn(List.of());
        when(assignments.save(any(StaffRoleAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StaffAccountResponse response = service.setRoles(9L, new StaffRolesRequest(Set.of()), 88L, "admin@example.test");

        assertTrue(response.roles().isEmpty());
        assertFalse(reception.isActive());
        assertNotNull(reception.getRevokedAt());
        verify(assignments).save(reception);
        verify(audit, times(1)).save(any(StaffAccountAuditEvent.class));
        StaffAccountAuditEvent event = capturedAudit();
        assertEquals(88L, field(event, "actorAdminId"));
        assertEquals("admin@example.test", field(event, "actorAdminEmail"));
        assertEquals("ADMIN", field(event, "actorRole"));
        assertEquals(9L, field(event, "targetStaffId"));
        assertEquals("ROLE_REVOKED", field(event, "actionType"));
        assertEquals("RECEPTION", field(event, "detail"));
        assertNotNull(field(event, "occurredAt"));
        verify(sessions).revokeAll(9L);
    }

    @Test void reassigningRevokedRoleReactivatesSameRowAndRecordsOneAssignmentEvent() throws Exception {
        StaffAccount account = account(9L);
        StaffRoleAssignment priorRow = new StaffRoleAssignment(account, StaffRole.RECEPTION, 71L);
        priorRow.revoke();
        when(accounts.findByIdForUpdate(9L)).thenReturn(Optional.of(account));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.RECEPTION)).thenReturn(Optional.of(priorRow));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.NURSE)).thenReturn(Optional.empty());
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.CHECKOUT)).thenReturn(Optional.empty());
        when(assignments.save(any(StaffRoleAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(9L)).thenReturn(List.of(priorRow));

        StaffAccountResponse response = service.setRoles(9L, new StaffRolesRequest(Set.of(StaffRole.RECEPTION)), 88L, "admin@example.test");

        assertTrue(priorRow.isActive());
        assertNull(priorRow.getRevokedAt());
        assertEquals(88L, priorRow.getAssignedByAdminId());
        assertEquals(Set.of(StaffRole.RECEPTION), response.roles());
        verify(assignments).save(priorRow);
        verify(assignments, times(1)).save(any(StaffRoleAssignment.class));
        verify(audit, times(1)).save(any(StaffAccountAuditEvent.class));
        StaffAccountAuditEvent event = capturedAudit();
        assertEquals("ROLE_ASSIGNED", field(event, "actionType"));
        assertEquals("RECEPTION", field(event, "detail"));
        assertEquals(88L, field(event, "actorAdminId"));
    }

    @Test void repeatedAssignmentDoesNotInsertDuplicateOrDuplicateAudit() {
        StaffAccount account = account(9L);
        StaffRoleAssignment existing = new StaffRoleAssignment(account, StaffRole.RECEPTION, 88L);
        when(accounts.findByIdForUpdate(9L)).thenReturn(Optional.of(account));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.RECEPTION)).thenReturn(Optional.of(existing));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.NURSE)).thenReturn(Optional.empty());
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.CHECKOUT)).thenReturn(Optional.empty());
        when(assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(9L)).thenReturn(List.of(existing));

        service.setRoles(9L, new StaffRolesRequest(Set.of(StaffRole.RECEPTION)), 88L, "admin@example.test");

        verify(assignments, never()).save(any(StaffRoleAssignment.class));
        verify(audit, never()).save(any(StaffAccountAuditEvent.class));
    }

    @Test void concurrentAssignmentConstraintRaceReturnsConflictInsteadOfPersistenceException() {
        StaffAccount account = account(9L);
        when(accounts.findByIdForUpdate(9L)).thenReturn(Optional.of(account));
        for (StaffRole role : StaffRole.values()) when(assignments.findByStaff_IdAndRole(9L, role)).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("duplicate key details must not escape" )).when(assignments).flush();

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setRoles(9L, new StaffRolesRequest(Set.of(StaffRole.RECEPTION)), 88L, "admin@example.test"));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason().contains("changed concurrently"));
        assertFalse(error.getReason().contains("duplicate key details"));
        verify(accounts).findByIdForUpdate(9L);
    }

    @Test void roleMutationsUseParentWriteLockInsideTransactionForRevokeAndReactivate() throws Exception {
        var lockMethod = StaffAccountRepository.class.getMethod("findByIdForUpdate", Long.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lockMethod.getAnnotation(org.springframework.data.jpa.repository.Lock.class).value());
        assertNotNull(AdminStaffManagementService.class.getMethod("setRoles", Long.class, StaffRolesRequest.class, Long.class, String.class)
                .getAnnotation(Transactional.class));

        StaffAccount account = account(9L);
        StaffRoleAssignment row = new StaffRoleAssignment(account, StaffRole.RECEPTION, 88L);
        when(accounts.findByIdForUpdate(9L)).thenReturn(Optional.of(account));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.RECEPTION)).thenReturn(Optional.of(row));
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.NURSE)).thenReturn(Optional.empty());
        when(assignments.findByStaff_IdAndRole(9L, StaffRole.CHECKOUT)).thenReturn(Optional.empty());
        when(assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(9L)).thenAnswer(invocation -> row.isActive() ? List.of(row) : List.of());
        when(assignments.save(any(StaffRoleAssignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.setRoles(9L, new StaffRolesRequest(Set.of()), 88L, "admin@example.test");
        service.setRoles(9L, new StaffRolesRequest(Set.of(StaffRole.RECEPTION)), 91L, "other-admin@example.test");

        assertTrue(row.isActive());
        verify(accounts, times(2)).findByIdForUpdate(9L);
        verify(assignments, times(2)).save(row);
    }

    @Test void disablingStaffRevokesSessionsAndRecordsAudit() {
        StaffAccount account = account(4L);
        when(accounts.findById(4L)).thenReturn(Optional.of(account));
        when(accounts.save(account)).thenReturn(account);
        when(assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(4L)).thenReturn(List.of());

        service.setEnabled(4L, new StaffStatusRequest(false), 88L, "admin@example.test");

        assertFalse(account.isEnabled());
        verify(sessions).revokeAll(4L);
        verify(audit).save(any(StaffAccountAuditEvent.class));
    }

    private StaffAccountAuditEvent capturedAudit() {
        var captor = org.mockito.ArgumentCaptor.forClass(StaffAccountAuditEvent.class);
        verify(audit).save(captor.capture());
        return captor.getValue();
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static StaffAccount account(Long id) {
        StaffAccount account = new StaffAccount();
        account.setId(id);
        account.setEmail("staff@example.test");
        account.setFullName("Test Staff");
        account.setEnabled(true);
        return account;
    }
}
