package com.attendease.service;

import com.attendease.dto.RegisterRequest;
import com.attendease.entity.UserRegistration;

import java.util.List;

public interface RegistrationService {
    UserRegistration register(RegisterRequest request);
    List<UserRegistration> getAllRegistrations();
}
