package com.gbkhealthcare.billing;
import jakarta.validation.constraints.*;
public record BillableServiceRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String code,@NotBlank @Size(max=160) String name,@Size(max=10000) String description,@NotBlank @Pattern(regexp="(?i)CONSULTATION|PROCEDURE|INVESTIGATION|OTHER") String category){}
