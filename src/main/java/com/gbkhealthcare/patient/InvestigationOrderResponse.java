package com.gbkhealthcare.patient;
import java.time.LocalDateTime;
public record InvestigationOrderResponse(Long id,Long patientVisitId,Long doctorId,InvestigationOrder.Type type,String name,String indication,InvestigationOrder.Priority priority,InvestigationOrder.Status status,LocalDateTime orderedAt,LocalDateTime updatedAt){static InvestigationOrderResponse from(InvestigationOrder o){return new InvestigationOrderResponse(o.getId(),o.getPatientVisit().getId(),o.getDoctor().getId(),o.getType(),o.getName(),o.getIndication(),o.getPriority(),o.getStatus(),o.getOrderedAt(),o.getUpdatedAt());}}
