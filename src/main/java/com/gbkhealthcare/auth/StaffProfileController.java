package com.gbkhealthcare.auth;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffProfileController {
    private final StaffAuthService auth;
    private final StaffAccessService access;
    public StaffProfileController(StaffAuthService auth, StaffAccessService access) { this.auth = auth; this.access = access; }
    @GetMapping("/me")
    public StaffProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
        access.requireActiveStaff(jwt);
        return auth.profile(jwt.getSubject());
    }
}
