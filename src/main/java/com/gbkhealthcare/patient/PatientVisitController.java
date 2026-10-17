
package com.gbkhealthcare.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PatientVisitController {

    private final PatientVisitService patientVisitService;
    private final DoctorRepository doctorRepository;

    public PatientVisitController(
            PatientVisitService patientVisitService,
            DoctorRepository doctorRepository) {
        this.patientVisitService = patientVisitService;
        this.doctorRepository = doctorRepository;
    }

    @PostMapping("/patients/{patientId}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public VisitResponse createVisit(
            @PathVariable Long patientId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody VisitRequest request) {

        Doctor doctor = getAuthenticatedDoctor(jwt);

        try {
            PatientVisit visit = patientVisitService.createVisitForDoctor(
                    patientId,
                    doctor.getId(),
                    request.toPatientVisit());

            return VisitResponse.from(visit);
        } catch (PatientNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Patient not found.");
        }
    }

    @GetMapping("/patients/{patientId}/visits")
    public List<VisitResponse> getPatientVisits(
            @PathVariable Long patientId) {
        try {
            return patientVisitService
                    .getVisitsByPatientId(patientId)
                    .stream()
                    .map(VisitResponse::from)
                    .toList();
        } catch (PatientNotFoundException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @GetMapping("/visits/{visitId}")
    public VisitResponse getVisit(
            @PathVariable Long visitId,
            @AuthenticationPrincipal Jwt jwt) {

        Doctor doctor = getAuthenticatedDoctor(jwt);

        try {
            return VisitResponse.from(
                    patientVisitService.getVisitByIdForDoctor(
                            visitId, doctor.getId()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Visit not found.");
        }
    }

    // Update an existing visit's follow-up details
    @PutMapping("/visits/{visitId}/follow-up")
    public VisitResponse updateFollowUp(
            @PathVariable Long visitId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody FollowUpRequest request) {

        Doctor doctor = getAuthenticatedDoctor(jwt);

        try {
            PatientVisit visit = patientVisitService.updateFollowUpForDoctor(
                    visitId,
                    doctor.getId(),
                    request.followUpDate,
                    request.followUpNotes);

            return VisitResponse.from(visit);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Visit not found.");
        }
    }

    public record VisitResponse(
            Long id,
            Integer visitNumber,
            Long patientId,
            LocalDate visitDate,
            LocalDate followUpDate,
            String followUpNotes,
            String chiefComplaints,
            String presentIllness,
            String generalExamination,
            String heartExamination,
            String lungsExamination,
            String abdomenExamination,
            String rectalExamination,
            Double temperature,
            Integer pulseRate,
            Integer spo2,
            Integer bpSystolic,
            Integer bpDiastolic,
            String doctor,
            String diagnosis,
            String treatmentPlan,
            LocalDateTime createdAt) {

        public static VisitResponse from(PatientVisit visit) {
            return new VisitResponse(
                    visit.getId(),
                    visit.getVisitNumber(),
                    visit.getPatient().getId(),
                    visit.getVisitDate(),
                    visit.getFollowUpDate(),
                    visit.getFollowUpNotes(),
                    visit.getChiefComplaints(),
                    visit.getPresentIllness(),
                    visit.getGeneralExamination(),
                    visit.getHeartExamination(),
                    visit.getLungsExamination(),
                    visit.getAbdomenExamination(),
                    visit.getRectalExamination(),
                    visit.getTemperature(),
                    visit.getPulseRate(),
                    visit.getSpo2(),
                    visit.getBpSystolic(),
                    visit.getBpDiastolic(),
                    visit.getDoctor(),
                    visit.getDiagnosis(),
                    visit.getTreatmentPlan(),
                    visit.getCreatedAt());
        }
    }

    public static class VisitRequest {

        @NotNull
        public LocalDate visitDate;

        public LocalDate followUpDate;

        @Size(max = 1000)
        public String chiefComplaints;

        public String presentIllness;
        public String generalExamination;
        public String heartExamination;
        public String lungsExamination;
        public String abdomenExamination;
        public String rectalExamination;

        @DecimalMin("80.0")
        @DecimalMax("120.0")
        public Double temperature;

        @Min(0)
        @Max(300)
        public Integer pulseRate;

        @Min(0)
        @Max(100)
        public Integer spo2;

        @Min(0)
        @Max(350)
        public Integer bpSystolic;

        @Min(0)
        @Max(250)
        public Integer bpDiastolic;

        @Size(max = 100)
        public String doctor;

        public String diagnosis;
        public String treatmentPlan;

        public PatientVisit toPatientVisit() {
            PatientVisit visit = new PatientVisit();

            visit.setVisitDate(visitDate);
            visit.setFollowUpDate(followUpDate);
            visit.setFollowUpNotes(null);
            visit.setChiefComplaints(chiefComplaints);
            visit.setPresentIllness(presentIllness);
            visit.setGeneralExamination(generalExamination);
            visit.setHeartExamination(heartExamination);
            visit.setLungsExamination(lungsExamination);
            visit.setAbdomenExamination(abdomenExamination);
            visit.setRectalExamination(rectalExamination);
            visit.setTemperature(temperature);
            visit.setPulseRate(pulseRate);
            visit.setSpo2(spo2);
            visit.setBpSystolic(bpSystolic);
            visit.setBpDiastolic(bpDiastolic);
            visit.setDoctor(doctor);
            visit.setDiagnosis(diagnosis);
            visit.setTreatmentPlan(treatmentPlan);

            return visit;
        }
    }

    public static class FollowUpRequest {

        @NotNull
        public LocalDate followUpDate;

        @Size(max = 2000)
        public String followUpNotes;
    }

    private Doctor getAuthenticatedDoctor(Jwt jwt) {
        return doctorRepository
                .findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active doctor account not found."));
    }
}
