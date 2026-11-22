package com.gbkhealthcare.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DoctorDirectoryControllerTest {
    private final DoctorRepository repository = mock(DoctorRepository.class);
    private final DoctorDirectoryController controller = new DoctorDirectoryController(repository);

    @Test
    void currentDoctorProfileUsesOnlyTheDoctorIdClaimAndReturnsSafeFields() {
        Doctor doctor = new Doctor();
        doctor.setId(60001L);
        doctor.setFullName("Local Test Doctor");
        doctor.setEmail("localdoctor@example.test");
        doctor.setEnabled(true);
        doctor.setApprovalStatus(Doctor.ApprovalStatus.APPROVED);
        when(repository.findById(60001L)).thenReturn(Optional.of(doctor));

        DoctorProfileResponse response = controller.getCurrentDoctorProfile(profileJwt(60001L));

        assertEquals(new DoctorProfileResponse(60001L, "Local Test Doctor", "localdoctor@example.test"), response);
        verify(repository).findById(60001L);
        verify(repository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void currentDoctorProfileRejectsMissingIdentityClaim() {
        assertThrows(ResponseStatusException.class,
                () -> controller.getCurrentDoctorProfile(jwt()));
        verifyNoInteractions(repository);
    }

    @Test
    void currentDoctorProfileRejectsDisabledOrUnapprovedAccount() {
        Doctor disabled = new Doctor();
        disabled.setEnabled(false);
        disabled.setApprovalStatus(Doctor.ApprovalStatus.APPROVED);
        when(repository.findById(60001L)).thenReturn(Optional.of(disabled));
        assertThrows(ResponseStatusException.class,
                () -> controller.getCurrentDoctorProfile(profileJwt(60001L)));

        Doctor pending = new Doctor();
        pending.setEnabled(true);
        pending.setApprovalStatus(Doctor.ApprovalStatus.PENDING);
        when(repository.findById(60001L)).thenReturn(Optional.of(pending));
        assertThrows(ResponseStatusException.class,
                () -> controller.getCurrentDoctorProfile(profileJwt(60001L)));
    }

    @Test
    void activeAuthenticatedDoctorCanReadPublicDirectoryProjection() {
        Doctor authenticated = new Doctor();
        authenticated.setEnabled(true);
        when(repository.findByEmailIgnoreCase("doctor@example.test")).thenReturn(Optional.of(authenticated));
        var expected = List.of(new DoctorDirectoryEntry(1L, "Doctor", "doctor@example.test", true));
        when(repository.findAllDirectoryEntries()).thenReturn(expected);
        assertEquals(expected, controller.listDoctors(jwt()));
        verify(repository).findAllDirectoryEntries();
    }

    @Test
    void disabledOrUnknownDoctorCannotReadDirectory() {
        when(repository.findByEmailIgnoreCase("doctor@example.test")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> controller.listDoctors(jwt()));
        verify(repository, never()).findAllDirectoryEntries();
    }

    private static Jwt jwt() {
        return Jwt.withTokenValue("test-token").header("alg", "none")
                .subject("doctor@example.test").build();
    }

    private static Jwt profileJwt(long doctorId) {
        return Jwt.withTokenValue("test-token").header("alg", "none")
                .subject("localdoctor@example.test").claim("doctorId", doctorId).build();
    }
}
