package com.gbkhealthcare.billing;
import com.gbkhealthcare.patient.PatientVisit;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity @Table(name="billing_invoices",uniqueConstraints=@UniqueConstraint(name="uk_billing_invoice_number",columnNames="invoice_number"),indexes={@Index(name="idx_invoice_visit_status",columnList="patient_visit_id,status"),@Index(name="idx_invoice_patient_date",columnList="patient_id,invoice_date")})
public class BillingInvoice {
 public enum Status{DRAFT,ISSUED,PARTIALLY_PAID,PAID,VOID}
 public enum DiscountStatus{NOT_REQUESTED,PENDING,APPROVED,REJECTED}
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="patient_visit_id",nullable=false,updatable=false) private PatientVisit patientVisit;
 @Column(name="patient_id",nullable=false,updatable=false) private Long patientId;
 @Column(name="invoice_number",nullable=false,length=48,updatable=false) private String invoiceNumber;
 @Column(name="invoice_date",nullable=false,updatable=false) private LocalDateTime invoiceDate;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.DRAFT;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal subtotal;
 @Column(name="discount_amount",nullable=false,precision=12,scale=2) private BigDecimal discountAmount;
 @Column(name="tax_amount",nullable=false,precision=12,scale=2) private BigDecimal taxAmount;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal total;
 @Column(name="paid_amount",nullable=false,precision=12,scale=2) private BigDecimal paidAmount=BigDecimal.ZERO;
 @Column(name="outstanding_amount",nullable=false,precision=12,scale=2) private BigDecimal outstandingAmount;
 @Enumerated(EnumType.STRING) @Column(name="discount_status",nullable=false,length=20) private DiscountStatus discountStatus;
 @Column(name="discount_requested_by_staff_id") private Long discountRequestedByStaffId;
 @Column(name="discount_approved_by_admin_id") private Long discountApprovedByAdminId;
 @Column(name="discount_approved_at") private LocalDateTime discountApprovedAt;
 @Column(name="created_by_staff_id",nullable=false,updatable=false) private Long createdByStaffId;
 @OneToMany(mappedBy="invoice",cascade=CascadeType.ALL,orphanRemoval=true) private List<BillingInvoiceLine> lines=new ArrayList<>();
 protected BillingInvoice(){}
 public BillingInvoice(PatientVisit visit,String number,BigDecimal subtotal,BigDecimal discount,BigDecimal tax,Long staffId,LocalDateTime at){patientVisit=visit;patientId=visit.getPatient().getId();invoiceNumber=number;invoiceDate=at;this.subtotal=subtotal;discountAmount=discount;taxAmount=tax;total=subtotal.add(tax).subtract(discount);paidAmount=BigDecimal.ZERO;outstandingAmount=total;discountStatus=discount.signum()>0?DiscountStatus.PENDING:DiscountStatus.NOT_REQUESTED;discountRequestedByStaffId=discount.signum()>0?staffId:null;createdByStaffId=staffId;}
 public void addLine(BillingInvoiceLine line){line.attach(this);lines.add(line);} public void issue(){if(status!=Status.DRAFT)throw new IllegalStateException("Only draft invoices can be issued.");if(discountStatus==DiscountStatus.PENDING)throw new IllegalStateException("Discount approval is pending.");if(discountStatus==DiscountStatus.REJECTED)throw new IllegalStateException("Discount was rejected; create a corrected invoice.");status=outstandingAmount.signum()==0?Status.PAID:Status.ISSUED;}
 public void approveDiscount(Long adminId,LocalDateTime at,boolean approved){if(status!=Status.DRAFT||discountStatus!=DiscountStatus.PENDING)throw new IllegalStateException("No pending discount approval exists.");discountStatus=approved?DiscountStatus.APPROVED:DiscountStatus.REJECTED;if(approved){discountApprovedByAdminId=adminId;discountApprovedAt=at;}}
 public void voidDraft(){if(status!=Status.DRAFT||paidAmount.signum()!=0)throw new IllegalStateException("Only unpaid draft invoices can be voided.");status=Status.VOID;}
 public void updateBalances(BigDecimal paid,BigDecimal adjustment,LocalDateTime at){paidAmount=paid;total=subtotal.add(taxAmount).subtract(discountAmount).add(adjustment);outstandingAmount=total.subtract(paidAmount);if(status!=Status.DRAFT&&status!=Status.VOID)status=outstandingAmount.signum()<=0?Status.PAID:paidAmount.signum()>0?Status.PARTIALLY_PAID:Status.ISSUED;}
 public Long getId(){return id;} public PatientVisit getPatientVisit(){return patientVisit;} public Long getPatientId(){return patientId;} public String getInvoiceNumber(){return invoiceNumber;} public LocalDateTime getInvoiceDate(){return invoiceDate;} public Status getStatus(){return status;} public BigDecimal getSubtotal(){return subtotal;} public BigDecimal getDiscountAmount(){return discountAmount;} public BigDecimal getTaxAmount(){return taxAmount;} public BigDecimal getTotal(){return total;} public BigDecimal getPaidAmount(){return paidAmount;} public BigDecimal getOutstandingAmount(){return outstandingAmount;} public DiscountStatus getDiscountStatus(){return discountStatus;} public Long getDiscountRequestedByStaffId(){return discountRequestedByStaffId;} public Long getDiscountApprovedByAdminId(){return discountApprovedByAdminId;} public LocalDateTime getDiscountApprovedAt(){return discountApprovedAt;} public Long getCreatedByStaffId(){return createdByStaffId;} public List<BillingInvoiceLine> getLines(){return List.copyOf(lines);}
}
