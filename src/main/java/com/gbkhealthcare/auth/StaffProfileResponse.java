package com.gbkhealthcare.auth;

import java.util.Set;

public record StaffProfileResponse(Long staffId, String fullName, String email, String mobileNumber,
        Set<StaffRole> roles) {}
