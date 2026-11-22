package com.gbkhealthcare.auth;

import com.gbkhealthcare.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorDirectoryController.class)
@Import(SecurityConfig.class)
class DoctorProfileControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private DoctorRepository doctors;
    @MockBean private JwtDecoder jwtDecoder;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/doctors/me")).andExpect(status().isUnauthorized());
        verifyNoInteractions(doctors);
    }

    @Test
    void doctorJwtReadsProfileBoundToItsDoctorIdClaim() throws Exception {
        when(jwtDecoder.decode("doctor-token")).thenReturn(jwt("doctor-token", "DOCTOR", 60001L));
        Doctor doctor = new Doctor();
        doctor.setId(60001L);
        doctor.setFullName("Local Test Doctor");
        doctor.setEmail("localdoctor@example.test");
        doctor.setEnabled(true);
        doctor.setApprovalStatus(Doctor.ApprovalStatus.APPROVED);
        when(doctors.findById(60001L)).thenReturn(Optional.of(doctor));

        mockMvc.perform(get("/api/doctors/me").param("doctorId", "1")
                        .header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId").value(60001))
                .andExpect(jsonPath("$.fullName").value("Local Test Doctor"))
                .andExpect(jsonPath("$.email").value("localdoctor@example.test"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
        verify(doctors).findById(60001L);
    }

    @Test
    void adminJwtCannotReadDoctorProfile() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN", null));

        mockMvc.perform(get("/api/doctors/me").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(doctors);
    }

    private static Jwt jwt(String value, String scope, Long doctorId) {
        Jwt.Builder builder = Jwt.withTokenValue(value).header("alg", "HS256")
                .subject("localdoctor@example.test").claim("scope", scope);
        if (doctorId != null) builder.claim("doctorId", doctorId);
        return builder.build();
    }
}
