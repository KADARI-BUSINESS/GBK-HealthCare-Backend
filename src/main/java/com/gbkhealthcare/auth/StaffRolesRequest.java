package com.gbkhealthcare.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record StaffRolesRequest(@NotNull @Size(max = 3) Set<StaffRole> roles) {}
