package com.gbkhealthcare.patient;
import java.math.BigDecimal;
import java.time.LocalDate;
public record PrescriptionItemResponse(Long id,String medicineName,String dosage,String route,String frequency,String duration,BigDecimal quantity,String instructions,LocalDate startDate){static PrescriptionItemResponse from(PrescriptionItem i){return new PrescriptionItemResponse(i.getId(),i.getMedicineName(),i.getDosage(),i.getRoute(),i.getFrequency(),i.getDuration(),i.getQuantity(),i.getInstructions(),i.getStartDate());}}
