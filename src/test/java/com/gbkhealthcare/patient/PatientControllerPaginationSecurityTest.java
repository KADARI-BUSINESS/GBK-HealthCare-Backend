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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@WebMvcTest(PatientController.class)
@Import(SecurityConfig.class)
class PatientControllerPaginationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientService patientService;

    @MockBean
    private DoctorRepository doctorRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void pagedPatientRouteRequiresDoctorJwt() throws Exception {
        mockMvc.perform(get("/api/patients/paged"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void doctorJwtCanRequestFivePatientRowsAndResponseIsScopedToThatDoctor() throws Exception {
        String token = "doctor-token";
        when(jwtDecoder.decode(token)).thenReturn(Jwt.withTokenValue(token)
                .header("alg", "HS256")
                .subject("doctor@example.test")
                .claim("scope", "DOCTOR")
                .build());
        Doctor doctor = new Doctor();
        doctor.setId(7L);
        doctor.setEmail("doctor@example.test");
        doctor.setEnabled(true);
        doctor.setApprovalStatus(Doctor.ApprovalStatus.APPROVED);
        when(doctorRepository.findByEmailIgnoreCase("doctor@example.test"))
                .thenReturn(Optional.of(doctor));
        when(patientService.findPageByDoctorAccountId(7L, 0, 5, null, null))
                .thenReturn(new PatientPageResponse(List.of(), 0, 0, 5, 0, true, true));

        mockMvc.perform(get("/api/patients/paged")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.size").value(5));

        verify(patientService).findPageByDoctorAccountId(7L, 0, 5, null, null);
    }

    @Test
    void unsupportedPatientPageSizeReturnsValidationDetail() throws Exception {
        String token = "doctor-token";
        when(jwtDecoder.decode(token)).thenReturn(Jwt.withTokenValue(token)
                .header("alg", "HS256")
                .subject("doctor@example.test")
                .claim("scope", "DOCTOR")
                .build());

        mockMvc.perform(get("/api/patients/paged")
                        .param("page", "0")
                        .param("size", "15")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(result -> {
                    ResponseStatusException exception = (ResponseStatusException) result.getResolvedException();
                    org.junit.jupiter.api.Assertions.assertEquals(
                            "Page size must be 5, 10, 20, or 50.", exception.getReason());
                });
    }

    @Test
    void csvExportRouteRequiresDoctorJwt() throws Exception {
        mockMvc.perform(get("/api/patients/export.csv"))
                .andExpect(status().isUnauthorized());
    }
}
