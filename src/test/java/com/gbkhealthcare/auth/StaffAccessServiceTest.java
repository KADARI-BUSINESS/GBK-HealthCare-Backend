package com.gbkhealthcare.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaffAccessServiceTest {
    private final StaffAccountRepository accounts = mock(StaffAccountRepository.class);
    private final StaffRoleAssignmentRepository assignments = mock(StaffRoleAssignmentRepository.class);
    private final StaffAccessService access = new StaffAccessService(accounts, assignments);

    @Test void removedRoleIsDeniedImmediatelyEvenWhenJwtIsUnexpired() {
        StaffAccount account = new StaffAccount(); account.setId(17L); account.setEmail("staff@example.test"); account.setEnabled(true);
        StaffRoleAssignment revoked = new StaffRoleAssignment(account, StaffRole.RECEPTION, 9L); revoked.revoke();
        StaffRoleAssignment stillActive = new StaffRoleAssignment(account, StaffRole.NURSE, 9L);
        when(accounts.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(account));
        when(assignments.findAllByStaff_IdAndActiveTrueOrderByRoleAsc(17L)).thenReturn(List.of(stillActive));
        when(assignments.findByStaff_IdAndRole(17L, StaffRole.RECEPTION)).thenReturn(Optional.of(revoked));
        Jwt jwt = Jwt.withTokenValue("still-valid-staff-jwt").header("alg", "HS256").subject("staff@example.test").claim("scope", "STAFF").build();
        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> access.requireRole(jwt, StaffRole.RECEPTION));
        assertEquals(403, error.getStatusCode().value());
    }

    @Test void disabledAccountIsDeniedDespiteAnExistingStaffJwt() {
        StaffAccount account = new StaffAccount(); account.setEmail("staff@example.test"); account.setEnabled(false);
        when(accounts.findByEmailIgnoreCase("staff@example.test")).thenReturn(Optional.of(account));
        Jwt jwt = Jwt.withTokenValue("still-valid-staff-jwt").header("alg", "HS256").subject("staff@example.test").claim("scope", "STAFF").build();
        assertEquals(401, assertThrows(ResponseStatusException.class, () -> access.requireActiveStaff(jwt)).getStatusCode().value());
        verifyNoInteractions(assignments);
    }
}
