package com.attendease.config;

import com.attendease.entity.Role;
import com.attendease.entity.User;
import com.attendease.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${attendease.admin.email:}")
    private String adminEmail;

    @Value("${attendease.admin.password:}")
    private String adminPassword;

    public AdminAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            return;
        }

        String normalizedEmail = adminEmail.trim();
        User admin = userRepository.findByEmail(normalizedEmail).orElseGet(() -> {
            User newAdmin = new User();
            newAdmin.setFullName("AttendEase Administrator");
            newAdmin.setEmail(normalizedEmail);
            newAdmin.setPassword(passwordEncoder.encode(adminPassword));
            newAdmin.setRole(Role.ADMIN);
            newAdmin.setActive(true);
            return userRepository.save(newAdmin);
        });

        boolean updated = false;
        if (admin.getRole() != Role.ADMIN || !admin.isActive()) {
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            updated = true;
        }
        if (!passwordEncoder.matches(adminPassword, admin.getPassword())) {
            admin.setPassword(passwordEncoder.encode(adminPassword));
            updated = true;
        }
        if (updated) {
            userRepository.save(admin);
        }
        logger.info("Configured administrator account is available for {}", normalizedEmail);
    }
}
