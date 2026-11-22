package com.gbkhealthcare.auth;

import com.gbkhealthcare.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorAuthController.class)
@Import(SecurityConfig.class)
class DoctorRegistrationControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private DoctorAuthService doctorAuthService;
    @MockBean private RefreshSessionManager refreshSessions;
    @MockBean private JwtDecoder jwtDecoder;

    @Test
    void registrationEndpointReturnsPendingApplicationCreated() throws Exception {
        when(doctorAuthService.register(any(DoctorRegistrationRequest.class)))
                .thenReturn(new DoctorAuthService.RegistrationResult(60002L, "Local Test Doctor",
                        "localdoctor@example.test", "PENDING", "Registration received."));

        mockMvc.perform(post("/api/auth/doctor/register")
                        .header("Origin", "http://localhost:5173")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"fullName":"Local Test Doctor","email":"localdoctor@example.test",
                                 "mobileNumber":"9876543210","qualification":"MBBS",
                                 "specialization":"General Medicine","medicalRegistrationNumber":"",
                                 "password":"a-long-test-password"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.doctorId").value(60002))
                .andExpect(jsonPath("$.approvalStatus").value("PENDING"));
        verify(doctorAuthService).register(any(DoctorRegistrationRequest.class));
    }

    @Test
    void invalidMobileIsRejectedBeforeRegistrationService() throws Exception {
        mockMvc.perform(post("/api/auth/doctor/register")
                        .header("Origin", "http://localhost:5173")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"fullName":"Local Test Doctor","email":"localdoctor@example.test",
                                 "mobileNumber":"1234567890","qualification":"MBBS",
                                 "specialization":"General Medicine","medicalRegistrationNumber":null,
                                 "password":"a-long-test-password"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(doctorAuthService);
    }
}
