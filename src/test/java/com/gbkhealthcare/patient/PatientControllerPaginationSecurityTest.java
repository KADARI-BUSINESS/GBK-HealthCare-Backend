package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.DoctorRepository;
import com.gbkhealthcare.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void csvExportRouteRequiresDoctorJwt() throws Exception {
        mockMvc.perform(get("/api/patients/export.csv"))
                .andExpect(status().isUnauthorized());
    }
}
