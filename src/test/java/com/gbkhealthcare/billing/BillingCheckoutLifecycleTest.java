package com.gbkhealthcare.billing;

import com.gbkhealthcare.auth.*;
import com.gbkhealthcare.patient.*;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BillingCheckoutLifecycleTest {
    private StaffAccessService staff;
    private PatientVisitRepository visits;
    private BillingInvoiceRepository invoices;
    private BillingPaymentRepository payments;
    private BillingRefundRepository refunds;
    private BillingAdjustmentRepository adjustments;
    private BillingInvoiceLineRepository lines;
    private BillingAuditEventRepository audit;
    private BillingService service;
    private PatientVisit visit;
    private BillingInvoice invoice;
    private StaffAccount actor;
    private Jwt jwt;

    @BeforeEach void setUp() {
        staff = mock(StaffAccessService.class); visits = mock(PatientVisitRepository.class);
        invoices = mock(BillingInvoiceRepository.class); payments = mock(BillingPaymentRepository.class);
        refunds = mock(BillingRefundRepository.class); adjustments = mock(BillingAdjustmentRepository.class);
        lines = mock(BillingInvoiceLineRepository.class); audit = mock(BillingAuditEventRepository.class);
        actor = mock(StaffAccount.class); when(actor.getId()).thenReturn(14L); when(actor.getEmail()).thenReturn("checkout@example.test");
        when(staff.requireRole(any(), eq(StaffRole.CHECKOUT))).thenReturn(actor);
        Patient patient = mock(Patient.class); when(patient.getId()).thenReturn(5L); when(patient.getFullName()).thenReturn("Test Patient");
        visit = mock(PatientVisit.class); when(visit.getId()).thenReturn(30L); when(visit.getPatient()).thenReturn(patient);
        when(visit.getVisitDate()).thenReturn(java.time.LocalDate.now()); when(visit.getConsultationStatus()).thenReturn("SIGNED");
        when(visit.getConsultationSignedAt()).thenReturn(LocalDateTime.now()); when(visit.getCheckoutStatus()).thenReturn("PENDING");
        invoice = new BillingInvoice(visit, "GBK-CHECKOUT", new BigDecimal("100.00"), BigDecimal.ZERO,
                BigDecimal.ZERO, 12L, LocalDateTime.now());
        try { var id = BillingInvoice.class.getDeclaredField("id"); id.setAccessible(true); id.set(invoice, 9L); }
        catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        invoice.issue(); invoice.updateBalances(new BigDecimal("100.00"), BigDecimal.ZERO, LocalDateTime.now());
        when(invoices.findByIdForUpdate(9L)).thenReturn(Optional.of(invoice));
        when(visits.findByIdForBillingUpdate(30L)).thenReturn(Optional.of(visit));
        when(visits.save(visit)).thenReturn(visit); when(lines.findAllByInvoice_IdOrderByIdAsc(anyLong())).thenReturn(List.of());
        when(payments.findAllByInvoice_IdOrderByPaidAtAsc(anyLong())).thenReturn(List.of());
        when(refunds.findAllByPayment_IdOrderByRefundedAtAsc(anyLong())).thenReturn(List.of());
        when(adjustments.findAllByInvoice_IdOrderByCreatedAtAsc(anyLong())).thenReturn(List.of());
        jwt = Jwt.withTokenValue("test").header("alg", "none").subject("checkout@example.test").build();
        service = new BillingService(staff, mock(AdminAccountRepository.class), visits, mock(BillableServiceRepository.class),
                mock(BillableServicePriceRepository.class), invoices, lines, payments, refunds, adjustments, audit);
    }

    @Test void checkoutCompletesOnlyAfterSignedConsultationAndZeroBalanceAndAuditsActor() {
        service.completeCheckout(jwt, 9L);
        verify(visit).setCheckoutStatus("COMPLETE"); verify(visit).setCheckoutCompletedAt(any(LocalDateTime.class));
        verify(visit).setCheckoutCompletedByStaffId(14L); verify(visits).save(visit);
        verify(audit).save(argThat(e -> e.getActionType().equals("CHECKOUT_COMPLETED") && e.getActorId().equals(14L)));
    }

    @Test void checkoutRejectsUnsignedConsultationAndOutstandingBalance() {
        when(visit.getConsultationStatus()).thenReturn("DRAFT");
        assertThrows(ResponseStatusException.class, () -> service.completeCheckout(jwt, 9L));
        verify(visits, never()).save(any());
        when(visit.getConsultationStatus()).thenReturn("SIGNED");
        invoice.updateBalances(new BigDecimal("20.00"), BigDecimal.ZERO, LocalDateTime.now());
        assertThrows(ResponseStatusException.class, () -> service.completeCheckout(jwt, 9L));
        verify(visits, never()).save(any());
    }

    @Test void duplicateCheckoutIsRejected() {
        when(visit.getCheckoutStatus()).thenReturn("COMPLETE");
        assertThrows(ResponseStatusException.class, () -> service.completeCheckout(jwt, 9L));
        verify(visits, never()).save(any());
    }

    @Test void checkoutLocksInvoiceAndEncounterRowsForConcurrentRequests() throws Exception {
        assertEquals(LockModeType.PESSIMISTIC_WRITE,
                BillingInvoiceRepository.class.getMethod("findByIdForUpdate", Long.class).getAnnotation(Lock.class).value());
        assertEquals(LockModeType.PESSIMISTIC_WRITE,
                PatientVisitRepository.class.getMethod("findByIdForBillingUpdate", Long.class).getAnnotation(Lock.class).value());
    }
}
