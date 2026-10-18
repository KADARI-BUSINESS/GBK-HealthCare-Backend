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

import java.awt.Color;
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
        writer.addMetricCards(
                new String[] { "Temperature", "Pulse Rate", "SpO2", "Blood Pressure" },
                new Object[] {
                        withUnit(visit.getTemperature(), " °C"),
                        withUnit(visit.getPulseRate(), " bpm"),
                        withUnit(visit.getSpo2(), " %"),
                        bloodPressure(visit)
                });
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

        private static final float MARGIN = 44f;
        private static final float BODY_FONT_SIZE = 9.5f;
        private static final float LINE_HEIGHT = 12.5f;
        private static final float FOOTER_RESERVE = 48f;

        private static final Color NAVY = new Color(29, 39, 85);
        private static final Color INDIGO = new Color(75, 79, 184);
        private static final Color TEAL = new Color(22, 157, 157);
        private static final Color PANEL = new Color(245, 247, 252);
        private static final Color PALE_INDIGO = new Color(239, 241, 252);
        private static final Color BORDER = new Color(224, 229, 239);
        private static final Color TEXT = new Color(42, 49, 67);
        private static final Color MUTED = new Color(104, 113, 132);
        private static final Color WHITE = Color.WHITE;

        private final PDDocument document;
        private PDPageContentStream contentStream;
        private float yPosition;

        private ReportWriter(PDDocument document) throws IOException {
            this.document = document;
            startNewPage();
        }

        private float pageWidth() {
            return PDRectangle.A4.getWidth();
        }

        private float contentWidth() {
            return pageWidth() - (2 * MARGIN);
        }

        private void startNewPage() throws IOException {
            if (contentStream != null) {
                drawFooter();
                contentStream.close();
            }

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            contentStream = new PDPageContentStream(document, page);

            float pageHeight = page.getMediaBox().getHeight();
            float headerHeight = 120f;
            float headerBottom = pageHeight - headerHeight;

            // Deep indigo header and teal accent create a consistent brand bar.
            contentStream.setNonStrokingColor(NAVY);
            contentStream.addRect(0, headerBottom, pageWidth(), headerHeight);
            contentStream.fill();

            contentStream.setNonStrokingColor(TEAL);
            contentStream.addRect(0, headerBottom, pageWidth(), 5f);
            contentStream.fill();

            drawTextAt("GBK HealthCare", boldFont, 20f,
                    MARGIN, pageHeight - 42f, WHITE);
            drawTextAt("PATIENT CLINICAL REPORT", boldFont, 10f,
                    MARGIN, pageHeight - 64f, new Color(225, 231, 255));
            drawTextAt("Clinical visit summary | Confidential health information",
                    regularFont, 8f, MARGIN, pageHeight - 82f,
                    new Color(219, 225, 245));

            // Confidentiality badge on the right side of the header.
            float badgeWidth = 96f;
            float badgeHeight = 22f;
            float badgeX = pageWidth() - MARGIN - badgeWidth;
            float badgeY = pageHeight - 52f;
            contentStream.setNonStrokingColor(new Color(58, 72, 132));
            contentStream.addRect(badgeX, badgeY, badgeWidth, badgeHeight);
            contentStream.fill();
            drawTextAt("CONFIDENTIAL", boldFont, 7.5f,
                    badgeX + 12f, badgeY + 8f, WHITE);

            yPosition = headerBottom - 25f;
        }

        private void addSection(String title) throws IOException {
            ensureSpace(34f);

            float boxHeight = 23f;
            float boxY = yPosition - boxHeight + 4f;

            contentStream.setNonStrokingColor(PALE_INDIGO);
            contentStream.addRect(MARGIN, boxY, contentWidth(), boxHeight);
            contentStream.fill();

            contentStream.setNonStrokingColor(TEAL);
            contentStream.addRect(MARGIN, boxY, 4f, boxHeight);
            contentStream.fill();

            drawTextAt(title, boldFont, 10.5f,
                    MARGIN + 12f, boxY + 7.5f, NAVY);
            yPosition = boxY - 13f;
        }

        private void addField(String label, Object value) throws IOException {
            String displayValue = displayValue(value);
            List<String> lines = wrapText(
                    toSupportedText(displayValue),
                    regularFont,
                    BODY_FONT_SIZE,
                    contentWidth() - 18f);

            float required = 15f + (lines.size() * LINE_HEIGHT) + 8f;
            ensureSpace(required);

            float labelX = MARGIN + 8f;
            drawTextAt(toSupportedText(label), boldFont, 8f,
                    labelX, yPosition, MUTED);

            float valueY = yPosition - 13f;
            Color valueColor = "Not recorded".equals(displayValue) ? MUTED : TEXT;
            for (String line : lines) {
                drawTextAt(line, regularFont, BODY_FONT_SIZE,
                        labelX, valueY, valueColor);
                valueY -= LINE_HEIGHT;
            }

            float separatorY = valueY + 4f;
            contentStream.setStrokingColor(BORDER);
            contentStream.setLineWidth(0.5f);
            contentStream.moveTo(MARGIN + 7f, separatorY);
            contentStream.lineTo(pageWidth() - MARGIN - 7f, separatorY);
            contentStream.stroke();

            yPosition = valueY - 5f;
        }

        private void addMetricCards(String[] labels, Object[] values) throws IOException {
            if (labels == null || values == null || labels.length == 0
                    || labels.length != values.length) {
                throw new IllegalArgumentException("Metric labels and values must match.");
            }

            float cardHeight = 54f;
            float gap = 7f;
            float cardWidth = (contentWidth() - ((labels.length - 1) * gap)) / labels.length;
            ensureSpace(cardHeight + 16f);

            float cardY = yPosition - cardHeight + 2f;
            for (int i = 0; i < labels.length; i++) {
                float cardX = MARGIN + i * (cardWidth + gap);

                contentStream.setNonStrokingColor(PANEL);
                contentStream.addRect(cardX, cardY, cardWidth, cardHeight);
                contentStream.fill();

                contentStream.setStrokingColor(BORDER);
                contentStream.setLineWidth(0.6f);
                contentStream.addRect(cardX, cardY, cardWidth, cardHeight);
                contentStream.stroke();

                contentStream.setNonStrokingColor(TEAL);
                contentStream.addRect(cardX, cardY + cardHeight - 3f, cardWidth, 3f);
                contentStream.fill();

                drawTextAt(toSupportedText(labels[i]), boldFont, 7.2f,
                        cardX + 7f, cardY + cardHeight - 17f, MUTED);

                String metricValue = toSupportedText(displayValue(values[i]));
                float valueFontSize = metricValue.length() > 13 ? 8f : 10f;
                drawTextAt(metricValue, boldFont, valueFontSize,
                        cardX + 7f, cardY + 17f, INDIGO);
            }

            yPosition = cardY - 16f;
        }

        private String displayValue(Object value) {
            if (value == null || value.toString().isBlank()) {
                return "Not recorded";
            }
            return value.toString().trim();
        }

        private void addParagraph(String text) throws IOException {
            String cleanedText = toSupportedText(text);
            List<String> lines = wrapText(
                    cleanedText,
                    regularFont,
                    8.5f,
                    contentWidth() - 20f);

            float boxHeight = 16f + (lines.size() * 11f);
            ensureSpace(boxHeight + 8f);
            float boxY = yPosition - boxHeight + 4f;

            contentStream.setNonStrokingColor(PANEL);
            contentStream.addRect(MARGIN, boxY, contentWidth(), boxHeight);
            contentStream.fill();
            contentStream.setNonStrokingColor(TEAL);
            contentStream.addRect(MARGIN, boxY, 3f, boxHeight);
            contentStream.fill();

            float textY = boxY + boxHeight - 13f;
            for (String line : lines) {
                drawTextAt(line, regularFont, 8.5f,
                        MARGIN + 10f, textY, MUTED);
                textY -= 11f;
            }
            yPosition = boxY - 12f;
        }

        private void drawFooter() throws IOException {
            if (contentStream == null) {
                return;
            }

            contentStream.setStrokingColor(BORDER);
            contentStream.setLineWidth(0.6f);
            contentStream.moveTo(MARGIN, 34f);
            contentStream.lineTo(pageWidth() - MARGIN, 34f);
            contentStream.stroke();

            drawTextAt("GBK HealthCare | Confidential clinical information",
                    regularFont, 7f, MARGIN, 20f, MUTED);
            String pageLabel = "Page " + document.getNumberOfPages();
            float pageLabelWidth = boldFont.getStringWidth(pageLabel) * 7f / 1000f;
            drawTextAt(pageLabel, boldFont, 7f,
                    pageWidth() - MARGIN - pageLabelWidth, 20f, MUTED);
        }

        private void ensureSpace(float requiredSpace) throws IOException {
            if (yPosition - requiredSpace < MARGIN + FOOTER_RESERVE) {
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
                float candidateWidth = font.getStringWidth(candidate) * fontSize / 1000f;

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

        private void drawTextAt(
                String text,
                PDFont font,
                float fontSize,
                float x,
                float y,
                Color color) throws IOException {
            contentStream.setNonStrokingColor(color);
            contentStream.beginText();
            contentStream.setFont(font, fontSize);
            contentStream.newLineAtOffset(x, y);
            contentStream.showText(toSupportedText(text));
            contentStream.endText();
        }

        @Override
        public void close() throws IOException {
            if (contentStream != null) {
                drawFooter();
                contentStream.close();
                contentStream = null;
            }
        }
    }

}