package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:doctor-activity;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DoctorActivityRepositoryTest {
    @Autowired private DoctorActivityRepository activityRepository;
    @Autowired private DoctorRepository doctorRepository;
    @PersistenceContext private EntityManager entityManager;

    @Test
    void returnsOnlyAuthenticatedDoctorsEventsNewestFirst() {
        Doctor doctorA = saveDoctor("activity-a@example.test");
        Doctor doctorB = saveDoctor("activity-b@example.test");
        LocalDateTime oldest = LocalDateTime.of(2026, 11, 13, 9, 0);
        LocalDateTime newest = oldest.plusHours(2);
        activityRepository.save(new DoctorActivity(doctorA,
                DoctorActivity.ActionType.PATIENT_REGISTERED,
                DoctorActivity.EntityType.PATIENT, 10L, oldest));
        activityRepository.save(new DoctorActivity(doctorA,
                DoctorActivity.ActionType.VISIT_RECORDED,
                DoctorActivity.EntityType.VISIT, 20L, newest));
        activityRepository.save(new DoctorActivity(doctorB,
                DoctorActivity.ActionType.FOLLOW_UP_CANCELLED,
                DoctorActivity.EntityType.FOLLOW_UP, 30L, newest.plusHours(1)));
        entityManager.flush();
        entityManager.clear();

        var results = activityRepository.findByDoctor_Id(doctorA.getId(),
                PageRequest.of(0, 8,
                        Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));

        assertEquals(2, results.size());
        assertEquals(DoctorActivity.ActionType.VISIT_RECORDED, results.get(0).getActionType());
        assertEquals(DoctorActivity.ActionType.PATIENT_REGISTERED, results.get(1).getActionType());
        assertEquals(doctorA.getId(), results.get(0).getDoctor().getId());
        assertEquals(doctorA.getId(), results.get(1).getDoctor().getId());
    }

    private Doctor saveDoctor(String email) {
        Doctor doctor = new Doctor();
        doctor.setFullName("Test Doctor");
        doctor.setEmail(email);
        doctor.setPasswordHash("test-only-hash");
        doctor.setEnabled(true);
        return doctorRepository.saveAndFlush(doctor);
    }
}
