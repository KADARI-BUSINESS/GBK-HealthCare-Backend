package com.gbkhealthcare.billing;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record InvoiceLineRequest(@NotNull @Positive Long serviceId,@NotNull @DecimalMin("0.01") @Digits(integer=6,fraction=2) BigDecimal quantity){}
