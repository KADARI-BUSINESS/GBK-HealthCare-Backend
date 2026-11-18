package com.gbkhealthcare.billing;
import java.math.BigDecimal;import java.time.LocalDateTime;
public record ServicePriceResponse(Long id,int version,BigDecimal amount,BigDecimal taxRatePercent,LocalDateTime effectiveAt,Long createdByAdminId){static ServicePriceResponse from(BillableServicePrice p){return new ServicePriceResponse(p.getId(),p.getVersion(),p.getAmount(),p.getTaxRatePercent(),p.getEffectiveAt(),p.getCreatedByAdminId());}}
