package com.gbkhealthcare.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminDoctorManagementServiceTest {
    private DoctorRepository doctors;
    private DoctorApprovalHistoryRepository approvalHistory;
    private AdminDoctorManagementService service;

    @BeforeEach void setUp() {
        doctors = mock(DoctorRepository.class);
        approvalHistory = mock(DoctorApprovalHistoryRepository.class);
        service = new AdminDoctorManagementService(doctors, approvalHistory);
    }

    @Test void pendingListReturnsOnlyPendingAccountsAndSafeProjection() {
        Doctor pending = doctor(7L, Doctor.ApprovalStatus.PENDING, true);
        when(doctors.findAllByApprovalStatusOrderByCreatedAtAsc(Doctor.ApprovalStatus.PENDING))
                .thenReturn(List.of(pending));

        List<AdminDoctorResponse> result = service.listPending();

        assertEquals(1, result.size());
        assertEquals(Doctor.ApprovalStatus.PENDING, result.get(0).approvalStatus());
        assertEquals("doctor-7@example.test", result.get(0).email());
        assertFalse(AdminDoctorResponse.class.getRecordComponents().length == 0);
        assertEquals(List.of("id", "fullName", "email", "mobileNumber", "qualification", "specialization",
                        "medicalRegistrationNumber", "approvalStatus", "enabled", "createdAt"),
                java.util.Arrays.stream(AdminDoctorResponse.class.getRecordComponents()).map(c -> c.getName()).toList());
    }

    @Test void approvalUpdateChangesApprovalFieldAndPreservesDoctorIdAndEnabledState() {
        Doctor doctor = doctor(1L, Doctor.ApprovalStatus.PENDING, true);
        when(doctors.findById(1L)).thenReturn(Optional.of(doctor));
        when(doctors.save(doctor)).thenReturn(doctor);

        AdminDoctorResponse result = service.updateApproval(1L, new DoctorApprovalRequest("APPROVED"),
                88L, "admin@example.test");

        assertEquals(1L, result.id());
        assertEquals(Doctor.ApprovalStatus.APPROVED, doctor.getApprovalStatus());
        assertTrue(doctor.isEnabled());
        var captor = org.mockito.ArgumentCaptor.forClass(DoctorApprovalHistory.class);
        verify(approvalHistory).save(captor.capture());
        assertEquals(Doctor.ApprovalStatus.PENDING, captor.getValue().getPreviousApprovalStatus());
        assertEquals(Doctor.ApprovalStatus.APPROVED, captor.getValue().getNewApprovalStatus());
        assertEquals(88L, captor.getValue().getAdminId());
        assertEquals("admin@example.test", captor.getValue().getAdminEmail());
        assertNotNull(captor.getValue().getDecisionAt());
        assertNull(captor.getValue().getRejectionReason());
        verify(doctors, never()).delete(any(Doctor.class));
    }

    @Test void repeatedRejectionDecisionsAppendSeparateHistoryRowsAndKeepReason() {
        Doctor doctor = doctor(13L, Doctor.ApprovalStatus.PENDING, true);
        when(doctors.findById(13L)).thenReturn(Optional.of(doctor));
        when(doctors.save(doctor)).thenReturn(doctor);

        service.updateApproval(13L, new DoctorApprovalRequest("REJECTED", "  Incomplete credentials  "),
                88L, "admin@example.test");
        service.updateApproval(13L, new DoctorApprovalRequest("REJECTED", null),
                88L, "admin@example.test");

        var captor = org.mockito.ArgumentCaptor.forClass(DoctorApprovalHistory.class);
        verify(approvalHistory, times(2)).save(captor.capture());
        assertEquals(Doctor.ApprovalStatus.PENDING, captor.getAllValues().get(0).getPreviousApprovalStatus());
        assertEquals("Incomplete credentials", captor.getAllValues().get(0).getRejectionReason());
        assertEquals(Doctor.ApprovalStatus.REJECTED, captor.getAllValues().get(1).getPreviousApprovalStatus());
        assertNull(captor.getAllValues().get(1).getRejectionReason());
    }

    @Test void historyListUsesNewestFirstRepositoryOrderingAndPagination() {
        Doctor doctor = doctor(3L, Doctor.ApprovalStatus.APPROVED, true);
        DoctorApprovalHistory row = new DoctorApprovalHistory(doctor, Doctor.ApprovalStatus.PENDING,
                Doctor.ApprovalStatus.APPROVED, 88L, "admin@example.test", LocalDateTime.now(), null);
        when(approvalHistory.findAllByOrderByDecisionAtDescIdDesc(PageRequest.of(0, 25)))
                .thenReturn(new PageImpl<>(List.of(row), PageRequest.of(0, 25), 1));

        DoctorApprovalHistoryPageResponse result = service.listApprovalHistory(0, 25);

        assertEquals(1, result.totalElements());
        assertEquals(3L, result.content().get(0).doctorId());
        assertEquals(88L, result.content().get(0).adminId());
        assertFalse(result.hasNext());
    }

    @Test void doctorDirectoryPageUsesRequestedPageAndStableSort() {
        Doctor doctor = doctor(28L, Doctor.ApprovalStatus.APPROVED, true);
        PageRequest pageable = PageRequest.of(1, 10,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        when(doctors.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Doctor>>any(),
                eq(pageable))).thenReturn(new PageImpl<>(List.of(doctor), pageable, 11));

        AdminDoctorPageResponse result = service.listPage(1, 10, "doctor", "enabled");

        assertEquals(11, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals(1, result.number());
        assertEquals(1, result.content().size());
        assertEquals(28L, result.content().get(0).id());
        verify(doctors).findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Doctor>>any(),
                eq(pageable));
    }

    @Test void doctorDirectoryPageAcceptsFiveRowsForAdminDefaultPageSize() {
        PageRequest pageable = PageRequest.of(0, 5,
                Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id")));
        when(doctors.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Doctor>>any(),
                eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        AdminDoctorPageResponse result = service.listPage(0, 5, "", "all");

        assertEquals(5, result.size());
        assertEquals(0, result.totalElements());
        verify(doctors).findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Doctor>>any(),
                eq(pageable));
    }

    @Test void doctorDirectoryPageRejectsInvalidPaginationAndFilterValues() {
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.listPage(-1, 10, "", "all")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.listPage(0, 15, "", "all")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.listPage(0, 10, "", "unknown")).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(ResponseStatusException.class,
                () -> service.listPage(0, 10, "x".repeat(161), "all")).getStatusCode());
        verifyNoInteractions(doctors);
    }

    @Test void statusUpdateChangesOnlyEnabledField() {
        Doctor doctor = doctor(9L, Doctor.ApprovalStatus.REJECTED, true);
        when(doctors.findById(9L)).thenReturn(Optional.of(doctor));
        when(doctors.save(doctor)).thenReturn(doctor);

        AdminDoctorResponse result = service.updateStatus(9L, new DoctorStatusRequest(false));

        assertFalse(result.enabled());
        assertEquals(Doctor.ApprovalStatus.REJECTED, result.approvalStatus());
        verify(doctors, never()).delete(any(Doctor.class));
        verifyNoInteractions(approvalHistory);
    }

    @Test void rejectionReasonCannotBeAttachedToApproval() {
        Doctor doctor = doctor(10L, Doctor.ApprovalStatus.PENDING, true);
        when(doctors.findById(10L)).thenReturn(Optional.of(doctor));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.updateApproval(10L, new DoctorApprovalRequest("APPROVED", "reason"),
                        88L, "admin@example.test"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals(Doctor.ApprovalStatus.PENDING, doctor.getApprovalStatus());
        verify(doctors, never()).save(any());
        verifyNoInteractions(approvalHistory);
    }

    @Test void missingDoctorReturnsNotFoundAndInvalidIdReturnsBadRequest() {
        when(doctors.findById(44L)).thenReturn(Optional.empty());
        ResponseStatusException missing = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(44L, new DoctorStatusRequest(false)));
        assertEquals(HttpStatus.NOT_FOUND, missing.getStatusCode());

        ResponseStatusException invalid = assertThrows(ResponseStatusException.class,
                () -> service.updateStatus(0L, new DoctorStatusRequest(false)));
        assertEquals(HttpStatus.BAD_REQUEST, invalid.getStatusCode());
    }

    private static Doctor doctor(Long id, Doctor.ApprovalStatus status, boolean enabled) {
        Doctor doctor = new Doctor();
        doctor.setId(id); doctor.setFullName("Doctor " + id); doctor.setEmail("doctor-" + id + "@example.test");
        doctor.setPasswordHash("must-not-be-exposed"); doctor.setApprovalStatus(status); doctor.setEnabled(enabled);
        // Existing ownership/creation metadata is not rewritten by status management.
        return doctor;
    }
}
