package com.gbkhealthcare.patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PatientCsvExportTest {

    private PatientRepository repository;
    private PatientService service;

    @BeforeEach
    void setUp() {
        repository = mock(PatientRepository.class);
        service = new PatientService(repository, mock(PatientVisitRepository.class));
    }

    @Test
    void exportsAllMatchingRecordsInBatchesAndScopesToDoctor() throws Exception {
        List<PatientCsvRow> firstBatch = new ArrayList<>();
        for (long id = 1; id <= 500; id++) {
            firstBatch.add(row(id, "Patient " + id, null, "Male", null,
                    null, null, null, null));
        }
        when(repository.findCsvExportBatch(17L, 0L, null, null)).thenReturn(firstBatch);
        when(repository.findCsvExportBatch(17L, 500L, null, null))
                .thenReturn(List.of(row(501L, "Last", 42, "Female",
                        LocalDate.of(2026, 1, 2), "9876543210", "a@example.test",
                        "Doctor", "Complaint")));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        service.writeCsvForDoctor(17L, null, null, output);

        String csv = new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF\"id\",\"fullName\",\"age\",\"gender\",\"visitDate\",\"phone\",\"email\",\"doctor\",\"chiefComplaints\"\r\n"));
        assertTrue(csv.contains("\"501\",\"Last\",\"42\",\"Female\",\"2026-01-02\",\"9876543210\",\"a@example.test\",\"Doctor\",\"Complaint\"\r\n"));
        assertEquals(501, csv.split("\r\n").length - 1);
        verify(repository).findCsvExportBatch(17L, 0L, null, null);
        verify(repository).findCsvExportBatch(17L, 500L, null, null);
    }

    @Test
    void appliesSearchGenderAndDoctorFiltersToExportQuery() throws Exception {
        when(repository.findCsvExportBatch(9L, 0L, "Sam", "Female"))
                .thenReturn(List.of());

        service.writeCsvForDoctor(9L, " Sam ", "Female", new ByteArrayOutputStream());

        verify(repository).findCsvExportBatch(9L, 0L, "Sam", "Female");
    }

    @Test
    void appliesSearchOnlyToExportQuery() throws Exception {
        when(repository.findCsvExportBatch(9L, 0L, "Sam", null)).thenReturn(List.of());

        service.writeCsvForDoctor(9L, "Sam", null, new ByteArrayOutputStream());

        verify(repository).findCsvExportBatch(9L, 0L, "Sam", null);
    }

    @Test
    void appliesGenderOnlyToExportQuery() throws Exception {
        when(repository.findCsvExportBatch(9L, 0L, null, "Female")).thenReturn(List.of());

        service.writeCsvForDoctor(9L, null, "Female", new ByteArrayOutputStream());

        verify(repository).findCsvExportBatch(9L, 0L, null, "Female");
    }

    @Test
    void emptyExportContainsOnlyBomAndQuotedHeader() throws Exception {
        when(repository.findCsvExportBatch(3L, 0L, null, null)).thenReturn(List.of());
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.writeCsvForDoctor(3L, null, null, output);

        assertEquals("\uFEFF\"id\",\"fullName\",\"age\",\"gender\",\"visitDate\",\"phone\",\"email\",\"doctor\",\"chiefComplaints\"\r\n",
                new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void quotesEveryFieldEscapesQuotesAndWritesNullAsEmpty() throws Exception {
        when(repository.findCsvExportBatch(3L, 0L, null, null)).thenReturn(List.of(
                row(1L, "Sam \"Sunny\"", null, "Other", null, null, null, "Dr \"A\"", null)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.writeCsvForDoctor(3L, null, null, output);

        String csv = new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"1\",\"Sam \"\"Sunny\"\"\",\"\",\"Other\",\"\",\"\",\"\",\"Dr \"\"A\"\"\",\"\"\r\n"));
    }

    @Test
    void protectsFormulaLikeTextValues() throws Exception {
        when(repository.findCsvExportBatch(3L, 0L, null, null)).thenReturn(List.of(
                row(1L, "  =1+1", 30, "+Male", null, "\t@cmd", null, null, "-1")));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        service.writeCsvForDoctor(3L, null, null, output);

        String csv = new String(output.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("'  =1+1"), csv.replace("\t", "<TAB>"));
        assertTrue(csv.contains("'+Male"), csv.replace("\t", "<TAB>"));
        assertTrue(csv.contains("'\t@cmd"), csv.replace("\t", "<TAB>"));
        assertTrue(csv.contains("\"'-1\""), csv.replace("\t", "<TAB>"));
    }

    private static PatientCsvRow row(Long id, String name, Integer age, String gender,
            LocalDate visitDate, String phone, String email, String doctor, String complaints) {
        return new PatientCsvRow() {
            public Long getId() { return id; }
            public String getFullName() { return name; }
            public Integer getAge() { return age; }
            public String getGender() { return gender; }
            public LocalDate getVisitDate() { return visitDate; }
            public String getPhone() { return phone; }
            public String getEmail() { return email; }
            public String getDoctor() { return doctor; }
            public String getChiefComplaints() { return complaints; }
        };
    }
}
