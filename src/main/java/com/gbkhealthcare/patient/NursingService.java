package com.gbkhealthcare.patient;

import com.gbkhealthcare.auth.StaffAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class NursingService {
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private final NursingQueueEntryRepository queue;
    private final NursingIntakeRecordRepository intakes;
    private final NursingIntakeAuditEventRepository audit;
    private final DoctorQueueEntryRepository doctorQueue;

    public NursingService(NursingQueueEntryRepository queue, NursingIntakeRecordRepository intakes,
            NursingIntakeAuditEventRepository audit, DoctorQueueEntryRepository doctorQueue) {
        this.queue = queue; this.intakes = intakes; this.audit = audit; this.doctorQueue = doctorQueue;
    }

    @Transactional(readOnly = true)
    public List<NursingQueueItemResponse> activeQueue() {
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        return queue.findActiveQueue().stream().map(q -> NursingQueueItemResponse.from(q, now)).toList();
    }

    @Transactional
    public NursingQueueItemResponse claim(Long queueId, StaffAccount nurse) {
        requireActor(nurse);
        NursingQueueEntry entry = lock(queueId);
        if (!"WAITING".equals(entry.getStatus())) throw conflict("This encounter has already been claimed or handed off.");
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        entry.claim(nurse.getId(), now);
        queue.save(entry);
        audit.save(new NursingIntakeAuditEvent(entry.getId(), null, nurse.getId(), "CLAIMED", null, now));
        return NursingQueueItemResponse.from(entry, now);
    }

    @Transactional(readOnly = true)
    public List<NursingIntakeResponse> intakeHistory(Long queueId, StaffAccount nurse) {
        requireClaimedBy(queueId, nurse);
        return intakes.findAllByQueueEntry_IdOrderByRevisionAsc(queueId).stream().map(NursingIntakeResponse::from).toList();
    }

    @Transactional
    public NursingIntakeResponse saveIntake(Long queueId, StaffAccount nurse, NursingIntakeRequest request) {
        requireActor(nurse);
        if (request == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nursing intake details are required.");
        NursingQueueEntry entry = lock(queueId);
        requireClaimOwner(entry, nurse);
        int revision = intakes.findMaxRevision(queueId) + 1;
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        NursingIntakeRecord record = intakes.save(new NursingIntakeRecord(entry, nurse.getId(), revision, request, calculateBmi(request), "DRAFT", now));
        audit.save(new NursingIntakeAuditEvent(entry.getId(), record.getId(), nurse.getId(), "INTAKE_SAVED", revision, now));
        return NursingIntakeResponse.from(record);
    }

    @Transactional(readOnly = true)
    public List<NursingAuditEventResponse> auditHistory(Long queueId, StaffAccount nurse) {
        requireClaimedBy(queueId, nurse);
        return audit.findAllByQueueEntryIdOrderByOccurredAtAscIdAsc(queueId).stream().map(NursingAuditEventResponse::from).toList();
    }

    @Transactional
    public NursingHandoffResponse handoff(Long queueId, StaffAccount nurse) {
        requireActor(nurse);
        NursingQueueEntry entry = lock(queueId);
        requireClaimOwner(entry, nurse);
        NursingIntakeRecord latest = intakes.findTopByQueueEntry_IdOrderByRevisionDesc(queueId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Save nursing intake before handing off."));
        if (entry.getVisit().getAssignedDoctor() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This encounter has no assigned Doctor and cannot be handed off.");
        }
        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        NursingIntakeRequest snapshot = new NursingIntakeRequest(latest.getBpSystolic(), latest.getBpDiastolic(), latest.getPulseRate(),
                latest.getTemperatureC(), latest.getRespiratoryRate(), latest.getOxygenSaturation(), latest.getHeightCm(), latest.getWeightKg(),
                latest.getAllergies(), latest.getMedicationReconciliation(), latest.getNursingNotes());
        NursingIntakeRecord signed = intakes.save(new NursingIntakeRecord(entry, nurse.getId(), intakes.findMaxRevision(queueId) + 1,
                snapshot, latest.getBmi(), "HANDED_OFF", now));
        entry.handoff(now);
        entry.getVisit().setEncounterStatus("DOCTOR_WAITING");
        queue.save(entry);
        DoctorQueueEntry target = doctorQueue.save(new DoctorQueueEntry(entry, entry.getVisit(), entry.getVisit().getAssignedDoctor(), now));
        audit.save(new NursingIntakeAuditEvent(entry.getId(), signed.getId(), nurse.getId(), "HANDED_OFF", signed.getRevision(), now));
        return new NursingHandoffResponse(target.getId(), entry.getVisit().getId(), entry.getVisit().getAssignedDoctor().getId(), "WAITING", now);
    }

    static BigDecimal calculateBmi(NursingIntakeRequest request) {
        if (request.heightCm() == null || request.weightKg() == null) return null;
        BigDecimal metres = request.heightCm().movePointLeft(2);
        return request.weightKg().divide(metres.multiply(metres), 2, RoundingMode.HALF_UP);
    }

    private NursingQueueEntry lock(Long id) {
        return queue.findByIdForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursing queue entry not found."));
    }
    private void requireClaimedBy(Long id, StaffAccount nurse) {
        requireActor(nurse);
        NursingQueueEntry entry = queue.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nursing queue entry not found."));
        requireClaimOwner(entry, nurse);
    }
    private static void requireClaimOwner(NursingQueueEntry entry, StaffAccount nurse) {
        if (!"CLAIMED".equals(entry.getStatus()) || !nurse.getId().equals(entry.getClaimedByStaffId()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the nurse who claimed this encounter can manage its intake.");
    }
    private static void requireActor(StaffAccount actor) {
        if (actor == null || actor.getId() == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active Nurse identity is required.");
    }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
