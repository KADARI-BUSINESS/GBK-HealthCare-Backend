package com.gbkhealthcare.patient;

public record DashboardStatsResponse(
        long totalPatients,
        long todaysVisits,
        long availableDoctors,
        long pendingFollowUps,
        long registeredRecords,
        long clinicalNotes) {
}
