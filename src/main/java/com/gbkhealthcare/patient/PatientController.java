package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final DoctorRepository doctorRepository;

    public PatientController(
            PatientService patientService,
            DoctorRepository doctorRepository) {
        this.patientService = patientService;
        this.doctorRepository = doctorRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Patient create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PatientRequest request) {

        Doctor doctor = doctorRepository
                .findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active doctor account not found."));

        Patient patient = request.toPatient();

        // Use the doctor identified by the verified JWT.
        // Do not trust the doctor name submitted by the frontend.
        patient.setDoctorAccount(doctor);
        patient.setDoctor(doctor.getFullName());

        return patientService.create(patient);
    }

    @GetMapping
    public List<Patient> findAll(
            @AuthenticationPrincipal Jwt jwt) {

        Doctor doctor = doctorRepository
                .findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active doctor account not found."));

        return patientService.findAllByDoctorAccountId(doctor.getId());
    }

    @GetMapping("/{id}")
    public Patient findById(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Doctor doctor = doctorRepository
                .findByEmailIgnoreCase(jwt.getSubject())
                .filter(Doctor::isEnabled)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Active doctor account not found."));

        try {
            return patientService.findByIdForDoctor(
                    id,
                    doctor.getId());
        } catch (PatientNotFoundException exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Patient not found.");
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
            return value == null || value.isBlank()
                    ? null
                    : value.trim();
        }
    }
}