package com.attendease;

import com.attendease.dto.LoginRequest;
import com.attendease.entity.Role;
import com.attendease.entity.User;
import com.attendease.entity.UserRegistration;
import com.attendease.repository.UserRegistrationRepository;
import com.attendease.repository.UserRepository;
import com.attendease.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRegistrationRepository userRegistrationRepository;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        authService = new AuthService(userRepository, userRegistrationRepository, passwordEncoder);
    }

    @Test
    void shouldLoginWhenStoredPasswordIsLegacyPlainText() {
        User user = new User();
        user.setId(1L);
        user.setFullName("Jane Doe");
        user.setEmail("jane@example.com");
        user.setPassword("Password@123");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setEmail("jane@example.com");
        request.setPassword("Password@123");

        var response = authService.login(request);

        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void shouldLoginWhenUserExistsInLegacyRegistrationTable() {
        UserRegistration legacyUser = new UserRegistration();
        legacyUser.setId(2L);
        legacyUser.setFullName("Sahil Sharma");
        legacyUser.setEmail("sahil@gmail.com");
        legacyUser.setPassword("Password@123");

        when(userRepository.findByEmail("sahil@gmail.com")).thenReturn(Optional.empty());
        when(userRegistrationRepository.findByEmail("sahil@gmail.com")).thenReturn(Optional.of(legacyUser));

        LoginRequest request = new LoginRequest();
        request.setEmail("sahil@gmail.com");
        request.setPassword("Password@123");

        var response = authService.login(request);

        assertEquals("Login successful", response.getMessage());
    }
}
