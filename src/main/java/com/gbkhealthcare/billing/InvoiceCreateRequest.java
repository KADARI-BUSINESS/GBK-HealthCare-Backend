package com.gbkhealthcare.billing;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.List;
public record InvoiceCreateRequest(@NotEmpty @Size(max=50) List<@NotNull @Valid InvoiceLineRequest> lines,@NotNull @DecimalMin("0.00") @Digits(integer=10,fraction=2) BigDecimal discountAmount){}
