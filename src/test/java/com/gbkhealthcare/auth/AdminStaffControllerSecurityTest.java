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
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminStaffController.class, StaffProfileController.class})
@Import(SecurityConfig.class)
class AdminStaffControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AdminStaffManagementService adminStaff;
    @MockBean private StaffAuthService staffAuth;
    @MockBean private StaffAccessService staffAccess;
    @MockBean private JwtDecoder jwtDecoder;

    @Test void staffAndDoctorTokensCannotAccessAdminStaffManagement() throws Exception {
        when(jwtDecoder.decode("doctor-token")).thenReturn(jwt("doctor-token", "DOCTOR"));
        when(jwtDecoder.decode("staff-token")).thenReturn(jwt("staff-token", "STAFF"));
        mockMvc.perform(get("/api/admin/staff").header("Authorization", "Bearer doctor-token")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/staff").header("Authorization", "Bearer staff-token")).andExpect(status().isForbidden());
        verifyNoInteractions(adminStaff);
    }
    @Test void doctorAndAdminTokensCannotAccessStaffProfile() throws Exception {
        when(jwtDecoder.decode("doctor-token")).thenReturn(jwt("doctor-token", "DOCTOR"));
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN"));
        mockMvc.perform(get("/api/staff/me").header("Authorization", "Bearer doctor-token")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/staff/me").header("Authorization", "Bearer admin-token")).andExpect(status().isForbidden());
        verifyNoInteractions(staffAccess, staffAuth);
    }
    @Test void adminResponseContainsStaffProfileOnlyAndNoCredentials() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN"));
        when(adminStaff.list()).thenReturn(java.util.List.of(new StaffAccountResponse(7L, "Staff One", "staff@example.test", null, Set.of(StaffRole.NURSE, StaffRole.RECEPTION), true, java.time.Instant.parse("2026-11-17T00:00:00Z"))));
        mockMvc.perform(get("/api/admin/staff").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].roles.length()").value(2))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist()).andExpect(jsonPath("$[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$[0].refreshToken").doesNotExist());
    }
    private static Jwt jwt(String value, String scope) {
        Jwt.Builder builder = Jwt.withTokenValue(value).header("alg", "HS256").subject("user@example.test").claim("scope", scope);
        if ("ADMIN".equals(scope)) builder.claim("adminId", 88L);
        if ("STAFF".equals(scope)) builder.claim("staffId", 7L);
        return builder.build();
    }
}
