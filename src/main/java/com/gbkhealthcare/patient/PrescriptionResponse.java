package com.gbkhealthcare.patient;
import java.time.LocalDateTime;
import java.util.List;
public record PrescriptionResponse(Long id,Long patientVisitId,Long doctorId,int revision,Prescription.State status,Long supersedesPrescriptionId,String notes,LocalDateTime createdAt,LocalDateTime signedAt,List<PrescriptionItemResponse> items){static PrescriptionResponse from(Prescription p){return new PrescriptionResponse(p.getId(),p.getPatientVisit().getId(),p.getDoctor().getId(),p.getRevision(),p.getStatus(),p.getSupersedes()==null?null:p.getSupersedes().getId(),p.getNotes(),p.getCreatedAt(),p.getSignedAt(),p.getItems().stream().map(PrescriptionItemResponse::from).toList());}}
