package com.gbkhealthcare.billing;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record ServicePriceRequest(@NotNull @DecimalMin("0.00") @Digits(integer=8,fraction=2) BigDecimal amount,@NotNull @DecimalMin("0.00") @DecimalMax("100.00") @Digits(integer=3,fraction=2) BigDecimal taxRatePercent){}
