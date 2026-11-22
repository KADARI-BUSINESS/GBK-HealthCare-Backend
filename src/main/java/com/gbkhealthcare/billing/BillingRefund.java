package com.gbkhealthcare.billing;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="billing_refunds",indexes=@Index(name="idx_refund_payment_time",columnList="payment_id, refunded_at"))
public class BillingRefund {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payment_id",nullable=false,updatable=false) private BillingPayment payment;
 @Column(nullable=false,precision=12,scale=2,updatable=false) private BigDecimal amount;
 @Column(nullable=false,length=1000,updatable=false) private String reason;
 @Column(name="original_payment_reference",length=180,updatable=false) private String originalPaymentReference;
 @Column(name="refund_reference",length=180,updatable=false) private String refundReference;
 @Column(name="refunded_by_staff_id",nullable=false,updatable=false) private Long refundedByStaffId;
 @Column(name="refunded_at",nullable=false,updatable=false) private LocalDateTime refundedAt;
 protected BillingRefund(){}
 public BillingRefund(BillingPayment payment,BigDecimal amount,String reason,String reference,Long staffId,LocalDateTime at){this.payment=payment;this.amount=amount;this.reason=reason.trim();originalPaymentReference=payment.getPaymentReference();refundReference=reference==null||reference.isBlank()?null:reference.trim();refundedByStaffId=staffId;refundedAt=at;}
 public Long getId(){return id;} public BillingPayment getPayment(){return payment;} public BigDecimal getAmount(){return amount;} public String getReason(){return reason;} public String getOriginalPaymentReference(){return originalPaymentReference;} public String getRefundReference(){return refundReference;} public Long getRefundedByStaffId(){return refundedByStaffId;} public LocalDateTime getRefundedAt(){return refundedAt;}
}
