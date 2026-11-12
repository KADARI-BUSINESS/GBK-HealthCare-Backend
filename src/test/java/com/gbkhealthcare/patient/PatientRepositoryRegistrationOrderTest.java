package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:patient-pagination;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PatientRepositoryRegistrationOrderTest {

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientVisitRepository visitRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void ordersNewestInitialVisitFirstAndMissingInitialVisitsLast() {
        Doctor doctor = saveDoctor("doctor-a@example.test");
        Patient older = savePatient(doctor, "Older", "Male", "older@example.test");
        Patient newer = savePatient(doctor, "Newer", "Male", "newer@example.test");
        Patient missingInitialVisit = savePatient(doctor, "No Initial", "Male", "none@example.test");
        Patient onlyLaterVisit = savePatient(doctor, "Later Only", "Male", "later@example.test");

        saveVisit(older, 1, LocalDateTime.of(2025, 1, 1, 9, 0));
        saveVisit(newer, 1, LocalDateTime.of(2026, 3, 1, 9, 0));
        saveVisit(onlyLaterVisit, 2, LocalDateTime.of(2026, 4, 1, 9, 0));

        Page<Patient> page = patientRepository.findPageByDoctorAndFilters(
                doctor.getId(), null, null, PageRequest.of(0, 10));

        assertEquals(List.of(newer.getId(), older.getId(), onlyLaterVisit.getId(),
                        missingInitialVisit.getId()),
                page.getContent().stream().map(Patient::getId).toList());
    }

    @Test
    void breaksEqualInitialVisitTimestampsByPatientIdDescending() {
        Doctor doctor = saveDoctor("doctor-equal@example.test");
        LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 5, 7, 11, 30);
        Patient first = savePatient(doctor, "First", "Female", "first@example.test");
        Patient second = savePatient(doctor, "Second", "Female", "second@example.test");
        saveVisit(first, 1, sameCreatedAt);
        saveVisit(second, 1, sameCreatedAt);

        Page<Patient> page = patientRepository.findPageByDoctorAndFilters(
                doctor.getId(), null, null, PageRequest.of(0, 10));

        assertEquals(List.of(second.getId(), first.getId()),
                page.getContent().stream().map(Patient::getId).toList());
    }

    @Test
    void combinesSearchAndGenderAndScopesResultsToDoctor() {
        Doctor doctor = saveDoctor("doctor-filter@example.test");
        Doctor otherDoctor = saveDoctor("doctor-other@example.test");
        Patient matching = savePatient(doctor, "Samira", "Female", "samira@example.test");
        savePatient(doctor, "Sam", "Male", "sam@example.test");
        savePatient(doctor, "Alex", "Female", "alex@example.test");
        savePatient(otherDoctor, "Samira", "Female", "other@example.test");

        Page<Patient> page = patientRepository.findPageByDoctorAndFilters(
                doctor.getId(), "sam", "Female", PageRequest.of(0, 10));

        assertEquals(List.of(matching.getId()),
                page.getContent().stream().map(Patient::getId).toList());
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void csvProjectionUsesSameFiltersAndDoesNotReturnAnotherDoctorsRows() {
        Doctor doctor = saveDoctor("doctor-csv@example.test");
        Doctor otherDoctor = saveDoctor("doctor-csv-other@example.test");
        Patient matching = savePatient(doctor, "Samira", "Female", "samira@example.test");
        savePatient(doctor, "Sam", "Male", "sam@example.test");
        savePatient(otherDoctor, "Samira", "Female", "other@example.test");

        List<PatientCsvRow> rows = patientRepository.findCsvExportBatch(
                doctor.getId(), 0L, "sam", "Female");

        assertEquals(1, rows.size());
        assertEquals(matching.getId(), rows.get(0).getId());
        assertEquals("Samira", rows.get(0).getFullName());
        assertEquals("Female", rows.get(0).getGender());
    }

    @Test
    void dashboardAggregatesPreserveDefinitionsAndDoctorScopeInOneQuery() {
        LocalDate today = LocalDate.now();
        Doctor doctor = saveDoctor("dashboard-doctor@example.test");
        Doctor otherDoctor = saveDoctor("dashboard-other@example.test");
        Doctor disabledDoctor = saveDoctor("dashboard-disabled@example.test");
        disabledDoctor.setEnabled(false);
        entityManager.flush();

        Patient patient = savePatient(doctor, "Dashboard Patient", "Female", "dash@example.test");
        Patient secondPatient = savePatient(doctor, "Second Patient", "Male", "second-dash@example.test");
        Patient otherPatient = savePatient(otherDoctor, "Other Patient", "Female", "other-dash@example.test");
        PatientVisit documentedVisit = saveDashboardVisit(patient, today, "Consultation");
        saveDashboardVisit(secondPatient, today.minusDays(1), null);
        PatientVisit otherDoctorVisit = saveDashboardVisit(otherPatient, today, "Other consultation");
        saveFollowUp(documentedVisit, today);
        saveFollowUp(otherDoctorVisit, today);

        Object[] counts = visitRepository.findDashboardStats(doctor.getId(), today).get(0);

        assertEquals(2L, ((Number) counts[0]).longValue());
        assertEquals(1L, ((Number) counts[1]).longValue());
        assertEquals(2L, ((Number) counts[2]).longValue());
        assertEquals(1L, ((Number) counts[3]).longValue());
        assertEquals(2L, ((Number) counts[4]).longValue());
        assertEquals(1L, ((Number) counts[5]).longValue());
    }

    private PatientVisit saveDashboardVisit(Patient patient, LocalDate date, String complaints) {
        PatientVisit visit = new PatientVisit();
        visit.setPatient(patient);
        visit.setVisitDate(date);
        visit.setChiefComplaints(complaints);
        entityManager.persist(visit);
        entityManager.flush();
        return visit;
    }

    private void saveFollowUp(PatientVisit visit, LocalDate date) {
        PatientFollowUp followUp = new PatientFollowUp();
        followUp.setVisit(visit);
        followUp.setFollowUpDate(date);
        followUp.setStatus("SCHEDULED");
        entityManager.persist(followUp);
        entityManager.flush();
    }

    private Doctor saveDoctor(String email) {
        Doctor doctor = new Doctor();
        doctor.setFullName("Test Doctor");
        doctor.setEmail(email);
        doctor.setPasswordHash("test-hash");
        doctor.setEnabled(true);
        entityManager.persist(doctor);
        entityManager.flush();
        return doctor;
    }

    private Patient savePatient(Doctor doctor, String name, String gender, String email) {
        Patient patient = new Patient();
        patient.setFullName(name);
        patient.setAge(40);
        patient.setGender(gender);
        patient.setVisitDate(LocalDate.of(2026, 1, 1));
        patient.setPhone("9876543210");
        patient.setEmail(email);
        patient.setDoctor(doctor.getFullName());
        patient.setDoctorAccount(doctor);
        entityManager.persist(patient);
        entityManager.flush();
        return patient;
    }

    private void saveVisit(Patient patient, int visitNumber, LocalDateTime createdAt) {
        PatientVisit visit = new PatientVisit();
        visit.setPatient(patient);
        visit.setVisitNumber(visitNumber);
        visit.setCreatedAt(createdAt);
        entityManager.persist(visit);
        entityManager.flush();
    }
}
