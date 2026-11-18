package com.gbkhealthcare.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/doctors")
public class AdminDoctorController {
    private final AdminDoctorManagementService management;

    public AdminDoctorController(AdminDoctorManagementService management) {
        this.management = management;
    }

    @GetMapping("/pending")
    public List<AdminDoctorResponse> listPending() {
        return management.listPending();
    }

    @GetMapping("/paged")
    public AdminDoctorPageResponse listPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(defaultValue = "all") String status) {
        return management.listPage(page, size, search, status);
    }

    @GetMapping
    public List<AdminDoctorResponse> listAll() {
        return management.listAll();
    }

    @GetMapping("/approval-history")
    public DoctorApprovalHistoryPageResponse listApprovalHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return management.listApprovalHistory(page, size);
    }

    @PutMapping("/{id}/approval")
    public AdminDoctorResponse updateApproval(@PathVariable Long id,
            @Valid @RequestBody DoctorApprovalRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long adminId = adminId(jwt);
        return management.updateApproval(id, request, adminId, jwt.getSubject());
    }

    @PutMapping("/{id}/status")
    public AdminDoctorResponse updateStatus(@PathVariable Long id,
            @Valid @RequestBody DoctorStatusRequest request) {
        return management.updateStatus(id, request);
    }

    private static Long adminId(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin identity is required.");
        }
        try {
            Object claim = jwt.getClaims().get("adminId");
            if (claim instanceof Number number) {
                long id = new BigDecimal(number.toString()).longValueExact();
                if (id > 0) return id;
            }
        } catch (RuntimeException invalidClaim) {
            // Missing, malformed or out-of-range Admin claims are unauthorized.
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin identity is required.");
    }
}
