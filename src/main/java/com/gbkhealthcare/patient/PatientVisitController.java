
package com.gbkhealthcare.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class PatientVisitController {

    private final PatientVisitService patientVisitService;

    public PatientVisitController(
            PatientVisitService patientVisitService) {
        this.patientVisitService = patientVisitService;
    }

    @PostMapping("/patients/{patientId}/visits")
    @ResponseStatus(HttpStatus.CREATED)
    public VisitResponse createVisit(
            @PathVariable Long patientId,
            @Valid @RequestBody VisitRequest request) {
        try {
            PatientVisit visit = patientVisitService.createVisit(
                    patientId, request.toPatientVisit());
            return VisitResponse.from(visit);
        } catch (PatientNotFoundException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, e.getMessage());
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
    public VisitResponse getVisit(@PathVariable Long visitId) {
        try {
            return VisitResponse.from(
                    patientVisitService.getVisitById(visitId));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    // Update an existing visit's follow-up details
    @PutMapping("/visits/{visitId}/follow-up")
    public VisitResponse updateFollowUp(
            @PathVariable Long visitId,
            @Valid @RequestBody FollowUpRequest request) {
        try {
            PatientVisit visit = patientVisitService.updateFollowUp(
                    visitId,
                    request.followUpDate,
                    request.followUpNotes);

            return VisitResponse.from(visit);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, e.getMessage());
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
}
