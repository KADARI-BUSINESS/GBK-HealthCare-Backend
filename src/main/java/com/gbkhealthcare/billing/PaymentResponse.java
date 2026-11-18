package com.gbkhealthcare.billing;
import java.math.BigDecimal;import java.time.LocalDateTime;
public record PaymentResponse(Long id,String receiptNumber,BigDecimal amount,BillingPayment.Method method,String methodLabel,String reference,LocalDateTime paidAt,Long recordedByStaffId){static PaymentResponse from(BillingPayment p){return new PaymentResponse(p.getId(),p.getReceiptNumber(),p.getAmount(),p.getMethod(),p.getMethodLabel(),p.getPaymentReference(),p.getPaidAt(),p.getRecordedByStaffId());}}
