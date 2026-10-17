package com.gbkhealthcare.patient;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.mail.javamail.JavaMailSender;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientReportEmailServiceTest {

    private JavaMailSender mailSender;
    private PatientReportEmailService emailService;
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {

        mailSender = mock(JavaMailSender.class);

        emailService = new PatientReportEmailService(
                mailSender,
                "kadari.venkatesh89@gmail.com");

        Session session = Session.getInstance(new Properties());

        mimeMessage = new MimeMessage(session);

        when(mailSender.createMimeMessage())
                .thenReturn(mimeMessage);
    }

    @Test
    void shouldSendClinicalReportAsPdfAttachment() throws Exception {

        // Dummy PDF bytes for unit testing only.
        byte[] pdfBytes = ("%PDF-1.7\nDummy test PDF content").getBytes(StandardCharsets.UTF_8);

        emailService.sendEncryptedClinicalReport(
                "recipient@example.com",
                pdfBytes);

        // Verify that the email was prepared and passed to the mail sender.
        verify(mailSender, times(1)).send(mimeMessage);

        assertEquals(
                "Your GBK HealthCare Clinical Report",
                mimeMessage.getSubject());

        assertEquals(
                "recipient@example.com",
                mimeMessage.getRecipients(Message.RecipientType.TO)[0]
                        .toString());

        // Finalise MIME headers before inspecting the mocked message.
        // A real mail transport normally performs this during sending.
        mimeMessage.saveChanges();

        Object content = mimeMessage.getContent();

        assertInstanceOf(Multipart.class, content);

        Multipart multipart = (Multipart) content;

        assertEquals(2, multipart.getCount());

        boolean attachmentFound = false;

        for (int i = 0; i < multipart.getCount(); i++) {

            Part part = multipart.getBodyPart(i);

            String fileName = part.getFileName();

            if ("GBK-HealthCare-Clinical-Report.pdf".equals(fileName)) {
                attachmentFound = true;

                System.out.println(
                        "Attachment content type: " + part.getContentType());

                assertTrue(
                        part.getContentType().toLowerCase()
                                .contains("application/pdf"),
                        "Unexpected attachment content type: "
                                + part.getContentType());
            }
        }

        assertTrue(
                attachmentFound,
                "Clinical report PDF attachment should be present.");
    }

    @Test
    void shouldRejectInvalidRecipientEmail() {

        byte[] pdfBytes = "%PDF-1.7\nDummy".getBytes(
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> emailService.sendEncryptedClinicalReport(
                        "invalid-email",
                        pdfBytes));

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldRejectInvalidPdfAttachment() {

        byte[] invalidBytes = "This is not a PDF".getBytes(
                StandardCharsets.UTF_8);

        assertThrows(
                IllegalArgumentException.class,
                () -> emailService.sendEncryptedClinicalReport(
                        "recipient@example.com",
                        invalidBytes));

        verifyNoInteractions(mailSender);
    }
}