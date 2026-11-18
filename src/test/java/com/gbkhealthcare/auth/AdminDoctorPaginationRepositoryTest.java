package com.gbkhealthcare.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin-doctor-pagination;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AdminDoctorPaginationRepositoryTest {
    @Autowired private DoctorRepository doctors;
    private AdminDoctorManagementService service;

    @BeforeEach
    void setUp() {
        service = new AdminDoctorManagementService(doctors, mock(DoctorApprovalHistoryRepository.class));
        for (int i = 1; i <= 11; i++) {
            Doctor doctor = doctor(i, "Cardio Doctor " + i, true,
                    i == 1 ? Doctor.ApprovalStatus.PENDING : Doctor.ApprovalStatus.APPROVED);
            doctors.save(doctor);
        }
        doctors.save(doctor(12, "Cardio Disabled", false, Doctor.ApprovalStatus.APPROVED));
    }

    @Test
    void filtersBeforePagingAndSearchIncludesApprovalStatus() {
        AdminDoctorPageResponse first = service.listPage(0, 10, "cardio", "enabled");
        AdminDoctorPageResponse second = service.listPage(1, 10, "cardio", "enabled");
        AdminDoctorPageResponse pending = service.listPage(0, 10, "pend", "enabled");
        AdminDoctorPageResponse disabled = service.listPage(0, 10, "cardio", "disabled");

        assertEquals(11, first.totalElements());
        assertEquals(2, first.totalPages());
        assertEquals(10, first.content().size());
        assertEquals(1, second.content().size());
        assertEquals(1, pending.totalElements());
        assertEquals(Doctor.ApprovalStatus.PENDING, pending.content().get(0).approvalStatus());
        assertEquals(1, disabled.totalElements());
        assertEquals("Cardio Disabled", disabled.content().get(0).fullName());
    }

    private static Doctor doctor(long id, String name, boolean enabled, Doctor.ApprovalStatus status) {
        Doctor doctor = new Doctor();
        doctor.setFullName(name);
        doctor.setEmail("doctor" + id + "@pagination.test");
        doctor.setPasswordHash("not-a-real-credential");
        doctor.setEnabled(enabled);
        doctor.setApprovalStatus(status);
        return doctor;
    }
}
