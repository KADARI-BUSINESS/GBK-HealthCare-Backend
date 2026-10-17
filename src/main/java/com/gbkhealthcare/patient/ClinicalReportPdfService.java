package com.gbkhealthcare.patient;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
public class ClinicalReportPdfService {

    private final PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private final PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates an unencrypted clinical report PDF.
     *
     * Patient demographics are read from Patient.
     * Clinical details are read from the selected PatientVisit.
     */
    public byte[] generateClinicalReport(
            Patient patient,
            PatientVisit visit) {

        validatePatientAndVisit(patient, visit);

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            try (ReportWriter writer = new ReportWriter(document)) {
                addPatientInformation(writer, patient, visit);
                addVitals(writer, visit);
                addClinicalExamination(writer, visit);
                addAssessmentAndFollowUp(writer, visit);

                writer.addParagraph(
                        "CONFIDENTIAL: This report contains sensitive " +
                                "health information. Share it only with the " +
                                "intended patient and authorized care team.");
            }

            document.save(output);
            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to generate the clinical report PDF.",
                    exception);
        }
    }

    /**
     * Generates a password-protected clinical report PDF.
     *
     * The user password is required to open the PDF.
     * The separate owner password is generated internally and is never
     * returned or included in the patient email.
     */
    public byte[] generateEncryptedClinicalReport(
            Patient patient,
            PatientVisit visit,
            String userPassword) {

        if (userPassword == null || userPassword.length() < 12) {
            throw new IllegalArgumentException(
                    "The PDF password must contain at least 12 characters.");
        }

        byte[] originalPdf = generateClinicalReport(patient, visit);

        try (
                PDDocument document = Loader.loadPDF(originalPdf);
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            AccessPermission permissions = new AccessPermission();

            // Restrict editing and copying-related operations.
            // PDF reader implementations may enforce permissions differently.
            permissions.setCanModify(false);
            permissions.setCanModifyAnnotations(false);
            permissions.setCanFillInForm(false);
            permissions.setCanAssembleDocument(false);
            permissions.setCanExtractContent(false);

            // Keep printing enabled for the patient.
            permissions.setCanPrint(true);
            permissions.setCanPrintFaithful(true);

            StandardProtectionPolicy policy = new StandardProtectionPolicy(
                    generateOwnerPassword(),
                    userPassword,
                    permissions);

            // Use 256-bit PDF encryption.
            policy.setEncryptionKeyLength(256);

            document.protect(policy);
            document.save(output);

            return output.toByteArray();

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to encrypt the clinical report PDF.",
                    exception);
        }
    }

    /**
     * Creates a random owner password for PDF encryption.
     *
     * This password must not be sent to the patient.
     */
    private String generateOwnerPassword() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private void validatePatientAndVisit(
            Patient patient,
            PatientVisit visit) {

        if (patient == null || patient.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved patient is required to generate the report.");
        }

        if (visit == null || visit.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved patient visit is required to generate the report.");
        }

        Long visitPatientId = visit.getPatient() == null
                ? null
                : visit.getPatient().getId();

        if (!Objects.equals(patient.getId(), visitPatientId)) {
            throw new IllegalArgumentException(
                    "The selected visit does not belong to this patient.");
        }
    }

    private void addPatientInformation(
            ReportWriter writer,
            Patient patient,
            PatientVisit visit) throws IOException {

        writer.addSection("Patient Information");

        writer.addField("Patient ID", patient.getId());
        writer.addField("Patient Name", patient.getFullName());
        writer.addField("Age", patient.getAge());
        writer.addField("Gender", patient.getGender());
        writer.addField("Phone", patient.getPhone());
        writer.addField("Email", patient.getEmail());
        writer.addField("Visit Number", visit.getVisitNumber());
        writer.addField("Visit Date", visit.getVisitDate());

        writer.addField(
                "Doctor",
                firstNonBlank(visit.getDoctor(), patient.getDoctor()));
    }

    private void addVitals(
            ReportWriter writer,
            PatientVisit visit) throws IOException {

        writer.addSection("Vital Signs");

        writer.addField(
                "Temperature",
                withUnit(visit.getTemperature(), " °C"));

        writer.addField(
                "Pulse Rate",
                withUnit(visit.getPulseRate(), " bpm"));

        writer.addField(
                "Oxygen Saturation (SpO2)",
                withUnit(visit.getSpo2(), " %"));

        writer.addField(
                "Blood Pressure",
                bloodPressure(visit));
    }

    private void addClinicalExamination(
            ReportWriter writer,
            PatientVisit visit) throws IOException {

        writer.addSection("Clinical Examination");

        writer.addField(
                "Chief Complaints",
                visit.getChiefComplaints());

        writer.addField(
                "Present Illness",
                visit.getPresentIllness());

        writer.addField(
                "General Examination",
                visit.getGeneralExamination());

        writer.addField(
                "Heart Examination",
                visit.getHeartExamination());

        writer.addField(
                "Lungs Examination",
                visit.getLungsExamination());

        writer.addField(
                "Abdomen Examination",
                visit.getAbdomenExamination());

        writer.addField(
                "Rectal Examination",
                visit.getRectalExamination());
    }

    private void addAssessmentAndFollowUp(
            ReportWriter writer,
            PatientVisit visit) throws IOException {

        writer.addSection("Assessment and Follow-up");

        writer.addField("Diagnosis", visit.getDiagnosis());
        writer.addField("Treatment Plan", visit.getTreatmentPlan());
        writer.addField("Follow-up Date", visit.getFollowUpDate());
        writer.addField("Follow-up Notes", visit.getFollowUpNotes());
    }

    private String bloodPressure(PatientVisit visit) {

        Integer systolic = visit.getBpSystolic();
        Integer diastolic = visit.getBpDiastolic();

        if (systolic == null && diastolic == null) {
            return null;
        }

        String systolicValue = systolic == null
                ? "Not recorded"
                : systolic.toString();

        String diastolicValue = diastolic == null
                ? "Not recorded"
                : diastolic.toString();

        return systolicValue + "/" + diastolicValue + " mmHg";
    }

    private String withUnit(Object value, String unit) {
        return value == null ? null : value + unit;
    }

    private String firstNonBlank(String first, String second) {

        if (first != null && !first.isBlank()) {
            return first;
        }

        return second;
    }

    /**
     * Writes a formatted, multi-page clinical PDF.
     */
    private final class ReportWriter implements AutoCloseable {

        private static final float MARGIN = 48;
        private static final float BODY_FONT_SIZE = 10;
        private static final float LINE_HEIGHT = 14;

        private final PDDocument document;

        private PDPageContentStream contentStream;
        private float yPosition;

        private ReportWriter(PDDocument document) throws IOException {
            this.document = document;
            startNewPage();
        }

        private void startNewPage() throws IOException {

            if (contentStream != null) {
                contentStream.close();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            contentStream = new PDPageContentStream(document, page);

            yPosition = page.getMediaBox().getHeight() - MARGIN;

            drawText("GBK HealthCare", boldFont, 18, yPosition);
            yPosition -= 23;

            drawText(
                    "PATIENT CLINICAL REPORT",
                    boldFont,
                    12,
                    yPosition);

            yPosition -= 18;

            contentStream.moveTo(MARGIN, yPosition);
            contentStream.lineTo(
                    page.getMediaBox().getWidth() - MARGIN,
                    yPosition);
            contentStream.stroke();

            yPosition -= 18;
        }

        private void addSection(String title) throws IOException {

            ensureSpace(30);
            yPosition -= 3;

            writeLine(title, boldFont, 12, 17);

            yPosition -= 2;
        }

        private void addField(String label, Object value)
                throws IOException {

            String displayValue;

            if (value == null || value.toString().isBlank()) {
                displayValue = "Not recorded";
            } else {
                displayValue = value.toString().trim();
            }

            addParagraph(label + ": " + displayValue);
        }

        private void addParagraph(String text) throws IOException {

            String cleanedText = toSupportedText(text);

            List<String> lines = wrapText(
                    cleanedText,
                    regularFont,
                    BODY_FONT_SIZE,
                    PDRectangle.A4.getWidth() - (2 * MARGIN));

            for (String line : lines) {
                writeLine(
                        line,
                        regularFont,
                        BODY_FONT_SIZE,
                        LINE_HEIGHT);
            }

            yPosition -= 2;
        }

        private void writeLine(
                String text,
                PDFont font,
                float fontSize,
                float lineHeight) throws IOException {

            ensureSpace(lineHeight);

            drawText(text, font, fontSize, yPosition);
            yPosition -= lineHeight;
        }

        private void drawText(
                String text,
                PDFont font,
                float fontSize,
                float y) throws IOException {

            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(MARGIN, y);
            contentStream.showText(toSupportedText(text));
            contentStream.endText();
        }

        private void ensureSpace(float requiredSpace) throws IOException {

            if (yPosition - requiredSpace < MARGIN) {
                startNewPage();
            }
        }

        private List<String> wrapText(
                String text,
                PDFont font,
                float fontSize,
                float maxWidth) throws IOException {

            List<String> lines = new ArrayList<>();
            StringBuilder currentLine = new StringBuilder();

            String[] words = text.split("\\s+");

            for (String word : words) {

                if (word.isBlank()) {
                    continue;
                }

                String candidate = currentLine.isEmpty()
                        ? word
                        : currentLine + " " + word;

                float candidateWidth = font.getStringWidth(candidate) * fontSize / 1000;

                if (candidateWidth <= maxWidth) {
                    currentLine.setLength(0);
                    currentLine.append(candidate);
                } else {

                    if (!currentLine.isEmpty()) {
                        lines.add(currentLine.toString());
                    }

                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }

            if (!currentLine.isEmpty()) {
                lines.add(currentLine.toString());
            }

            if (lines.isEmpty()) {
                lines.add(" ");
            }

            return lines;
        }

        /**
         * PDFBox's standard Helvetica font supports a limited character set.
         * Unsupported characters are replaced with '?' in this initial version.
         */
        private String toSupportedText(String text) {

            if (text == null) {
                return "";
            }

            StringBuilder result = new StringBuilder();

            for (int offset = 0; offset < text.length();) {

                int codePoint = text.codePointAt(offset);

                String character = new String(Character.toChars(codePoint));

                if (Character.isISOControl(codePoint)) {
                    result.append(' ');
                } else {
                    try {
                        regularFont.encode(character);
                        result.append(character);
                    } catch (IOException | IllegalArgumentException exception) {
                        result.append('?');
                    }
                }

                offset += Character.charCount(codePoint);
            }

            return result.toString().replaceAll("\\s+", " ").trim();
        }

        @Override
        public void close() throws IOException {

            if (contentStream != null) {
                contentStream.close();
                contentStream = null;
            }
        }
    }
}