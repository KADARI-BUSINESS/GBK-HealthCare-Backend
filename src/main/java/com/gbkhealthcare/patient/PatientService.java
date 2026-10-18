package com.gbkhealthcare.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientVisitRepository patientVisitRepository;

    public PatientService(
            PatientRepository patientRepository,
            PatientVisitRepository patientVisitRepository) {
        this.patientRepository = patientRepository;
        this.patientVisitRepository = patientVisitRepository;
    }

    @Transactional
    public Patient create(Patient patient) {
        patient.setId(null);
        Patient savedPatient = patientRepository.save(patient);

        PatientVisit visit = new PatientVisit();
        visit.setPatient(savedPatient);
        visit.setVisitNumber(1);
        visit.setVisitDate(savedPatient.getVisitDate());
        visit.setChiefComplaints(savedPatient.getChiefComplaints());
        visit.setPresentIllness(savedPatient.getPresentIllness());
        visit.setGeneralExamination(savedPatient.getGeneralExamination());
        visit.setHeartExamination(savedPatient.getHeartExamination());
        visit.setLungsExamination(savedPatient.getLungsExamination());
        visit.setAbdomenExamination(savedPatient.getAbdomenExamination());
        visit.setRectalExamination(savedPatient.getRectalExamination());
        visit.setTemperature(savedPatient.getTemperature());
        visit.setPulseRate(savedPatient.getPulseRate());
        visit.setSpo2(savedPatient.getSpo2());
        visit.setBpSystolic(savedPatient.getBpSystolic());
        visit.setBpDiastolic(savedPatient.getBpDiastolic());
        visit.setDoctor(savedPatient.getDoctor());

        patientVisitRepository.save(visit);

        return savedPatient;
    }

    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Patient> findAllByDoctorAccountId(Long doctorAccountId) {
        return patientRepository
                .findAllByDoctorAccount_IdOrderByVisitDateDesc(
                        doctorAccountId);
    }

    @Transactional(readOnly = true)
    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Patient findByIdForDoctor(
            Long patientId,
            Long doctorAccountId) {

        return patientRepository
                .findByIdAndDoctorAccount_Id(
                        patientId,
                        doctorAccountId)
                .orElseThrow(() -> new PatientNotFoundException(patientId));
    }
}