package com.attendease.service;

import com.attendease.dto.AttendanceLocationRequest;
import com.attendease.dto.CompensationTransactionRequest;
import com.attendease.dto.SalaryUpdateRequest;
import com.attendease.dto.UserSummary;
import com.attendease.entity.AdminActivityLog;
import com.attendease.entity.AttendanceLocation;
import com.attendease.entity.AttendanceRecord;
import com.attendease.entity.CompensationTransaction;
import com.attendease.entity.CompensationTransactionType;
import com.attendease.entity.Role;
import com.attendease.entity.User;
import com.attendease.entity.UserRegistration;
import com.attendease.repository.AdminActivityLogRepository;
import com.attendease.repository.AttendanceRecordRepository;
import com.attendease.repository.CompensationTransactionRepository;
import com.attendease.repository.AttendanceLocationRepository;
import com.attendease.repository.UserRegistrationRepository;
import com.attendease.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final UserRegistrationRepository legacyRegistrationRepository;
    private final AttendanceLocationRepository locationRepository;
    private final AdminActivityLogRepository activityLogRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CompensationTransactionRepository compensationTransactionRepository;

    @Value("${attendease.company.latitude:12.9716}")
    private double defaultLatitude;

    @Value("${attendease.company.longitude:77.5946}")
    private double defaultLongitude;

    @Value("${attendease.company.radius-meters:200}")
    private int defaultRadiusMeters;

    public AdminService(UserRepository userRepository,
                        UserRegistrationRepository legacyRegistrationRepository,
                        AttendanceLocationRepository locationRepository,
                        AdminActivityLogRepository activityLogRepository,
                        AttendanceRecordRepository attendanceRecordRepository,
                        CompensationTransactionRepository compensationTransactionRepository) {
        this.userRepository = userRepository;
        this.legacyRegistrationRepository = legacyRegistrationRepository;
        this.locationRepository = locationRepository;
        this.activityLogRepository = activityLogRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.compensationTransactionRepository = compensationTransactionRepository;
    }

    @PostConstruct
    void initializeDefaultLocation() {
        if (locationRepository.count() == 0) {
            AttendanceLocation location = new AttendanceLocation();
            location.setName("Primary office");
            location.setAddress("Initial location from application configuration");
            location.setLatitude(defaultLatitude);
            location.setLongitude(defaultLongitude);
            location.setRadiusMeters(defaultRadiusMeters);
            location.setActive(true);
            locationRepository.save(location);
            logger.info("Created initial attendance location from application configuration");
        }
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getUsers(int year, int month) {
        YearMonth selectedMonth;
        try {
            selectedMonth = YearMonth.of(year, month);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Select a valid year and month");
        }

        LocalDateTime start = selectedMonth.atDay(1).atStartOfDay();
        LocalDateTime end = selectedMonth.plusMonths(1).atDay(1).atStartOfDay().minusNanos(1);
        Map<Long, Set<LocalDate>> workedDates = new HashMap<>();
        Map<Long, BigDecimal> advances = new HashMap<>();
        Map<Long, BigDecimal> salaryPayments = new HashMap<>();
        for (AttendanceRecord record : attendanceRecordRepository.findByCheckInTimeBetweenOrderByCheckInTimeDesc(start, end)) {
            if (record.getCheckInTime() != null) {
                workedDates.computeIfAbsent(record.getUserId(), ignored -> new HashSet<>())
                        .add(record.getCheckInTime().toLocalDate());
            }
        }
            for (CompensationTransaction transaction : compensationTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)) {
                Map<Long, BigDecimal> totals = transaction.getType() == CompensationTransactionType.ADVANCE
                    ? advances : salaryPayments;
                totals.merge(transaction.getUserId(), transaction.getAmount(), BigDecimal::add);
            }

        List<UserSummary> users = new ArrayList<>(userRepository.findAll().stream()
                .map(user -> toSummary(user, workedDates.getOrDefault(user.getId(), Set.of()).size(), selectedMonth,
                    advances.get(user.getId()), salaryPayments.getOrDefault(user.getId(), BigDecimal.ZERO)))
                .toList());

        legacyRegistrationRepository.findAll().stream()
                .filter(registration -> !userRepository.existsByEmail(registration.getEmail()))
                .map(this::toLegacySummary)
                .forEach(users::add);

        users.sort(Comparator.comparing(UserSummary::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return users;
    }

    @Transactional(readOnly = true)
    public List<CompensationTransaction> getTransactions(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found");
        }
        return compensationTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public CompensationTransaction addTransaction(Long userId, CompensationTransactionRequest request,
                                                  String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Compensation transactions can only be recorded for regular users");
        }
        if (!user.isActive()) {
            throw new IllegalArgumentException("Approve the user before recording compensation transactions");
        }
        if (user.getDailySalary() == null) {
            throw new IllegalArgumentException("Set a daily salary before recording a salary payment or advance");
        }
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime monthStart = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime nextMonthStart = currentMonth.plusMonths(1).atDay(1).atStartOfDay();
        if (!attendanceRecordRepository.existsByUserIdAndCheckInTimeBetween(
                userId, monthStart, nextMonthStart.minusNanos(1))) {
            throw new IllegalArgumentException("Mark attendance at least once this month before recording a salary payment or advance");
        }
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated administrator was not found"));
        CompensationTransaction transaction = new CompensationTransaction();
        transaction.setUserId(user.getId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription().trim());
        transaction.setRecordedByEmail(admin.getEmail());
        CompensationTransaction saved = compensationTransactionRepository.save(transaction);
        String action = request.getType() == CompensationTransactionType.ADVANCE
                ? "ADVANCE_RECORDED" : "SALARY_PAYMENT_RECORDED";
        recordActivity(adminEmail, action, request.getType() + " of " + request.getAmount() + " for "
            + user.getEmail() + ": " + request.getDescription().trim());
        logger.info("Admin {} recorded compensation transaction id={} for userId={}", adminEmail, saved.getId(), userId);
        return saved;
    }

    @Transactional
    public UserSummary updateSalary(Long userId, SalaryUpdateRequest request, String adminEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Salary can only be assigned to regular user accounts");
        }
        user.setDailySalary(request.getDailySalary());
        User saved = userRepository.save(user);
        recordActivity(adminEmail, "USER_DAILY_SALARY_UPDATED", "Updated daily salary for " + saved.getEmail());
        logger.info("Admin {} updated daily salary for userId={}", adminEmail, userId);
        YearMonth currentMonth = YearMonth.now();
        return getUserSummary(userId, currentMonth.getYear(), currentMonth.getMonthValue());
    }

    @Transactional(readOnly = true)
    public UserSummary getUserSummary(Long userId, int year, int month) {
        YearMonth selectedMonth;
        try {
            selectedMonth = YearMonth.of(year, month);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Select a valid year and month");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        LocalDateTime start = selectedMonth.atDay(1).atStartOfDay();
        LocalDateTime end = selectedMonth.plusMonths(1).atDay(1).atStartOfDay().minusNanos(1);
        Set<LocalDate> workedDates = new HashSet<>();
        for (AttendanceRecord record : attendanceRecordRepository.findByUserIdOrderByCheckInTimeDesc(userId)) {
            if (record.getCheckInTime() != null
                    && !record.getCheckInTime().isBefore(start)
                    && !record.getCheckInTime().isAfter(end)) {
                workedDates.add(record.getCheckInTime().toLocalDate());
            }
        }
        BigDecimal advance = null;
        BigDecimal salaryPaid = BigDecimal.ZERO;
        for (CompensationTransaction transaction : compensationTransactionRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)) {
            if (!transaction.getUserId().equals(userId)) {
                continue;
            }
            if (transaction.getType() == CompensationTransactionType.ADVANCE) {
                advance = advance == null ? transaction.getAmount() : advance.add(transaction.getAmount());
            } else {
                salaryPaid = salaryPaid.add(transaction.getAmount());
            }
        }
        return toSummary(user, workedDates.size(), selectedMonth, advance, salaryPaid);
    }

    @Transactional
    public UserSummary approveUser(Long userId, String source, String adminEmail, BigDecimal dailySalary) {
        if ("LEGACY".equalsIgnoreCase(source)) {
            return approveLegacyUser(userId, adminEmail, dailySalary);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Only regular user registrations can be approved");
        }
        boolean changed = false;
        if (!user.isActive()) {
            user.setActive(true);
            changed = true;
        }
        if (dailySalary != null) {
            user.setDailySalary(dailySalary);
            changed = true;
        }
        if (changed) {
            user = userRepository.save(user);
            recordActivity(adminEmail, "USER_APPROVED", "Approved registration for " + user.getEmail());
            logger.info("Admin {} approved registration for userId={}", adminEmail, userId);
        }
        return toSummary(user);
    }

    private UserSummary approveLegacyUser(Long registrationId, String adminEmail, BigDecimal dailySalary) {
        UserRegistration registration = legacyRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        if (userRepository.existsByEmail(registration.getEmail())) {
            throw new IllegalArgumentException("A user account already exists for this email");
        }

        User user = new User();
        user.setFullName(registration.getFullName());
        user.setEmail(registration.getEmail());
        user.setPassword(registration.getPassword());
        user.setRole(Role.USER);
        user.setActive(true);
        user.setDailySalary(dailySalary);
        User saved = userRepository.save(user);
        legacyRegistrationRepository.delete(registration);
        recordActivity(adminEmail, "USER_APPROVED", "Approved legacy registration for " + saved.getEmail());
        logger.info("Admin {} approved legacy registration id={} as userId={}", adminEmail, registrationId, saved.getId());
        return toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceLocation> getLocations() {
        return locationRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public AttendanceLocation createLocation(AttendanceLocationRequest request, String adminEmail) {
        AttendanceLocation location = new AttendanceLocation();
        applyLocationRequest(location, request);
        AttendanceLocation saved = locationRepository.save(location);
        recordActivity(adminEmail, "LOCATION_CREATED", "Created attendance location " + saved.getName());
        logger.info("Admin {} created attendance location id={}", adminEmail, saved.getId());
        return saved;
    }

    @Transactional
    public AttendanceLocation updateLocation(Long locationId, AttendanceLocationRequest request, String adminEmail) {
        AttendanceLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance location not found"));
        applyLocationRequest(location, request);
        AttendanceLocation saved = locationRepository.save(location);
        recordActivity(adminEmail, "LOCATION_UPDATED", "Updated attendance location " + saved.getName());
        logger.info("Admin {} updated attendance location id={}", adminEmail, locationId);
        return saved;
    }

    @Transactional
    public void deactivateLocation(Long locationId, String adminEmail) {
        AttendanceLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance location not found"));
        location.setActive(false);
        locationRepository.save(location);
        recordActivity(adminEmail, "LOCATION_DEACTIVATED", "Deactivated attendance location " + location.getName());
        logger.info("Admin {} deactivated attendance location id={}", adminEmail, locationId);
    }

    @Transactional(readOnly = true)
    public List<AdminActivityLog> getActivity() {
        return activityLogRepository.findAllByOrderByCreatedAtDesc();
    }

    private void applyLocationRequest(AttendanceLocation location, AttendanceLocationRequest request) {
        location.setName(request.getName().trim());
        location.setAddress(request.getAddress() == null ? "" : request.getAddress().trim());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setRadiusMeters(request.getRadiusMeters());
        location.setActive(request.isActive());
    }

    private UserSummary toSummary(User user) {
        return toSummary(user, 0, YearMonth.now(), null, BigDecimal.ZERO);
    }

    private UserSummary toSummary(User user, int workingDaysThisMonth, YearMonth selectedMonth,
                                  BigDecimal advancesFromLedger, BigDecimal salaryPaid) {
        int scheduledWorkingDays = countWeekdays(selectedMonth);
        BigDecimal advance = advancesFromLedger != null ? advancesFromLedger
                : user.getMonthlyAdvance() == null ? BigDecimal.ZERO : user.getMonthlyAdvance();
        BigDecimal earnedPay = user.getDailySalary() == null ? null
            : user.getDailySalary().multiply(BigDecimal.valueOf(workingDaysThisMonth))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBalance = earnedPay == null ? null : earnedPay.subtract(advance);
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(),
            user.getProfileImage(), user.isActive(), user.getCreatedAt(), user.getDailySalary(), advance,
            workingDaysThisMonth, scheduledWorkingDays, salaryPaid, earnedPay, totalBalance);
    }

    private int countWeekdays(YearMonth month) {
        int weekdays = 0;
        for (LocalDate day = month.atDay(1); !day.isAfter(month.atEndOfMonth()); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                weekdays++;
            }
        }
        return weekdays;
    }

    private UserSummary toLegacySummary(UserRegistration registration) {
        UserSummary summary = new UserSummary(registration.getId(), registration.getFullName(),
                registration.getEmail(), Role.USER.name(), null, false, registration.getCreatedAt());
        summary.setSource("LEGACY");
        return summary;
    }

    private void recordActivity(String adminEmail, String action, String details) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated administrator was not found"));
        AdminActivityLog activity = new AdminActivityLog();
        activity.setAdminId(admin.getId());
        activity.setAction(action);
        activity.setDetails(details);
        activityLogRepository.save(activity);
    }
}
