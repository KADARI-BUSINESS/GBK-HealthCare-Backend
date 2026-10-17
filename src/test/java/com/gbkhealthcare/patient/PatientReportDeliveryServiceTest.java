package com.gbkhealthcare.patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatientReportDeliveryServiceTest {

    private PatientReportDeliveryRepository deliveryRepository;
    private PatientRepository patientRepository;
    private PatientVisitRepository visitRepository;
    private ClinicalReportPdfService pdfService;
    private PatientReportEmailService emailService;

    private PatientReportDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        deliveryRepository = mock(PatientReportDeliveryRepository.class);

        patientRepository = mock(PatientRepository.class);
        visitRepository = mock(PatientVisitRepository.class);
        pdfService = mock(ClinicalReportPdfService.class);
        emailService = mock(PatientReportEmailService.class);

        deliveryService = new PatientReportDeliveryService(
                deliveryRepository,
                patientRepository,
                visitRepository,
                pdfService,
                emailService);

        when(deliveryRepository.save(any(PatientReportDelivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldRejectDeliveryWhenConsentIsMissing() {

        assertThrows(
                IllegalArgumentException.class,
                () -> deliveryService.requestDelivery(
                        1L,
                        10L,
                        false));

        verifyNoInteractions(
                deliveryRepository,
                patientRepository,
                visitRepository,
                pdfService,
                emailService);
    }

    @Test
    void shouldCreatePendingDeliveryWhenConsentIsGiven() {

        Patient patient = createPatient();
        PatientVisit visit = createVisit(patient);

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        when(visitRepository.findById(10L))
                .thenReturn(Optional.of(visit));

        PatientReportDeliveryService.DeliveryResult result = deliveryService.requestDelivery(1L, 10L, true);

        assertEquals(
                PatientReportDelivery.Status.PENDING,
                result.status());

        assertEquals(0, result.attemptCount());

        verify(deliveryRepository).save(
                argThat(delivery -> delivery.isConsentGiven()
                        && "patient@example.com".equals(
                                delivery.getRecipientEmail())
                        && delivery.getPatient().getId().equals(1L)
                        && delivery.getVisit().getId().equals(10L)
                        && delivery.getStatus() == PatientReportDelivery.Status.PENDING));

        verifyNoInteractions(pdfService, emailService);
    }

    @Test
    void shouldMarkDeliverySentWhenEmailSucceeds() {

        Patient patient = createPatient();
        PatientVisit visit = createVisit(patient);

        PatientReportDelivery delivery = new PatientReportDelivery();

        delivery.setId(100L);
        delivery.setPatient(patient);
        delivery.setVisit(visit);
        delivery.setRecipientEmail("patient@example.com");
        delivery.setConsentGiven(true);
        delivery.setConsentedAt(LocalDateTime.now());
        delivery.setStatus(PatientReportDelivery.Status.PENDING);
        delivery.setAttemptCount(0);

        String pdfPassword = "TestReportPassword2026";

        byte[] encryptedPdf = "%PDF-1.7\nMocked test report"
                .getBytes(StandardCharsets.UTF_8);

        when(deliveryRepository.findById(100L))
                .thenReturn(Optional.of(delivery));

        when(patientRepository.findById(1L))
                .thenReturn(Optional.of(patient));

        when(visitRepository.findById(10L))
                .thenReturn(Optional.of(visit));

        when(pdfService.generateEncryptedClinicalReport(
                patient,
                visit,
                pdfPassword)).thenReturn(encryptedPdf);

        PatientReportDeliveryService.DeliveryResult result = deliveryService.sendPendingReport(
                100L,
                pdfPassword);

        assertEquals(
                PatientReportDelivery.Status.SENT,
                result.status());

        assertEquals(1, result.attemptCount());
        assertNotNull(result.sentAt());

        verify(emailService).sendEncryptedClinicalReport(
                "patient@example.com",
                encryptedPdf);
    }

    private Patient createPatient() {

        Patient patient = new Patient();

        patient.setId(1L);
        patient.setFullName("Test Patient");
        patient.setAge(42);
        patient.setGender("Male");
        patient.setPhone("9876543210");
        patient.setEmail("patient@example.com");
        patient.setVisitDate(LocalDate.of(2026, 10, 17));
        patient.setDoctor("Dr. Test");

        return patient;
    }

    private PatientVisit createVisit(Patient patient) {

        PatientVisit visit = new PatientVisit();

        visit.setId(10L);
        visit.setPatient(patient);
        visit.setVisitNumber(1);
        visit.setVisitDate(LocalDate.of(2026, 10, 17));

        visit.setTemperature(36.8);
        visit.setPulseRate(72);
        visit.setSpo2(98);
        visit.setBpSystolic(120);
        visit.setBpDiastolic(80);

        visit.setChiefComplaints("Test complaint");
        visit.setDiagnosis("Test diagnosis");
        visit.setTreatmentPlan("Test treatment");
        visit.setDoctor("Dr. Test");

        return visit;
    }
}