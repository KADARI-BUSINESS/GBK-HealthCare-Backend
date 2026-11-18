package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.StaffAccessService;
import com.gbkhealthcare.auth.StaffAccount;
import com.gbkhealthcare.auth.StaffRole;
import com.gbkhealthcare.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReceptionController.class)
@Import(SecurityConfig.class)
class ReceptionControllerSecurityTest {
    @Autowired private MockMvc mvc;
    @MockBean private StaffAccessService access;
    @MockBean private ReceptionService reception;
    @MockBean private JwtDecoder decoder;

    @Test void receptionStaffRoleCanSearchPatients() throws Exception {
        when(decoder.decode("reception-token")).thenReturn(jwt("reception-token", "STAFF"));
        StaffAccount actor = mock(StaffAccount.class); when(actor.getId()).thenReturn(44L);
        when(access.requireRole(any(), eq(StaffRole.RECEPTION))).thenReturn(actor);
        when(reception.searchPatients("Ravi")).thenReturn(java.util.List.of());
        mvc.perform(get("/api/reception/patients?query=Ravi").header("Authorization", "Bearer reception-token"))
                .andExpect(status().isOk());
        verify(reception).searchPatients("Ravi");
    }

    @Test void staffWithoutReceptionRoleIsForbidden() throws Exception {
        when(decoder.decode("staff-token")).thenReturn(jwt("staff-token", "STAFF"));
        when(access.requireRole(any(), eq(StaffRole.RECEPTION)))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mvc.perform(get("/api/reception/patients?query=Ravi").header("Authorization", "Bearer staff-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reception);
    }

    @Test void doctorTokenCannotAccessReceptionEndpoint() throws Exception {
        when(decoder.decode("doctor-token")).thenReturn(jwt("doctor-token", "DOCTOR"));
        mvc.perform(get("/api/reception/patients?query=Ravi").header("Authorization", "Bearer doctor-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(access, reception);
    }

    @Test void anonymousRequestIsUnauthorized() throws Exception {
        mvc.perform(get("/api/reception/patients?query=Ravi")).andExpect(status().isUnauthorized());
        verifyNoInteractions(access, reception);
    }

    @Test void receptionFollowUpLifecycleRequiresActiveReceptionRole() throws Exception {
        when(decoder.decode("reception-token")).thenReturn(jwt("reception-token", "STAFF"));
        when(access.requireRole(any(), eq(StaffRole.RECEPTION))).thenReturn(mock(StaffAccount.class));
        when(reception.followUpsForReception("SCHEDULED")).thenReturn(java.util.List.of());
        mvc.perform(get("/api/reception/follow-ups?status=SCHEDULED").header("Authorization", "Bearer reception-token"))
                .andExpect(status().isOk());
        verify(reception).followUpsForReception("SCHEDULED");
    }

    @Test void staffWithoutReceptionRoleCannotReadFollowUpLifecycle() throws Exception {
        when(decoder.decode("staff-token")).thenReturn(jwt("staff-token", "STAFF"));
        when(access.requireRole(any(), eq(StaffRole.RECEPTION))).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN));
        mvc.perform(get("/api/reception/follow-ups").header("Authorization", "Bearer staff-token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(reception);
    }

    private static Jwt jwt(String value, String scope) {
        return Jwt.withTokenValue(value).header("alg", "HS256").subject("staff@example.test")
                .claim("scope", scope).build();
    }
}
