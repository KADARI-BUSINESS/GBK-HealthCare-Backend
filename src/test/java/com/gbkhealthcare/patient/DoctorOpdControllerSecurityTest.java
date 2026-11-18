package com.gbkhealthcare.patient;

import com.gbkhealthcare.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorOpdController.class)
@Import(SecurityConfig.class)
class DoctorOpdControllerSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private DoctorOpdService opd;
    @MockBean private DoctorClinicalOrderService orders;
    @MockBean private JwtDecoder decoder;

    @Test void doctorScopeCanReachDoctorQueue() throws Exception {
        when(decoder.decode("doctor-token")).thenReturn(jwt("doctor-token", "DOCTOR"));
        when(opd.queue(any())).thenReturn(java.util.List.of());
        mvc.perform(get("/api/doctor-opd/queue").header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isOk());
        verify(opd).queue(any());
    }

    @Test void staffAndAdminTokensCannotReachDoctorQueue() throws Exception {
        when(decoder.decode("staff-token")).thenReturn(jwt("staff-token", "STAFF"));
        when(decoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN"));
        mvc.perform(get("/api/doctor-opd/queue").header("Authorization", "Bearer staff-token")).andExpect(status().isForbidden());
        mvc.perform(get("/api/doctor-opd/queue").header("Authorization", "Bearer admin-token")).andExpect(status().isForbidden());
        verifyNoInteractions(opd);
    }

    @Test void anonymousRequestIsRejected() throws Exception {
        mvc.perform(get("/api/doctor-opd/queue")).andExpect(status().isUnauthorized());
        verifyNoInteractions(opd);
    }

    private static Jwt jwt(String value, String scope) {
        return Jwt.withTokenValue(value).header("alg", "HS256").subject("doctor@example.test").claim("scope", scope).build();
    }
}
