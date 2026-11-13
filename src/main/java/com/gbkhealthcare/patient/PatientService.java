package com.gbkhealthcare.patient;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class PatientService {

    private static final int CSV_EXPORT_BATCH_SIZE = 500;
    private static final String[] CSV_HEADERS = {
            "id", "fullName", "age", "gender", "visitDate", "phone",
            "email", "doctor", "chiefComplaints"
    };

    private final PatientRepository patientRepository;
    private final PatientVisitRepository patientVisitRepository;
    private final DoctorActivityService activityService;

    public PatientService(
            PatientRepository patientRepository,
            PatientVisitRepository patientVisitRepository,
            DoctorActivityService activityService) {
        this.patientRepository = patientRepository;
        this.patientVisitRepository = patientVisitRepository;
        this.activityService = activityService;
    }

    @Transactional
    public Patient create(Patient patient) {
        if (patient.getDoctorAccount() == null || patient.getDoctorAccount().getId() == null) {
            throw new IllegalStateException("Patient registration requires an authenticated doctor.");
        }
        Long doctorId = patient.getDoctorAccount().getId();
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

        // Registration creates the initial visit as part of this operation. Log only
        // the registration so the generated visit is not misreported as a second action.
        activityService.recordActivity(doctorId,
                DoctorActivity.ActionType.PATIENT_REGISTERED,
                DoctorActivity.EntityType.PATIENT, savedPatient.getId());

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
    public PatientPageResponse findPageByDoctorAccountId(
            Long doctorAccountId,
            int page,
            int size,
            String search,
            String gender) {

        String normalizedSearch = search == null || search.isBlank()
                ? null
                : search.trim();
        String normalizedGender = gender == null || gender.isBlank()
                ? null
                : gender;
        PageRequest pageable = PageRequest.of(page, size);
        Page<Patient> result = patientRepository.findPageByDoctorAndFilters(
                doctorAccountId,
                normalizedSearch,
                normalizedGender,
                pageable);

        return PatientPageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public void writeCsvForDoctor(
            Long doctorAccountId,
            String search,
            String gender,
            OutputStream outputStream) throws IOException {
        String normalizedSearch = search == null || search.isBlank()
                ? null
                : search.trim();
        String normalizedGender = gender == null || gender.isBlank()
                ? null
                : gender;

        outputStream.write(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        writeCsvLine(outputStream, CSV_HEADERS);

        long afterId = 0L;
        List<PatientCsvRow> batch;
        do {
            batch = patientRepository.findCsvExportBatch(
                    doctorAccountId, afterId, normalizedSearch, normalizedGender);
            for (PatientCsvRow row : batch) {
                writeCsvLine(outputStream, new String[] {
                        value(row.getId()), safeText(row.getFullName()), value(row.getAge()),
                        safeText(row.getGender()), value(row.getVisitDate()),
                        safeText(row.getPhone()), safeText(row.getEmail()),
                        safeText(row.getDoctor()), safeText(row.getChiefComplaints())
                });
                afterId = row.getId();
            }
            outputStream.flush();
        } while (batch.size() == CSV_EXPORT_BATCH_SIZE);
    }

    private static String value(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String safeText(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int index = 0;
        while (index < value.length()
                && (Character.isWhitespace(value.charAt(index))
                    || Character.isISOControl(value.charAt(index)))) {
            index++;
        }
        if (index < value.length()) {
            char first = value.charAt(index);
            if (first == '=' || first == '+' || first == '-' || first == '@'
                    || first == '\uFF1D' || first == '\uFF0B'
                    || first == '\uFF0D' || first == '\uFF20') {
                return "'" + value;
            }
        }
        if (Character.isWhitespace(value.charAt(0))
                || Character.isISOControl(value.charAt(0))) {
            return "'" + value;
        }
        return value;
    }

    private static void writeCsvLine(OutputStream outputStream, String[] fields)
            throws IOException {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append('"').append(fields[i] == null ? "" : fields[i].replace("\"", "\"\""))
                    .append('"');
        }
        line.append("\r\n");
        outputStream.write(line.toString().getBytes(StandardCharsets.UTF_8));
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
