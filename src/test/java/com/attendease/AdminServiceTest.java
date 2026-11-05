package com.attendease;

import com.attendease.dto.CompensationTransactionRequest;
import com.attendease.entity.CompensationTransaction;
import com.attendease.entity.CompensationTransactionType;
import com.attendease.entity.Role;
import com.attendease.entity.User;
import com.attendease.repository.AdminActivityLogRepository;
import com.attendease.repository.AttendanceLocationRepository;
import com.attendease.repository.AttendanceRecordRepository;
import com.attendease.repository.CompensationTransactionRepository;
import com.attendease.repository.UserRegistrationRepository;
import com.attendease.repository.UserRepository;
import com.attendease.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserRegistrationRepository legacyRegistrationRepository;
    @Mock
    private AttendanceLocationRepository locationRepository;
    @Mock
    private AdminActivityLogRepository activityLogRepository;
    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;
    @Mock
    private CompensationTransactionRepository compensationTransactionRepository;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, legacyRegistrationRepository, locationRepository,
                activityLogRepository, attendanceRecordRepository, compensationTransactionRepository);
    }

    @Test
    void rejectsTransactionWhenDailySalaryIsNotSet() {
        User user = activeUser();
        user.setDailySalary(null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminService.addTransaction(2L, request(), "admin@example.com"));

        assertEquals("Set a daily salary before recording a salary payment or advance", exception.getMessage());
        verify(attendanceRecordRepository, never()).existsByUserIdAndCheckInTimeBetween(any(), any(), any());
        verify(compensationTransactionRepository, never()).save(any(CompensationTransaction.class));
    }

    @Test
    void rejectsTransactionWhenNoAttendanceWasMarkedThisMonth() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(activeUser()));
        when(attendanceRecordRepository.existsByUserIdAndCheckInTimeBetween(any(), any(), any())).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminService.addTransaction(2L, request(), "admin@example.com"));

        assertEquals("Mark attendance at least once this month before recording a salary payment or advance",
                exception.getMessage());
        verify(compensationTransactionRepository, never()).save(any(CompensationTransaction.class));
    }

    @Test
    void recordsTransactionWhenSalaryAndAttendanceExist() {
        User user = activeUser();
        User admin = new User();
        admin.setId(1L);
        admin.setEmail("admin@example.com");
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(admin));
        when(attendanceRecordRepository.existsByUserIdAndCheckInTimeBetween(any(), any(), any())).thenReturn(true);
        when(compensationTransactionRepository.save(any(CompensationTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompensationTransaction saved = adminService.addTransaction(2L, request(), "admin@example.com");

        assertEquals(2L, saved.getUserId());
        assertEquals(CompensationTransactionType.ADVANCE, saved.getType());
        verify(activityLogRepository).save(any());
    }

    private User activeUser() {
        User user = new User();
        user.setId(2L);
        user.setEmail("user@example.com");
        user.setRole(Role.USER);
        user.setActive(true);
        user.setDailySalary(new BigDecimal("100.00"));
        return user;
    }

    private CompensationTransactionRequest request() {
        CompensationTransactionRequest request = new CompensationTransactionRequest();
        request.setType(CompensationTransactionType.ADVANCE);
        request.setAmount(new BigDecimal("25.00"));
        request.setDescription("Travel advance");
        return request;
    }
}
