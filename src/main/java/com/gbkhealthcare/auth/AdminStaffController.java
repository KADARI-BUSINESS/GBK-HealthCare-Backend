package com.gbkhealthcare.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/staff")
public class AdminStaffController {
    private final AdminStaffManagementService service;
    public AdminStaffController(AdminStaffManagementService service) { this.service = service; }
    @GetMapping public List<StaffAccountResponse> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public StaffAccountResponse create(@Valid @RequestBody StaffCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.create(request, adminId(jwt), jwt.getSubject());
    }
    @PutMapping("/{id}/roles")
    public StaffAccountResponse roles(@PathVariable Long id, @Valid @RequestBody StaffRolesRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.setRoles(id, request, adminId(jwt), jwt.getSubject());
    }
    @PutMapping("/{id}/status")
    public StaffAccountResponse status(@PathVariable Long id, @Valid @RequestBody StaffStatusRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.setEnabled(id, request, adminId(jwt), jwt.getSubject());
    }
    private static Long adminId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin identity is required.");
        try {
            Object value = jwt.getClaims().get("adminId");
            if (value instanceof Number number) { long id = new BigDecimal(number.toString()).longValueExact(); if (id > 0) return id; }
        } catch (RuntimeException invalidClaim) { /* rejected below */ }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin identity is required.");
    }
}
