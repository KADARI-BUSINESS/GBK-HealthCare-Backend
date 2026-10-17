package com.gbkhealthcare.patient;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.mail.MailException;

@Service
public class PatientReportDeliveryService {

    private static final int MAX_ATTEMPTS = 3;

    private final PatientReportDeliveryRepository deliveryRepository;
    private final PatientRepository patientRepository;
    private final PatientVisitRepository visitRepository;
    private final ClinicalReportPdfService pdfService;
    private final PatientReportEmailService emailService;

    public PatientReportDeliveryService(
            PatientReportDeliveryRepository deliveryRepository,
            PatientRepository patientRepository,
            PatientVisitRepository visitRepository,
            ClinicalReportPdfService pdfService,
            PatientReportEmailService emailService) {

        this.deliveryRepository = deliveryRepository;
        this.patientRepository = patientRepository;
        this.visitRepository = visitRepository;
        this.pdfService = pdfService;
        this.emailService = emailService;
    }

    /**
     * Creates a pending report-delivery record after validating consent,
     * patient, visit ownership and the stored recipient email address.
     *
     * The email address is read from the saved Patient entity.
     */
    public DeliveryResult requestDelivery(
            Long patientId,
            Long visitId,
            boolean consentGiven) {

        if (!consentGiven) {
            throw new IllegalArgumentException(
                    "Patient consent is required to email a clinical report.");
        }

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(
                        () -> new PatientNotFoundException(patientId));

        PatientVisit visit = visitRepository.findById(visitId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Visit with ID " + visitId + " was not found."));

        if (visit.getPatient() == null
                || !Objects.equals(
                        patient.getId(),
                        visit.getPatient().getId())) {

            throw new IllegalArgumentException(
                    "The selected visit does not belong to this patient.");
        }

        String recipientEmail = patient.getEmail();

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "A saved patient email address is required.");
        }

        PatientReportDelivery delivery = new PatientReportDelivery();

        delivery.setPatient(patient);
        delivery.setVisit(visit);
        delivery.setRecipientEmail(recipientEmail.trim());
        delivery.setConsentGiven(true);
        delivery.setConsentedAt(LocalDateTime.now());
        delivery.setStatus(PatientReportDelivery.Status.PENDING);
        delivery.setAttemptCount(0);

        PatientReportDelivery saved = deliveryRepository.save(delivery);

        return DeliveryResult.from(saved);
    }

    /**
     * Sends a pending report or retries a previously failed delivery.
     *
     * The PDF password is accepted only for this invocation and is
     * never stored in the delivery entity or included in the email.
     *
     * The caller must deliver that password separately through a
     * verified channel.
     */
    public DeliveryResult sendPendingReport(
            Long deliveryId,
            String pdfPassword) {

        if (pdfPassword == null || pdfPassword.length() < 12) {
            throw new IllegalArgumentException(
                    "The PDF password must contain at least 12 characters.");
        }

        PatientReportDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Report delivery with ID "
                                        + deliveryId
                                        + " was not found."));

        if (!delivery.isConsentGiven()) {
            throw new IllegalStateException(
                    "Patient consent is required before sending a report.");
        }

        if (delivery.getStatus() == PatientReportDelivery.Status.SENT) {
            throw new IllegalStateException(
                    "This report has already been sent.");
        }

        if (delivery.getStatus() == PatientReportDelivery.Status.SENDING) {
            throw new IllegalStateException(
                    "This report is already being sent.");
        }

        if (delivery.getStatus() != PatientReportDelivery.Status.PENDING
                && delivery.getStatus() != PatientReportDelivery.Status.FAILED) {

            throw new IllegalStateException(
                    "This report is not eligible for delivery.");
        }

        if (delivery.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new IllegalStateException(
                    "Maximum email attempts reached for this report.");
        }

        Long patientId = delivery.getPatient().getId();
        Long visitId = delivery.getVisit().getId();

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(
                        () -> new PatientNotFoundException(patientId));

        PatientVisit visit = visitRepository.findById(visitId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Visit with ID " + visitId + " was not found."));

        if (visit.getPatient() == null
                || !Objects.equals(patient.getId(), visit.getPatient().getId())) {

            throw new IllegalStateException(
                    "The report visit no longer matches its patient.");
        }

        // Do not silently send a report if the patient's stored email
        // has changed since consent was recorded.
        if (patient.getEmail() == null
                || !patient.getEmail().trim()
                        .equalsIgnoreCase(delivery.getRecipientEmail())) {

            throw new IllegalStateException(
                    "The patient's email has changed. Obtain consent again.");
        }

        // Persist the attempt before contacting the external mail provider.
        delivery.setStatus(PatientReportDelivery.Status.SENDING);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastAttemptAt(LocalDateTime.now());
        delivery.setFailureCode(null);

        deliveryRepository.save(delivery);

        final byte[] encryptedPdf;

        try {
            encryptedPdf = pdfService.generateEncryptedClinicalReport(
                    patient,
                    visit,
                    pdfPassword);
        } catch (RuntimeException exception) {
            return markFailed(delivery, "PDF_GENERATION_FAILED");
        }

        try {
            emailService.sendEncryptedClinicalReport(
                    delivery.getRecipientEmail(),
                    encryptedPdf);

            delivery.setStatus(PatientReportDelivery.Status.SENT);
            delivery.setSentAt(LocalDateTime.now());
            delivery.setFailureCode(null);

            return DeliveryResult.from(deliveryRepository.save(delivery));

        } catch (MailException exception) {
            return markFailed(delivery, "EMAIL_SEND_FAILED");

        } catch (RuntimeException exception) {
            return markFailed(delivery, "EMAIL_PREPARATION_FAILED");
        }
    }

    private DeliveryResult markFailed(
            PatientReportDelivery delivery,
            String failureCode) {

        delivery.setStatus(PatientReportDelivery.Status.FAILED);
        delivery.setFailureCode(failureCode);

        return DeliveryResult.from(deliveryRepository.save(delivery));
    }

    /**
     * A safe response model that does not expose patient or visit entities.
     */
    public record DeliveryResult(
            Long deliveryId,
            PatientReportDelivery.Status status,
            int attemptCount,
            LocalDateTime createdAt,
            LocalDateTime lastAttemptAt,
            LocalDateTime sentAt,
            String failureCode) {

        public static DeliveryResult from(
                PatientReportDelivery delivery) {

            return new DeliveryResult(
                    delivery.getId(),
                    delivery.getStatus(),
                    delivery.getAttemptCount(),
                    delivery.getCreatedAt(),
                    delivery.getLastAttemptAt(),
                    delivery.getSentAt(),
                    delivery.getFailureCode());
        }
    }
}