package com.gbkhealthcare.billing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CheckoutQueueItemResponse(
        Long patientVisitId, Long patientId, String patientName, String patientMobile,
        LocalDate visitDate, String assignedDoctorName, LocalDateTime consultationSignedAt,
        String consultationStatus, String checkoutStatus, Long invoiceId, String invoiceNumber,
        String invoiceStatus, BigDecimal invoiceTotal, BigDecimal amountPaid,
        BigDecimal refundedAmount, BigDecimal adjustmentAmount, BigDecimal outstandingBalance,
        boolean checkoutReady, List<String> checkoutBlockers) {}
