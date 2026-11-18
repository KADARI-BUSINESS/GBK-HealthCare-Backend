package com.gbkhealthcare.auth;

import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.time.LocalDateTime;
import jakarta.persistence.criteria.Predicate;

@Service
public class AdminDoctorManagementService {
    private final DoctorRepository doctors;
    private final DoctorApprovalHistoryRepository approvalHistory;

    public AdminDoctorManagementService(DoctorRepository doctors,
            DoctorApprovalHistoryRepository approvalHistory) {
        this.doctors = doctors;
        this.approvalHistory = approvalHistory;
    }

    @Transactional(readOnly = true)
    public List<AdminDoctorResponse> listPending() {
        return doctors.findAllByApprovalStatusOrderByCreatedAtAsc(Doctor.ApprovalStatus.PENDING)
                .stream().map(AdminDoctorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AdminDoctorResponse> listAll() {
        return doctors.findAllByOrderByCreatedAtAsc().stream().map(AdminDoctorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AdminDoctorPageResponse listPage(int page, int size, String search, String status) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page must be zero or greater.");
        }
        if (size != 5 && size != 10 && size != 20 && size != 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Page size must be 5, 10, 20, or 50.");
        }
        if (search != null && search.length() > 160) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Search text is too long.");
        }
        String normalizedStatus = status == null || status.isBlank() ? "all" : status;
        if (!List.of("all", "enabled", "disabled").contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account status filter is invalid.");
        }

        String normalizedSearch = search == null || search.isBlank()
                ? null : search.trim().toLowerCase(Locale.ROOT);
        Boolean enabled = switch (normalizedStatus) {
            case "enabled" -> true;
            case "disabled" -> false;
            default -> null;
        };
        List<Doctor.ApprovalStatus> matchingApprovalStatuses = java.util.Arrays.stream(Doctor.ApprovalStatus.values())
                .filter(value -> normalizedSearch != null
                        && value.name().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .toList();

        Specification<Doctor> filters = (root, query, criteria) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (enabled != null) predicates.add(criteria.equal(root.get("enabled"), enabled));
            if (normalizedSearch != null) {
                String escapedSearch = normalizedSearch.replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escapedSearch + "%";
                List<Predicate> matches = new ArrayList<>();
                matches.add(criteria.like(criteria.lower(root.get("fullName")), pattern, '\\'));
                matches.add(criteria.like(criteria.lower(root.get("email")), pattern, '\\'));
                matchingApprovalStatuses.stream()
                        .map(value -> criteria.equal(root.get("approvalStatus"), value))
                        .forEach(matches::add);
                predicates.add(criteria.or(matches.toArray(Predicate[]::new)));
            }
            return criteria.and(predicates.toArray(Predicate[]::new));
        };

        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        Page<AdminDoctorResponse> result = doctors.findAll(filters, pageable).map(AdminDoctorResponse::from);
        return AdminDoctorPageResponse.from(result);
    }

    @Transactional
    public AdminDoctorResponse updateApproval(Long id, DoctorApprovalRequest request,
            Long adminId, String adminEmail) {
        Doctor doctor = requireDoctor(id);
        if (adminId == null || adminId < 1 || adminEmail == null || adminEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated Admin identity is required.");
        }
        Doctor.ApprovalStatus nextStatus = Doctor.ApprovalStatus.valueOf(request.approvalStatus());
        String rejectionReason = request.rejectionReason() == null || request.rejectionReason().isBlank()
                ? null : request.rejectionReason().trim();
        if (nextStatus == Doctor.ApprovalStatus.APPROVED && rejectionReason != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A rejection reason can only be supplied for a rejected Doctor.");
        }
        Doctor.ApprovalStatus previousStatus = doctor.getApprovalStatus();
        doctor.setApprovalStatus(nextStatus);
        Doctor saved = doctors.save(doctor);
        approvalHistory.save(new DoctorApprovalHistory(saved, previousStatus, nextStatus,
                adminId, adminEmail.trim(), LocalDateTime.now(), rejectionReason));
        return AdminDoctorResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public DoctorApprovalHistoryPageResponse listApprovalHistory(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be non-negative and size must be between 1 and 100.");
        }
        Page<DoctorApprovalHistory> results = approvalHistory
                .findAllByOrderByDecisionAtDescIdDesc(PageRequest.of(page, size));
        List<DoctorApprovalHistoryResponse> content = results.getContent().stream()
                .map(DoctorApprovalHistoryResponse::from).toList();
        return new DoctorApprovalHistoryPageResponse(content, results.getNumber(), results.getSize(),
                results.getTotalElements(), results.hasNext());
    }

    @Transactional
    public AdminDoctorResponse updateStatus(Long id, DoctorStatusRequest request) {
        Doctor doctor = requireDoctor(id);
        doctor.setEnabled(request.enabled());
        return AdminDoctorResponse.from(doctors.save(doctor));
    }

    private Doctor requireDoctor(Long id) {
        if (id == null || id < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor ID must be positive.");
        }
        return doctors.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor account not found."));
    }
}
