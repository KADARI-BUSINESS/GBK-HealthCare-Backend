package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DoctorActivityServiceTest {
    @Test
    void returnsOnlyRequestedDoctorsEventsWithStableDescendingSortAndSafeLabel() {
        DoctorActivityRepository repository = mock(DoctorActivityRepository.class);
        DoctorRepository doctors = mock(DoctorRepository.class);
        Doctor doctor = new Doctor();
        doctor.setId(11L);
        when(doctors.getReferenceById(11L)).thenReturn(doctor);
        DoctorActivity event = new DoctorActivity(doctor,
                DoctorActivity.ActionType.FOLLOW_UP_RESCHEDULED,
                DoctorActivity.EntityType.FOLLOW_UP, 91L,
                LocalDateTime.of(2026, 11, 13, 12, 0));
        when(repository.findByDoctor_Id(eq(11L), any(Pageable.class)))
                .thenReturn(List.of(event));
        DoctorActivityService service = new DoctorActivityService(repository, doctors);

        List<DoctorActivityResponse> result = service.getRecentActivities(11L, 8);

        assertEquals(1, result.size());
        assertEquals(DoctorActivity.ActionType.FOLLOW_UP_RESCHEDULED, result.get(0).actionType());
        assertEquals(DoctorActivity.EntityType.FOLLOW_UP, result.get(0).entityType());
        assertEquals(91L, result.get(0).entityId());
        assertEquals("Rescheduled a follow-up", result.get(0).displayMessage());
        assertEquals(java.time.Instant.parse("2026-11-13T06:30:00Z"), result.get(0).occurredAt());

        org.mockito.ArgumentCaptor<Pageable> page = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByDoctor_Id(eq(11L), page.capture());
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(8, page.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")),
                page.getValue().getSort());
    }

    @Test
    void recordingUsesAuthenticatedDoctorAndMinimalEventFields() {
        DoctorActivityRepository repository = mock(DoctorActivityRepository.class);
        DoctorRepository doctors = mock(DoctorRepository.class);
        Doctor doctor = new Doctor();
        doctor.setId(11L);
        when(doctors.getReferenceById(11L)).thenReturn(doctor);
        DoctorActivityService service = new DoctorActivityService(repository, doctors);

        service.recordActivity(11L, DoctorActivity.ActionType.PATIENT_REGISTERED,
                DoctorActivity.EntityType.PATIENT, 32L);

        org.mockito.ArgumentCaptor<DoctorActivity> event =
                org.mockito.ArgumentCaptor.forClass(DoctorActivity.class);
        verify(repository).save(event.capture());
        assertEquals(11L, event.getValue().getDoctor().getId());
        assertEquals(DoctorActivity.ActionType.PATIENT_REGISTERED, event.getValue().getActionType());
        assertEquals(DoctorActivity.EntityType.PATIENT, event.getValue().getEntityType());
        assertEquals(32L, event.getValue().getEntityId());
        assertTrue(event.getValue().getOccurredAt().isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    void returnsEmptyListForDoctorWithoutEvents() {
        DoctorActivityRepository repository = mock(DoctorActivityRepository.class);
        when(repository.findByDoctor_Id(eq(11L), any(Pageable.class))).thenReturn(List.of());
        DoctorActivityService service = new DoctorActivityService(
                repository, mock(DoctorRepository.class));

        assertTrue(service.getRecentActivities(11L, 8).isEmpty());
    }
}
