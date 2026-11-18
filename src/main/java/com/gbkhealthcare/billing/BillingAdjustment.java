package com.gbkhealthcare.billing;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="billing_adjustments",indexes=@Index(name="idx_adjustment_invoice_time",columnList="invoice_id, created_at"))
public class BillingAdjustment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invoice_id",nullable=false,updatable=false) private BillingInvoice invoice;
 @Column(nullable=false,precision=12,scale=2,updatable=false) private BigDecimal amount;
 @Column(nullable=false,length=1000,updatable=false) private String reason;
 @Column(name="created_by_staff_id",nullable=false,updatable=false) private Long createdByStaffId;
 @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
 protected BillingAdjustment(){}
 public BillingAdjustment(BillingInvoice invoice,BigDecimal amount,String reason,Long staffId,LocalDateTime at){this.invoice=invoice;this.amount=amount;this.reason=reason.trim();createdByStaffId=staffId;createdAt=at;}
 public Long getId(){return id;} public BillingInvoice getInvoice(){return invoice;} public BigDecimal getAmount(){return amount;} public String getReason(){return reason;} public Long getCreatedByStaffId(){return createdByStaffId;} public LocalDateTime getCreatedAt(){return createdAt;}
}
