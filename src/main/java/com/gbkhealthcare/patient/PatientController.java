package com.gbkhealthcare.patient;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {
    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Patient create(@Valid @RequestBody PatientRequest request) {
        Patient patient = request.toPatient();
        return patientService.create(patient);
    }

    @GetMapping
    public List<Patient> findAll() {
        return patientService.findAll();
    }

    @GetMapping("/{id}")
    public Patient findById(@PathVariable Long id) {
        try {
            return patientService.findById(id);
        } catch (PatientNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        }
    }

    public static class PatientRequest {
        @NotBlank
        @Size(max = 120)
        public String fullName;

        @NotNull
        @Min(0)
        @Max(130)
        public Integer age;

        @NotBlank
        @Pattern(regexp = "Male|Female|Other")
        public String gender;

        @NotNull
        public LocalDate visitDate;

        @NotBlank
        @Pattern(regexp = "[6-9][0-9]{9}", message = "Enter a valid 10-digit Indian mobile number.")
        public String phone;

        @Email
        @Size(max = 160)
        public String email;

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

        public String chiefComplaints;
        public String presentIllness;
        public String generalExamination;
        public String heartExamination;
        public String lungsExamination;
        public String abdomenExamination;
        public String rectalExamination;

        @NotBlank
        @Size(max = 100)
        public String doctor;

        Patient toPatient() {
            Patient patient = new Patient();
            patient.setFullName(fullName.trim());
            patient.setAge(age);
            patient.setGender(gender);
            patient.setVisitDate(visitDate);
            patient.setPhone(phone);
            patient.setEmail(blankToNull(email));
            patient.setTemperature(temperature);
            patient.setPulseRate(pulseRate);
            patient.setSpo2(spo2);
            patient.setBpSystolic(bpSystolic);
            patient.setBpDiastolic(bpDiastolic);
            patient.setChiefComplaints(blankToNull(chiefComplaints));
            patient.setPresentIllness(blankToNull(presentIllness));
            patient.setGeneralExamination(blankToNull(generalExamination));
            patient.setHeartExamination(blankToNull(heartExamination));
            patient.setLungsExamination(blankToNull(lungsExamination));
            patient.setAbdomenExamination(blankToNull(abdomenExamination));
            patient.setRectalExamination(blankToNull(rectalExamination));
            patient.setDoctor(doctor.trim());
            return patient;
        }

        private String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value.trim();
        }
    }
}
