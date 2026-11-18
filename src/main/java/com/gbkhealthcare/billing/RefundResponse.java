package com.gbkhealthcare.billing;
import java.math.BigDecimal;import java.time.LocalDateTime;
public record RefundResponse(Long id,Long paymentId,BigDecimal amount,String reason,String originalPaymentReference,String refundReference,Long refundedByStaffId,LocalDateTime refundedAt){static RefundResponse from(BillingRefund r){return new RefundResponse(r.getId(),r.getPayment().getId(),r.getAmount(),r.getReason(),r.getOriginalPaymentReference(),r.getRefundReference(),r.getRefundedByStaffId(),r.getRefundedAt());}}
