package com.gbkhealthcare.billing;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record PaymentRequest(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@NotNull BillingPayment.Method method,@Size(max=80) String methodLabel,@Size(max=180) String paymentReference){}
