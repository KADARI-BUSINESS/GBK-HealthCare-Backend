package com.gbkhealthcare.billing;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="billing_invoice_lines",indexes=@Index(name="idx_invoice_line_invoice",columnList="invoice_id"))
public class BillingInvoiceLine {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invoice_id",nullable=false) private BillingInvoice invoice;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_id",updatable=false) private BillableService service;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="price_version_id",updatable=false) private BillableServicePrice priceVersion;
 @Column(name="service_code_snapshot",nullable=false,length=40,updatable=false) private String serviceCodeSnapshot;
 @Column(name="service_name_snapshot",nullable=false,length=160,updatable=false) private String serviceNameSnapshot;
 @Column(nullable=false,precision=8,scale=2,updatable=false) private BigDecimal quantity;
 @Column(name="unit_price_snapshot",nullable=false,precision=10,scale=2,updatable=false) private BigDecimal unitPriceSnapshot;
 @Column(name="tax_rate_percent_snapshot",nullable=false,precision=5,scale=2,updatable=false) private BigDecimal taxRatePercentSnapshot;
 @Column(name="tax_amount_snapshot",nullable=false,precision=10,scale=2,updatable=false) private BigDecimal taxAmountSnapshot;
 @Column(name="line_total",nullable=false,precision=12,scale=2,updatable=false) private BigDecimal lineTotal;
 protected BillingInvoiceLine(){}
 public BillingInvoiceLine(BillableService service,BillableServicePrice price,BigDecimal quantity){this.service=service;priceVersion=price;serviceCodeSnapshot=service.getCode();serviceNameSnapshot=service.getName();this.quantity=quantity;unitPriceSnapshot=price.getAmount();taxRatePercentSnapshot=price.getTaxRatePercent();BigDecimal base=unitPriceSnapshot.multiply(quantity).setScale(2,java.math.RoundingMode.HALF_UP);taxAmountSnapshot=base.multiply(taxRatePercentSnapshot).divide(new BigDecimal("100"),2,java.math.RoundingMode.HALF_UP);lineTotal=base.add(taxAmountSnapshot);}
 void attach(BillingInvoice value){invoice=value;} public Long getId(){return id;} public String getServiceCodeSnapshot(){return serviceCodeSnapshot;} public String getServiceNameSnapshot(){return serviceNameSnapshot;} public BigDecimal getQuantity(){return quantity;} public BigDecimal getUnitPriceSnapshot(){return unitPriceSnapshot;} public BigDecimal getTaxRatePercentSnapshot(){return taxRatePercentSnapshot;} public BigDecimal getTaxAmountSnapshot(){return taxAmountSnapshot;} public BigDecimal getLineTotal(){return lineTotal;}
}
