package com.attendease.service;

import com.attendease.dto.AuthResponse;
import com.attendease.dto.LoginRequest;
import com.attendease.dto.RegisterRequest;
import com.attendease.entity.Role;
import com.attendease.entity.User;
import com.attendease.entity.UserRegistration;
import com.attendease.repository.UserRegistrationRepository;
import com.attendease.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final UserRegistrationRepository userRegistrationRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserRegistrationRepository userRegistrationRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRegistrationRepository = userRegistrationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Registration request is required");
        }

        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Password and confirm password must match");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        if (userRepository.existsByEmail(email) || userRegistrationRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProfileImage(request.getProfileImage());
        user.setRole(Role.USER);
        user.setActive(false);

        User saved = userRepository.save(user);

        return new AuthResponse(saved.getId(), saved.getFullName(), saved.getEmail(), saved.getRole().name(), saved.getProfileImage(), "Registration successful. Awaiting admin approval.");
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request is required");
        }

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        logger.info("Attempting login for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    logger.warn("No user found in users table for email={} during login; checking legacy user_registration table", email);
                    return userRegistrationRepository.findByEmail(email)
                            .map(this::toUser)
                            .orElse(null);
                });

        if (user == null) {
            logger.warn("No user found for email={} during login", email);
            throw new IllegalArgumentException("Invalid email or password");
        }

        logger.info("User found for login: id={}, storedPasswordStartsWithBcrypt={}, storedPasswordLength={}",
                user.getId(),
                user.getPassword() != null && user.getPassword().startsWith("$2"),
                user.getPassword() == null ? 0 : user.getPassword().length());

        if (!isPasswordValid(request.getPassword(), user.getPassword())) {
            logger.warn("Password mismatch for userId={}, email={}, rawPasswordProvided={} ", user.getId(), email, request.getPassword() != null);
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Your registration is awaiting administrator approval");
        }

        logger.info("Password validation succeeded for email={} using id={}", email, user.getId());
        return new AuthResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(), user.getProfileImage(), "Login successful");
    }

    private boolean isPasswordValid(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            logger.warn("Login password validation failed because rawPassword or storedPassword was null");
            return false;
        }

        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            logger.info("Password matched using BCrypt encoder");
            return true;
        }

        boolean matchesLegacyPlainText = rawPassword.equals(storedPassword);
        if (matchesLegacyPlainText) {
            logger.warn("Password matched using legacy plain-text fallback for stored password; database should be migrated to BCrypt");
        }

        return matchesLegacyPlainText;
    }

    private User toUser(UserRegistration registration) {
        User user = new User();
        user.setId(registration.getId());
        user.setFullName(registration.getFullName());
        user.setEmail(registration.getEmail());
        user.setPassword(registration.getPassword());
        user.setRole(Role.USER);
        user.setActive(false);
        return user;
    }
}
