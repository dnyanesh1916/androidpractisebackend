package com.attendease;

import com.attendease.dto.LoginRequest;
import com.attendease.dto.PasswordResetWithCodeRequest;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        user.setPassword("Password@123");
        user.setRole(Role.USER);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUsername("user1");
        request.setPassword("Password@123");

        var response = authService.login(request);

        assertEquals("Login successful", response.getMessage());
    }

    @Test
    void shouldAllowConfiguredAdminEmailAndReturnGeneratedUsername() {
        User admin = new User();
        admin.setId(7L);
        admin.setFullName("AttendEase Administrator");
        admin.setEmail("admin@gmail.com");
        admin.setPassword(passwordEncoder.encode("AdminPassword@123"));
        admin.setRole(Role.ADMIN);
        admin.setActive(true);
        when(userRepository.findByEmail("admin@gmail.com")).thenReturn(Optional.of(admin));

        LoginRequest request = new LoginRequest();
        request.setUsername("admin@gmail.com");
        request.setPassword("AdminPassword@123");

        var response = authService.login(request);

        assertEquals("user7", response.getUsername());
        assertEquals("ADMIN", response.getRole());
    }

    @Test
    void shouldNotAllowRegularUserToLoginWithEmail() {
        User user = new User();
        user.setId(8L);
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setRole(Role.USER);
        user.setActive(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginRequest request = new LoginRequest();
        request.setUsername("user@example.com");
        request.setPassword("Password@123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void shouldNotLoginLegacyRegistrationBeforeAdministratorApproval() {
        UserRegistration legacyUser = new UserRegistration();
        legacyUser.setId(2L);
        legacyUser.setPassword("Password@123");

        when(userRepository.findById(2L)).thenReturn(Optional.empty());
        when(userRegistrationRepository.findById(2L)).thenReturn(Optional.of(legacyUser));

        LoginRequest request = new LoginRequest();
        request.setUsername("user2");
        request.setPassword("Password@123");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));

        assertEquals("Your registration is awaiting administrator approval", exception.getMessage());
    }

    @Test
    void shouldSaveNewRegistrationAsPendingApproval() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("New User");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));
        request.setPassword("Password@123");
        request.setConfirmPassword("Password@123");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        var response = authService.register(request);

        assertEquals("Registration successful. Your username is user3. Awaiting admin approval.", response.getMessage());
        assertEquals("user3", response.getUsername());
        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertFalse(savedUser.getValue().isActive());
        assertEquals(LocalDate.of(1990, 1, 1), savedUser.getValue().getDateOfBirth());
    }

    @Test
    void shouldResetPasswordAndConsumeValidRecoveryCode() {
        User user = new User();
        user.setId(3L);
        user.setRole(Role.USER);
        user.setPassword("old-password-hash");
        user.setPasswordResetCodeHash(passwordEncoder.encode("ABCD2345EFGH6789"));
        user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(userRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        PasswordResetWithCodeRequest request = recoveryRequest("ABCD2345EFGH6789");
        authService.resetPasswordWithCode(request);

        assertTrue(passwordEncoder.matches("NewPassword@123", user.getPassword()));
        assertNull(user.getPasswordResetCodeHash());
        assertNull(user.getPasswordResetExpiresAt());
        assertThrows(IllegalArgumentException.class, () -> authService.resetPasswordWithCode(request));
    }

    @Test
    void shouldRejectExpiredRecoveryCode() {
        User user = new User();
        user.setId(3L);
        user.setRole(Role.USER);
        user.setPasswordResetCodeHash(passwordEncoder.encode("ABCD2345EFGH6789"));
        user.setPasswordResetExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> authService.resetPasswordWithCode(recoveryRequest("ABCD2345EFGH6789")));

        assertEquals("Invalid or expired recovery code", exception.getMessage());
        verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    private PasswordResetWithCodeRequest recoveryRequest(String recoveryCode) {
        PasswordResetWithCodeRequest request = new PasswordResetWithCodeRequest();
        request.setUsername("user3");
        request.setRecoveryCode(recoveryCode);
        request.setPassword("NewPassword@123");
        request.setConfirmPassword("NewPassword@123");
        return request;
    }

}
