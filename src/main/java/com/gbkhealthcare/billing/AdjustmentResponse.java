package com.gbkhealthcare.billing;
import java.math.BigDecimal;import java.time.LocalDateTime;
public record AdjustmentResponse(Long id,BigDecimal amount,String reason,Long createdByStaffId,LocalDateTime createdAt){static AdjustmentResponse from(BillingAdjustment a){return new AdjustmentResponse(a.getId(),a.getAmount(),a.getReason(),a.getCreatedByStaffId(),a.getCreatedAt());}}
