
package com.gbkhealthcare.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.time.LocalDate;
import java.util.List;

@Service
public class PatientVisitService {

        private final PatientVisitRepository patientVisitRepository;
        private final PatientRepository patientRepository;

        public PatientVisitService(
                        PatientVisitRepository patientVisitRepository,
                        PatientRepository patientRepository) {
                this.patientVisitRepository = patientVisitRepository;
                this.patientRepository = patientRepository;
        }

        @Transactional
        public PatientVisit createVisit(Long patientId, PatientVisit visit) {
                // Lock this patient's row while assigning the next visit number.
                Patient patient = patientRepository.findByIdForUpdate(patientId)
                                .orElseThrow(() -> new PatientNotFoundException(
                                                "Patient with ID " + patientId + " was not found."));

                Integer currentMax = patientVisitRepository.findMaxVisitNumberByPatientId(patientId);

                int nextVisitNumber = (currentMax == null ? 0 : currentMax) + 1;

                visit.setId(null);
                visit.setPatient(patient);
                visit.setVisitNumber(nextVisitNumber);

                return patientVisitRepository.save(visit);
        }

        @Transactional(readOnly = true)
        public List<PatientVisit> getVisitsByPatientId(Long patientId) {
                if (!patientRepository.existsById(patientId)) {
                        throw new PatientNotFoundException(
                                        "Patient with ID " + patientId + " was not found.");
                }

                return patientVisitRepository
                                .findByPatient_IdOrderByVisitDateDesc(patientId);
        }

        @Transactional(readOnly = true)
        public PatientVisit getVisitById(Long visitId) {
                return patientVisitRepository.findById(visitId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Visit with ID " + visitId + " was not found."));
        }

        @Transactional
        public PatientVisit updateFollowUp(
                        Long visitId,
                        LocalDate followUpDate,
                        String followUpNotes) {

                PatientVisit visit = patientVisitRepository.findById(visitId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Visit with ID " + visitId + " was not found."));

                visit.setFollowUpDate(followUpDate);
                visit.setFollowUpNotes(
                                followUpNotes == null || followUpNotes.isBlank()
                                                ? null
                                                : followUpNotes.trim());

                return patientVisitRepository.save(visit);
        }

        @Transactional
        public PatientVisit createVisitForDoctor(
                        Long patientId,
                        Long doctorAccountId,
                        PatientVisit visit) {

                Patient patient = patientRepository.findByIdForUpdate(patientId)
                                .orElseThrow(() -> new PatientNotFoundException(patientId));

                if (!isOwnedByDoctor(patient, doctorAccountId)) {
                        throw new PatientNotFoundException(patientId);
                }

                Integer currentMax = patientVisitRepository.findMaxVisitNumberByPatientId(patientId);

                int nextVisitNumber = (currentMax == null ? 0 : currentMax) + 1;

                visit.setId(null);
                visit.setPatient(patient);
                visit.setVisitNumber(nextVisitNumber);

                // Do not trust the doctor name supplied by the request.
                visit.setDoctor(patient.getDoctorAccount().getFullName());

                return patientVisitRepository.save(visit);
        }

        @Transactional(readOnly = true)
        public List<PatientVisit> getVisitsByPatientIdForDoctor(
                        Long patientId,
                        Long doctorAccountId) {

                Patient patient = patientRepository
                                .findByIdAndDoctorAccount_Id(patientId, doctorAccountId)
                                .orElseThrow(() -> new PatientNotFoundException(patientId));

                return patientVisitRepository
                                .findByPatient_IdOrderByVisitDateDesc(patient.getId());
        }

        @Transactional(readOnly = true)
        public PatientVisit getVisitByIdForDoctor(
                        Long visitId,
                        Long doctorAccountId) {

                PatientVisit visit = patientVisitRepository.findById(visitId)
                                .orElseThrow(() -> new IllegalArgumentException("Visit not found."));

                if (!isOwnedByDoctor(visit.getPatient(), doctorAccountId)) {
                        throw new IllegalArgumentException("Visit not found.");
                }

                return visit;
        }

        @Transactional
        public PatientVisit updateFollowUpForDoctor(
                        Long visitId,
                        Long doctorAccountId,
                        LocalDate followUpDate,
                        String followUpNotes) {

                PatientVisit visit = patientVisitRepository.findById(visitId)
                                .orElseThrow(() -> new IllegalArgumentException("Visit not found."));

                if (!isOwnedByDoctor(visit.getPatient(), doctorAccountId)) {
                        throw new IllegalArgumentException("Visit not found.");
                }

                visit.setFollowUpDate(followUpDate);
                visit.setFollowUpNotes(
                                followUpNotes == null || followUpNotes.isBlank()
                                                ? null
                                                : followUpNotes.trim());

                return patientVisitRepository.save(visit);
        }

        private boolean isOwnedByDoctor(
                        Patient patient,
                        Long doctorAccountId) {

                return patient.getDoctorAccount() != null
                                && Objects.equals(
                                                patient.getDoctorAccount().getId(),
                                                doctorAccountId);
        }
}
