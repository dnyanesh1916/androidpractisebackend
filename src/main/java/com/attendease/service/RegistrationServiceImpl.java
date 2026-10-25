package com.attendease.service;

import com.attendease.dto.RegisterRequest;
import com.attendease.entity.UserRegistration;
import com.attendease.repository.UserRegistrationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRegistrationRepository repository;
    private final PasswordEncoder passwordEncoder;

    public RegistrationServiceImpl(UserRegistrationRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserRegistration register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request is required");
        }

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password must match");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        if (repository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        UserRegistration user = new UserRegistration();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        return repository.save(user);
    }

    @Override
    public java.util.List<UserRegistration> getAllRegistrations() {
        return repository.findAll();
    }
}
