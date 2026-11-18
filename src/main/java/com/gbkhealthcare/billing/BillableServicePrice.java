package com.gbkhealthcare.billing;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name="billable_service_prices",uniqueConstraints=@UniqueConstraint(name="uk_service_price_version",columnNames={"service_id","version"}),indexes=@Index(name="idx_service_price_latest",columnList="service_id, version"))
public class BillableServicePrice {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_id",nullable=false,updatable=false) private BillableService service;
 @Column(nullable=false) private int version;
 @Column(nullable=false,precision=10,scale=2) private BigDecimal amount;
 @Column(name="tax_rate_percent",nullable=false,precision=5,scale=2) private BigDecimal taxRatePercent;
 @Column(name="effective_at",nullable=false,updatable=false) private LocalDateTime effectiveAt;
 @Column(name="created_by_admin_id",nullable=false,updatable=false) private Long createdByAdminId;
 protected BillableServicePrice(){}
 public BillableServicePrice(BillableService service,int version,BigDecimal amount,BigDecimal taxRate,Long adminId,LocalDateTime at){this.service=service;this.version=version;this.amount=amount;this.taxRatePercent=taxRate;this.createdByAdminId=adminId;this.effectiveAt=at;}
 public Long getId(){return id;} public BillableService getService(){return service;} public int getVersion(){return version;} public BigDecimal getAmount(){return amount;} public BigDecimal getTaxRatePercent(){return taxRatePercent;} public LocalDateTime getEffectiveAt(){return effectiveAt;} public Long getCreatedByAdminId(){return createdByAdminId;}
}
