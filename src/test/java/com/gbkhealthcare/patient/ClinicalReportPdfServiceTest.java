package com.gbkhealthcare.patient;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClinicalReportPdfServiceTest {

    private final ClinicalReportPdfService pdfService =
            new ClinicalReportPdfService();

    @Test
    void shouldGeneratePdfContainingPatientAndClinicalDetails()
            throws Exception {

        Patient patient = createTestPatient();

        PatientVisit visit = createTestVisit(patient);

        byte[] pdfBytes =
                pdfService.generateClinicalReport(patient, visit);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 100);

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {

            assertTrue(document.getNumberOfPages() >= 1);

            String reportText =
                    new PDFTextStripper().getText(document);

            assertTrue(reportText.contains("GBK HealthCare"));
            assertTrue(reportText.contains("Test Patient"));
            assertTrue(reportText.contains("test.patient@example.com"));
            assertTrue(reportText.contains("Temperature"));
	    assertTrue(reportText.contains("36.8"));
	    assertTrue(reportText.contains("Pulse Rate"));
            assertTrue(reportText.contains("72 bpm"));
            assertTrue(reportText.contains("120/80 mmHg"));
            assertTrue(reportText.contains("Headache"));
            assertTrue(reportText.contains("Test diagnosis"));
            assertTrue(reportText.contains("Hydration and review"));
        }
    }

    @Test
    void shouldRejectVisitBelongingToAnotherPatient() {

        Patient patient1 = createTestPatient();
        patient1.setId(101L);

        Patient patient2 = createTestPatient();
        patient2.setId(202L);

        PatientVisit visit = createTestVisit(patient2);

        assertThrows(
                IllegalArgumentException.class,
                () -> pdfService.generateClinicalReport(
                        patient1, visit)
        );
    }

    @Test
    void encryptedReportShouldRequirePassword() throws Exception {

        Patient patient = createTestPatient();

        PatientVisit visit = createTestVisit(patient);

        String pdfPassword = "TestReportPassword2026";

        byte[] encryptedPdf =
                pdfService.generateEncryptedClinicalReport(
                        patient,
                        visit,
                        pdfPassword
                );

        assertNotNull(encryptedPdf);
        assertTrue(encryptedPdf.length > 100);

        // Without a password, opening the encrypted PDF must fail.
        assertThrows(
                InvalidPasswordException.class,
                () -> Loader.loadPDF(encryptedPdf)
        );

        // The correct password must successfully open the report.
        try (PDDocument document =
                     Loader.loadPDF(encryptedPdf, pdfPassword)) {

            assertTrue(document.getNumberOfPages() >= 1);

            String reportText =
                    new PDFTextStripper().getText(document);

            assertTrue(reportText.contains("Test Patient"));
            assertTrue(reportText.contains("Test diagnosis"));
        }
    }

    private Patient createTestPatient() {

        Patient patient = new Patient();

        patient.setId(101L);
        patient.setFullName("Test Patient");
        patient.setAge(42);
        patient.setGender("Male");
        patient.setPhone("9876543210");
        patient.setEmail("test.patient@example.com");
        patient.setVisitDate(LocalDate.of(2026, 10, 17));
        patient.setDoctor("Dr. Test");

        return patient;
    }

    private PatientVisit createTestVisit(Patient patient) {

        PatientVisit visit = new PatientVisit();

        visit.setId(501L);
        visit.setPatient(patient);
        visit.setVisitNumber(1);
        visit.setVisitDate(LocalDate.of(2026, 10, 17));

        visit.setTemperature(36.8);
        visit.setPulseRate(72);
        visit.setSpo2(98);
        visit.setBpSystolic(120);
        visit.setBpDiastolic(80);

        visit.setChiefComplaints("Headache");
        visit.setPresentIllness("Mild headache since morning");
        visit.setGeneralExamination("Patient is alert");
        visit.setHeartExamination("Normal");
        visit.setLungsExamination("Normal");
        visit.setDiagnosis("Test diagnosis");
        visit.setTreatmentPlan("Hydration and review");

        visit.setDoctor("Dr. Test");

        return visit;
    }
}