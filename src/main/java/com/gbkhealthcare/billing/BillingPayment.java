package com.gbkhealthcare.billing;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="billing_payments",uniqueConstraints=@UniqueConstraint(name="uk_payment_receipt_number",columnNames="receipt_number"),indexes=@Index(name="idx_payment_invoice_time",columnList="invoice_id, paid_at"))
public class BillingPayment {
 public enum Method{CASH,UPI,CARD,OTHER}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invoice_id",nullable=false,updatable=false) private BillingInvoice invoice;
 @Column(name="receipt_number",nullable=false,length=48,updatable=false) private String receiptNumber;
 @Column(nullable=false,precision=12,scale=2,updatable=false) private BigDecimal amount;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16,updatable=false) private Method method;
 @Column(name="method_label",length=80,updatable=false) private String methodLabel;
 @Column(name="payment_reference",length=180,updatable=false) private String paymentReference;
 @Column(name="paid_at",nullable=false,updatable=false) private LocalDateTime paidAt;
 @Column(name="recorded_by_staff_id",nullable=false,updatable=false) private Long recordedByStaffId;
 protected BillingPayment(){}
 public BillingPayment(BillingInvoice invoice,String receipt,BigDecimal amount,Method method,String methodLabel,String reference,Long staffId,LocalDateTime at){this.invoice=invoice;receiptNumber=receipt;this.amount=amount;this.method=method;this.methodLabel=methodLabel==null||methodLabel.isBlank()?null:methodLabel.trim();paymentReference=reference==null||reference.isBlank()?null:reference.trim();recordedByStaffId=staffId;paidAt=at;}
 public BillingPayment(BillingInvoice invoice,String receipt,BigDecimal amount,Method method,String reference,Long staffId,LocalDateTime at){this(invoice,receipt,amount,method,null,reference,staffId,at);}
 public Long getId(){return id;} public BillingInvoice getInvoice(){return invoice;} public String getReceiptNumber(){return receiptNumber;} public BigDecimal getAmount(){return amount;} public Method getMethod(){return method;} public String getMethodLabel(){return methodLabel;} public String getPaymentReference(){return paymentReference;} public LocalDateTime getPaidAt(){return paidAt;} public Long getRecordedByStaffId(){return recordedByStaffId;}
}
