package com.gbkhealthcare.patient;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Service
public class PatientReportEmailService {

    private static final String ATTACHMENT_NAME = "GBK-HealthCare-Clinical-Report.pdf";

    private final JavaMailSender mailSender;
    private final String senderEmail;

    public PatientReportEmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String senderEmail) {

        this.mailSender = mailSender;
        this.senderEmail = senderEmail;
    }

    /**
     * Sends a password-protected clinical report PDF.
     *
     * The PDF password must be communicated separately through
     * a verified channel. It must never be included in this email.
     *
     * This method sends only the email. Patient consent, report ownership,
     * delivery status and retry handling will be enforced by the
     * application service/controller that calls it.
     */
    public void sendEncryptedClinicalReport(
            String recipientEmail,
            byte[] encryptedPdf) {

        validateInputs(recipientEmail, encryptedPdf);

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(senderEmail.trim());
            helper.setTo(recipientEmail.trim());

            // Avoid putting patient names or medical details in the subject.
            helper.setSubject(
                    "Your GBK HealthCare Clinical Report");

            helper.setText("""
                    Dear Patient,

                    Your requested GBK HealthCare clinical report is attached
                    as a password-protected PDF.

                    For your protection, the PDF password is not included
                    in this email. Please obtain it through the separate,
                    verified channel arranged by your clinic.

                    If you did not request this report, do not forward
                    the attachment and contact your healthcare provider.

                    Regards,
                    GBK HealthCare
                    """);

            helper.addAttachment(
                    ATTACHMENT_NAME,
                    new ByteArrayResource(encryptedPdf),
                    "application/pdf");

            mailSender.send(message);

        } catch (MessagingException exception) {
            throw new IllegalStateException(
                    "Unable to prepare the clinical report email.",
                    exception);
        } catch (MailException exception) {
            // Allow the calling service to record the failure and
            // implement retry handling without losing the patient record.
            throw exception;
        }
    }

    private void validateInputs(
            String recipientEmail,
            byte[] encryptedPdf) {

        if (senderEmail == null || senderEmail.isBlank()) {
            throw new IllegalStateException(
                    "The sender email is not configured.");
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "A recipient email address is required.");
        }

        try {
            InternetAddress address = new InternetAddress(recipientEmail.trim());

            address.validate();

        } catch (jakarta.mail.internet.AddressException exception) {
            throw new IllegalArgumentException(
                    "The recipient email address is invalid.",
                    exception);
        }

        Objects.requireNonNull(
                encryptedPdf,
                "The encrypted PDF must not be null.");

        if (encryptedPdf.length < 8
                || encryptedPdf[0] != '%'
                || encryptedPdf[1] != 'P'
                || encryptedPdf[2] != 'D'
                || encryptedPdf[3] != 'F'
                || encryptedPdf[4] != '-') {

            throw new IllegalArgumentException(
                    "The attachment is not a valid PDF file.");
        }
    }
}