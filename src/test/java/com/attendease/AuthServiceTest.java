package com.attendease;

import com.attendease.dto.LoginRequest;
import com.attendease.dto.RegisterRequest;
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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    void shouldNotLoginLegacyRegistrationBeforeAdministratorApproval() {
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

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Your registration is awaiting administrator approval", exception.getMessage());
    }

    @Test
    void shouldSaveNewRegistrationAsPendingApproval() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("New User");
        request.setEmail("new@example.com");
        request.setPassword("Password@123");
        request.setConfirmPassword("Password@123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        assertEquals("Registration successful. Awaiting admin approval.", response.getMessage());
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertFalse(savedUser.getValue().isActive());
    }
}
