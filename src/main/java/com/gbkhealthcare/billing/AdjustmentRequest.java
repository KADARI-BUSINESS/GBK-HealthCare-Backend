package com.gbkhealthcare.billing;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record AdjustmentRequest(@NotNull @DecimalMin(value="-99999999.99") @DecimalMax(value="99999999.99") @Digits(integer=10,fraction=2) BigDecimal amount,@NotBlank @Size(max=1000) String reason){}
