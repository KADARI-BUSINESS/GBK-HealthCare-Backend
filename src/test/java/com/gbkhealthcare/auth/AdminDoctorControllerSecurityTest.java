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

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminDoctorController.class)
@Import(SecurityConfig.class)
class AdminDoctorControllerSecurityTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AdminDoctorManagementService management;
    @MockBean private JwtDecoder jwtDecoder;

    @Test void rejectsUnauthenticatedAdminApiRequests() throws Exception {
        mockMvc.perform(get("/api/admin/doctors")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/admin/doctors/approval-history")).andExpect(status().isUnauthorized());
        verifyNoInteractions(management);
    }

    @Test void doctorJwtCannotAccessAdminDoctorEndpoints() throws Exception {
        when(jwtDecoder.decode("doctor-token")).thenReturn(jwt("doctor-token", "DOCTOR"));
        mockMvc.perform(get("/api/admin/doctors").header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/doctors/paged").header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/doctors/pending").header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/doctors/approval-history").header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/doctors/4/approval").header("Authorization", "Bearer doctor-token")
                        .contentType("application/json").content("{\"approvalStatus\":\"APPROVED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/doctors/4/status").header("Authorization", "Bearer doctor-token")
                        .contentType("application/json").content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(management);
    }

    @Test void adminJwtCanAccessAndResponseContainsNoAuthenticationSecrets() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN"));
        when(management.listAll()).thenReturn(List.of(new AdminDoctorResponse(1L, "Doctor One", "doctor@example.test",
                null, null, null, null, Doctor.ApprovalStatus.APPROVED, true, null)));

        mockMvc.perform(get("/api/admin/doctors").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$[0].enabled").value(true))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$[0].refreshToken").doesNotExist());
    }

    @Test void adminJwtCanListPendingAndUpdateApprovalAndEnabledStatus() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN"));
        AdminDoctorResponse pending = new AdminDoctorResponse(12L, "Doctor Two", "doctor2@example.test",
                "9876543210", "MBBS", "Cardiology", null, Doctor.ApprovalStatus.PENDING, true, null);
        AdminDoctorResponse approved = new AdminDoctorResponse(12L, "Doctor Two", "doctor2@example.test",
                "9876543210", "MBBS", "Cardiology", null, Doctor.ApprovalStatus.APPROVED, true, null);
        AdminDoctorResponse disabled = new AdminDoctorResponse(12L, "Doctor Two", "doctor2@example.test",
                "9876543210", "MBBS", "Cardiology", null, Doctor.ApprovalStatus.APPROVED, false, null);
        when(management.listPending()).thenReturn(List.of(pending));
        when(management.updateApproval(eq(12L), any(DoctorApprovalRequest.class), eq(88L), eq("admin@example.test")))
                .thenReturn(approved);
        when(management.updateStatus(eq(12L), any(DoctorStatusRequest.class))).thenReturn(disabled);
        DoctorApprovalHistoryResponse historyItem = new DoctorApprovalHistoryResponse(2L, 12L, "Doctor Two",
                "doctor2@example.test", Doctor.ApprovalStatus.PENDING, Doctor.ApprovalStatus.APPROVED,
                88L, "admin@example.test", java.time.LocalDateTime.now(), null);
        when(management.listApprovalHistory(0, 25)).thenReturn(
                new DoctorApprovalHistoryPageResponse(List.of(historyItem), 0, 25, 1, false));

        mockMvc.perform(get("/api/admin/doctors/pending").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].approvalStatus").value("PENDING"));
        mockMvc.perform(put("/api/admin/doctors/12/approval").header("Authorization", "Bearer admin-token")
                        .contentType("application/json").content("{\"approvalStatus\":\"APPROVED\",\"rejectionReason\":null}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.approvalStatus").value("APPROVED"));
        mockMvc.perform(put("/api/admin/doctors/12/status").header("Authorization", "Bearer admin-token")
                        .contentType("application/json").content("{\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        mockMvc.perform(get("/api/admin/doctors/approval-history").header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].doctorId").value(12))
                .andExpect(jsonPath("$.content[0].adminId").value(88))
                .andExpect(jsonPath("$.content[0].adminEmail").value("admin@example.test"))
                .andExpect(jsonPath("$.content[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[0].refreshToken").doesNotExist());
    }

    @Test void adminJwtCanRequestFilteredDoctorPage() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN"));
        AdminDoctorResponse doctor = new AdminDoctorResponse(12L, "Doctor Two", "doctor2@example.test",
                "9876543210", "MBBS", "Cardiology", null, Doctor.ApprovalStatus.APPROVED, true, null);
        when(management.listPage(1, 10, "cardio", "enabled"))
                .thenReturn(new AdminDoctorPageResponse(List.of(doctor), 11, 2, 10, 1, false, true));

        mockMvc.perform(get("/api/admin/doctors/paged")
                        .param("page", "1").param("size", "10")
                        .param("search", "cardio").param("status", "enabled")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(12))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.number").value(1));
    }

    @Test void rejectsInvalidApprovalAndMissingEnabledValue() throws Exception {
        when(jwtDecoder.decode("admin-token")).thenReturn(jwt("admin-token", "ADMIN"));
        mockMvc.perform(put("/api/admin/doctors/4/approval")
                        .header("Authorization", "Bearer admin-token")
                        .contentType("application/json")
                        .content("{\"approvalStatus\":\"PENDING\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/api/admin/doctors/4/status")
                        .header("Authorization", "Bearer admin-token")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(management);
    }

    private static Jwt jwt(String value, String scope) {
        Jwt.Builder builder = Jwt.withTokenValue(value).header("alg", "HS256").subject("admin@example.test")
                .claim("scope", scope);
        if ("ADMIN".equals(scope)) builder.claim("adminId", 88L);
        return builder.build();
    }
}
