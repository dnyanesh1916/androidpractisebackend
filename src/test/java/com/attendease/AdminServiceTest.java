package com.attendease;

import com.attendease.dto.CompensationTransactionRequest;
import com.attendease.entity.CompensationTransaction;
import com.attendease.entity.CompensationTransactionType;
import com.attendease.entity.AttendanceRecord;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(attendanceRecordRepository.existsByUserIdAndCheckInTimeBetween(any(), any(), any())).thenReturn(true);
        when(compensationTransactionRepository.save(any(CompensationTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CompensationTransaction saved = adminService.addTransaction(2L, request(), "user1");

        assertEquals(2L, saved.getUserId());
        assertEquals(CompensationTransactionType.ADVANCE, saved.getType());
        verify(activityLogRepository).save(any());
    }

    @Test
    void requiresDailySalaryBeforeApprovingUser() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> adminService.approveUser(2L, "USER", "user1", null));

        assertEquals("A daily salary greater than zero is required", exception.getMessage());
    }

    @Test
    void subtractsSalaryPaidFromMonthlyBalance() {
        User user = activeUser();
        user.setId(2L);
        AttendanceRecord attendance = new AttendanceRecord();
        attendance.setCheckInTime(LocalDate.now().withDayOfMonth(1).atStartOfDay());

        CompensationTransaction advance = new CompensationTransaction();
        advance.setUserId(2L);
        advance.setType(CompensationTransactionType.ADVANCE);
        advance.setAmount(new BigDecimal("10.00"));
        CompensationTransaction salary = new CompensationTransaction();
        salary.setUserId(2L);
        salary.setType(CompensationTransactionType.SALARY_PAYMENT);
        salary.setAmount(new BigDecimal("20.00"));

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(attendanceRecordRepository.findByUserIdOrderByCheckInTimeDesc(2L)).thenReturn(List.of(attendance));
        when(compensationTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(any(), any()))
                .thenReturn(List.of(advance, salary));

        var summary = adminService.getUserSummary(2L, LocalDate.now().getYear(), LocalDate.now().getMonthValue());

        assertEquals(new BigDecimal("70.00"), summary.getTotalBalanceThisMonth());
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
