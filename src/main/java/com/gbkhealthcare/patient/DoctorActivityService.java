package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.Doctor;
import com.gbkhealthcare.auth.DoctorRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class DoctorActivityService {
    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");

    private final DoctorActivityRepository activityRepository;
    private final DoctorRepository doctorRepository;

    public DoctorActivityService(
            DoctorActivityRepository activityRepository,
            DoctorRepository doctorRepository) {
        this.activityRepository = activityRepository;
        this.doctorRepository = doctorRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordActivity(Long doctorId, DoctorActivity.ActionType actionType,
            DoctorActivity.EntityType entityType, Long entityId) {
        Doctor doctor = doctorRepository.getReferenceById(doctorId);
        activityRepository.save(new DoctorActivity(
                doctor, actionType, entityType, entityId,
                LocalDateTime.now(INDIA_ZONE)));
    }

    @Transactional(readOnly = true)
    public List<DoctorActivityResponse> getRecentActivities(Long doctorId, int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id")));
        return activityRepository.findByDoctor_Id(doctorId, pageable).stream()
                .map(activity -> new DoctorActivityResponse(
                        activity.getId(), activity.getActionType(),
                        activity.getEntityType(), activity.getEntityId(),
                        activity.getOccurredAt().atZone(INDIA_ZONE).toInstant(),
                        displayMessage(activity.getActionType())))
                .toList();
    }

    static String displayMessage(DoctorActivity.ActionType actionType) {
        return switch (actionType) {
            case PATIENT_REGISTERED -> "Registered a new patient";
            case VISIT_RECORDED -> "Recorded a clinical visit";
            case FOLLOW_UP_SCHEDULED -> "Scheduled a follow-up";
            case FOLLOW_UP_RESCHEDULED -> "Rescheduled a follow-up";
            case FOLLOW_UP_COMPLETED -> "Completed a follow-up";
            case FOLLOW_UP_CANCELLED -> "Cancelled a follow-up";
        };
    }
}
