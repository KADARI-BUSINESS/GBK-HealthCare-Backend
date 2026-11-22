package com.gbkhealthcare.billing;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record RefundRequest(@NotNull @DecimalMin("0.01") @Digits(integer=10,fraction=2) BigDecimal amount,@NotBlank @Size(max=1000) String reason,@Size(max=180) String refundReference){}
