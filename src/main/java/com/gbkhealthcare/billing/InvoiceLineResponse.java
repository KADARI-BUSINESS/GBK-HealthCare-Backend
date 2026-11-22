package com.gbkhealthcare.billing;
import java.math.BigDecimal;
public record InvoiceLineResponse(Long id,String serviceCode,String serviceName,BigDecimal quantity,BigDecimal unitPrice,BigDecimal taxRatePercent,BigDecimal taxAmount,BigDecimal lineTotal){static InvoiceLineResponse from(BillingInvoiceLine l){return new InvoiceLineResponse(l.getId(),l.getServiceCodeSnapshot(),l.getServiceNameSnapshot(),l.getQuantity(),l.getUnitPriceSnapshot(),l.getTaxRatePercentSnapshot(),l.getTaxAmountSnapshot(),l.getLineTotal());}}
