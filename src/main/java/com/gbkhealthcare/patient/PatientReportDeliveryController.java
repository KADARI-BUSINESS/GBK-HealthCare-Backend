package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PatientReportDeliveryController {

    private final PatientReportDeliveryService deliveryService;
    private final DoctorRepository doctorRepository;

    public PatientReportDeliveryController(
            PatientReportDeliveryService deliveryService,
            DoctorRepository doctorRepository) {
        this.deliveryService = deliveryService;
        this.doctorRepository = doctorRepository;
    }

    private Doctor getAuthenticatedDoctor(Jwt jwt) {
        return doctorRepository
                .findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active doctor account not found."));
    }

    @PostMapping("/api/patients/{patientId}/visits/{visitId}/report-deliveries")
    public ResponseEntity<PatientReportDeliveryService.DeliveryResult> requestDelivery(
            @PathVariable Long patientId,
            @PathVariable Long visitId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ConsentRequest request) {

        Doctor doctor = getAuthenticatedDoctor(jwt);

        try {
            var result = deliveryService.requestDeliveryForDoctor(
                    patientId,
                    visitId,
                    request.consentGiven(),
                    doctor.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(result);

        } catch (PatientNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Patient not found.");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    @PostMapping("/api/report-deliveries/{deliveryId}/send")
    public PatientReportDeliveryService.DeliveryResult sendReport(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SendReportRequest request) {

        Doctor doctor = getAuthenticatedDoctor(jwt);

        try {
            return deliveryService.sendPendingReportForDoctor(
                    deliveryId,
                    request.pdfPassword(),
                    doctor.getId());

        } catch (PatientNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Report delivery not found.");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Report delivery not found.");
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    @GetMapping("/api/report-deliveries/{deliveryId}/status")
    public PatientReportDeliveryService.DeliveryResult getStatus(
            @PathVariable Long deliveryId,
            @AuthenticationPrincipal Jwt jwt) {

        Doctor doctor = getAuthenticatedDoctor(jwt);

        try {
            return deliveryService.getStatusForDoctor(
                    deliveryId, doctor.getId());

        } catch (PatientNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Report delivery not found.");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Report delivery not found.");
        }
    }

    public record ConsentRequest(
            @NotNull Boolean consentGiven) {
    }

    public record SendReportRequest(
            @NotBlank @Size(min = 12, max = 128) String pdfPassword) {
    }
}