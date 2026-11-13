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
}
