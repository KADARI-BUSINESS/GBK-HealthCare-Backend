package com.gbkhealthcare.auth;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth/doctor")
public class DoctorAuthController {

    private final DoctorAuthService doctorAuthService;

    public DoctorAuthController(DoctorAuthService doctorAuthService) {
        this.doctorAuthService = doctorAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<DoctorAuthService.LoginResult> login(
            @Valid @RequestBody DoctorLoginRequest request) {

        try {
            DoctorAuthService.LoginResult result = doctorAuthService.login(request);

            return ResponseEntity.ok(result);

        } catch (BadCredentialsException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password.");
        }
    }
}