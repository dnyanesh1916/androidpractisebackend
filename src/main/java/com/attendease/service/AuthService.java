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

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^user([1-9][0-9]*)$");

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

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail("user-" + UUID.randomUUID() + "@attendease.invalid");
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProfileImage(request.getProfileImage());
        user.setRole(Role.USER);
        user.setActive(false);

        User saved = userRepository.save(user);

        return new AuthResponse(saved.getId(), saved.getFullName(), null, saved.getRole().name(),
                saved.getProfileImage(), "Registration successful. Your username is " + saved.getUsername()
                + ". Awaiting admin approval.");
    }

    public AuthResponse login(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Login request is required");
        }

        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        Matcher matcher = USERNAME_PATTERN.matcher(username);
        logger.info("Attempting login for username={}", username);

        User user;
        if (matcher.matches()) {
            long userId;
            try {
                userId = Long.parseLong(matcher.group(1));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid username or password");
            }
            user = userRepository.findById(userId).orElse(null);
        } else {
            user = userRepository.findByEmail(username)
                    .filter(candidate -> candidate.getRole() == Role.ADMIN)
                    .orElse(null);
        }

        if (user == null) {
            UserRegistration registration = matcher.matches()
                    ? userRegistrationRepository.findById(Long.parseLong(matcher.group(1))).orElse(null)
                    : null;
            if (registration != null && isPasswordValid(request.getPassword(), registration.getPassword())) {
                throw new IllegalArgumentException("Your registration is awaiting administrator approval");
            }
            logger.warn("No user found for username={}", username);
            throw new IllegalArgumentException("Invalid username or password");
        }

        logger.info("User found for login: id={}, storedPasswordStartsWithBcrypt={}, storedPasswordLength={}",
                user.getId(),
                user.getPassword() != null && user.getPassword().startsWith("$2"),
                user.getPassword() == null ? 0 : user.getPassword().length());

        if (!isPasswordValid(request.getPassword(), user.getPassword())) {
            logger.warn("Password mismatch for username={}, rawPasswordProvided={}", username, request.getPassword() != null);
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!user.isActive()) {
            throw new IllegalArgumentException("Your registration is awaiting administrator approval");
        }

        logger.info("Password validation succeeded for username={} using id={}", username, user.getId());
        return new AuthResponse(user.getId(), user.getFullName(), null, user.getRole().name(),
                user.getProfileImage(), "Login successful");
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

}
