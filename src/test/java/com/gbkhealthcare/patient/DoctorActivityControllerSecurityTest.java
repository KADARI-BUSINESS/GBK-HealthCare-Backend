package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import com.gbkhealthcare.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorActivityController.class)
@Import(SecurityConfig.class)
class DoctorActivityControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private DoctorActivityService activityService;
    @MockBean private DoctorRepository doctorRepository;
    @MockBean private JwtDecoder jwtDecoder;

    @Test
    void rejectsRequestsWithoutDoctorJwt() throws Exception {
        mockMvc.perform(get("/api/dashboard/activity"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usesDoctorIdentityFromJwtAndReturnsEmptyArrayWhenNoEvents() throws Exception {
        Doctor doctor = new Doctor();
        doctor.setId(44L);
        doctor.setEnabled(true);
        when(jwtDecoder.decode("activity-test-token")).thenReturn(
                Jwt.withTokenValue("activity-test-token")
                        .header("alg", "HS256")
                        .subject("doctor-a@example.com")
                        .claim("scope", "DOCTOR")
                        .build());
        when(doctorRepository.findByEmailIgnoreCase("doctor-a@example.com"))
                .thenReturn(Optional.of(doctor));
        when(activityService.getRecentActivities(44L, 8)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/activity")
                        .header("Authorization", "Bearer activity-test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(activityService).getRecentActivities(44L, 8);
    }
}
