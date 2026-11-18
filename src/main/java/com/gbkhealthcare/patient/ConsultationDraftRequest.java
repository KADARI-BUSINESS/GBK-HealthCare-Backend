package com.gbkhealthcare.patient;

import jakarta.validation.constraints.Size;

public record ConsultationDraftRequest(
        @Size(max = 1000) String chiefComplaints,
        @Size(max = 20000) String presentIllness,
        @Size(max = 20000) String generalExamination,
        @Size(max = 20000) String heartExamination,
        @Size(max = 20000) String lungsExamination,
        @Size(max = 20000) String abdomenExamination,
        @Size(max = 20000) String rectalExamination,
        @Size(max = 20000) String diagnosis,
        @Size(max = 20000) String assessment,
        @Size(max = 20000) String treatmentPlan,
        @Size(max = 20000) String doctorNotes) {}
