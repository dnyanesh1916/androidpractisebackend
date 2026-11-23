package com.attendease.service;

import com.attendease.dto.AttendanceLocationRequest;
import com.attendease.dto.CompensationTransactionRequest;
import com.attendease.dto.RecoveryCodeResponse;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^user([1-9][0-9]*)$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String RECOVERY_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int RECOVERY_CODE_LENGTH = 16;

    private static final Duration RECOVERY_CODE_LIFETIME = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final UserRegistrationRepository legacyRegistrationRepository;
    private final AttendanceLocationRepository locationRepository;
    private final AdminActivityLogRepository activityLogRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final CompensationTransactionRepository compensationTransactionRepository;
    private final PasswordEncoder passwordEncoder;

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
                        CompensationTransactionRepository compensationTransactionRepository,
                        PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.legacyRegistrationRepository = legacyRegistrationRepository;
        this.locationRepository = locationRepository;
        this.activityLogRepository = activityLogRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.compensationTransactionRepository = compensationTransactionRepository;
        this.passwordEncoder = passwordEncoder;
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
        return getUsers(year, month, null);
    }

    @Transactional(readOnly = true)
    public List<UserSummary> getUsers(int year, int month, String locationName) {
        YearMonth selectedMonth;
        try {
            selectedMonth = YearMonth.of(year, month);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Select a valid year and month");
        }

        LocalDateTime start = selectedMonth.atDay(1).atStartOfDay();
        LocalDateTime end = selectedMonth.plusMonths(1).atDay(1).atStartOfDay().minusNanos(1);
        AttendanceLocation selectedLocation = locationName == null || locationName.isBlank() ? null
                : locationRepository.findAllByActiveTrue().stream()
                        .filter(location -> location.getName().equals(locationName))
                        .findFirst()
                        .orElse(null);
        if (locationName != null && !locationName.isBlank() && selectedLocation == null) {
            throw new IllegalArgumentException("Active attendance location not found");
        }
        List<AttendanceLocation> knownLocations = new ArrayList<>(locationRepository.findAllByOrderByNameAsc());
        if (selectedLocation != null
                && knownLocations.stream().noneMatch(location -> location.getId().equals(selectedLocation.getId()))) {
            knownLocations.add(selectedLocation);
        }
        Map<Long, Set<LocalDate>> workedDates = new HashMap<>();
        Map<Long, String> locationNames = new HashMap<>();
        Map<Long, BigDecimal> advances = new HashMap<>();
        Map<Long, BigDecimal> salaryPayments = new HashMap<>();
        for (AttendanceRecord record : attendanceRecordRepository.findByCheckInTimeBetweenOrderByCheckInTimeDesc(start, end)) {
            if (record.getCheckInTime() == null) {
                continue;
            }
            AttendanceLocation recordedLocation = resolveAttendanceLocation(record, knownLocations);
            String recordedLocationName = recordedLocation == null
                    ? record.getLocationName() : recordedLocation.getName();
            boolean matchesLocation = selectedLocation == null
                    || (recordedLocation != null && selectedLocation.getId().equals(recordedLocation.getId()))
                    || (recordedLocation == null && selectedLocation.getName().equals(record.getLocationName()));
            if (matchesLocation) {
                workedDates.computeIfAbsent(record.getUserId(), ignored -> new HashSet<>())
                        .add(record.getCheckInTime().toLocalDate());
                if (recordedLocationName != null) {
                    locationNames.putIfAbsent(record.getUserId(), recordedLocationName);
                }
            }
        }
            for (CompensationTransaction transaction : compensationTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end)) {
                Map<Long, BigDecimal> totals = transaction.getType() == CompensationTransactionType.ADVANCE
                    ? advances : salaryPayments;
                totals.merge(transaction.getUserId(), transaction.getAmount(), BigDecimal::add);
            }

        List<UserSummary> users = new ArrayList<>(userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.USER)
                .filter(user -> selectedLocation == null || workedDates.containsKey(user.getId()))
                .map(user -> {
                    UserSummary summary = toSummary(user, workedDates.getOrDefault(user.getId(), Set.of()).size(),
                            selectedMonth, advances.get(user.getId()),
                            salaryPayments.getOrDefault(user.getId(), BigDecimal.ZERO));
                    summary.setLocationName(locationNames.get(user.getId()));
                    return summary;
                })
                .toList());

        if (selectedLocation == null) {
            legacyRegistrationRepository.findAll().stream()
                    .filter(registration -> registration.getEmail() == null
                            || !userRepository.existsByEmail(registration.getEmail()))
                    .map(this::toLegacySummary)
                    .forEach(users::add);
        }

        users.sort(Comparator.comparing(UserSummary::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return users;
    }

    private AttendanceLocation resolveAttendanceLocation(AttendanceRecord record, List<AttendanceLocation> locations) {
        if (record.getLocationId() != null) {
            return locations.stream()
                    .filter(location -> location.getId().equals(record.getLocationId()))
                    .findFirst()
                    .orElse(null);
        }
        if (record.getLocationName() != null && !record.getLocationName().isBlank()) {
            return locations.stream()
                    .filter(location -> location.getName().equals(record.getLocationName()))
                    .findFirst()
                    .orElse(null);
        }
        if (!record.isLocationVerified()) {
            return null;
        }
        return locations.stream()
                .filter(location -> isWithinRadius(record.getCheckInLatitude(), record.getCheckInLongitude(),
                        location.getLatitude(), location.getLongitude(), location.getRadiusMeters()))
                .min(Comparator.comparingDouble(location -> distanceMeters(
                        record.getCheckInLatitude(), record.getCheckInLongitude(),
                        location.getLatitude(), location.getLongitude())))
                .orElse(null);
    }

    private double distanceMeters(double latitude, double longitude, double centerLatitude, double centerLongitude) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(latitude - centerLatitude);
        double dLng = Math.toRadians(longitude - centerLongitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(centerLatitude)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private boolean isWithinRadius(double latitude, double longitude, double centerLatitude,
                                   double centerLongitude, int allowedRadiusMeters) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(latitude - centerLatitude);
        double dLng = Math.toRadians(longitude - centerLongitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(centerLatitude)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double distance = 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return distance <= allowedRadiusMeters;
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
                                                  String adminUsername) {
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
        User admin = findAdmin(adminUsername);
        CompensationTransaction transaction = new CompensationTransaction();
        transaction.setUserId(user.getId());
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription().trim());
        transaction.setRecordedByEmail(admin.getEmail() == null ? admin.getUsername() : admin.getEmail());
        CompensationTransaction saved = compensationTransactionRepository.save(transaction);
        String action = request.getType() == CompensationTransactionType.ADVANCE
                ? "ADVANCE_RECORDED" : "SALARY_PAYMENT_RECORDED";
        recordActivity(adminUsername, action, request.getType() + " of " + request.getAmount() + " for "
            + user.getUsername() + ": " + request.getDescription().trim());
        logger.info("Admin {} recorded compensation transaction id={} for userId={}", adminUsername, saved.getId(), userId);
        return saved;
    }

    @Transactional
    public UserSummary updateSalary(Long userId, SalaryUpdateRequest request, String adminUsername) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Salary can only be assigned to regular user accounts");
        }
        requireDailySalary(request.getDailySalary());
        user.setDailySalary(request.getDailySalary());
        User saved = userRepository.save(user);
        recordActivity(adminUsername, "USER_DAILY_SALARY_UPDATED", "Updated daily salary for " + saved.getUsername());
        logger.info("Admin {} updated daily salary for userId={}", adminUsername, userId);
        YearMonth currentMonth = YearMonth.now();
        return getUserSummary(userId, currentMonth.getYear(), currentMonth.getMonthValue());
    }

    @Transactional
    public RecoveryCodeResponse issuePasswordRecoveryCode(Long userId, String adminUsername) {
        User user = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getRole() != Role.USER) {
            throw new IllegalArgumentException("Recovery codes can only be issued for regular users");
        }

        String recoveryCode = generateRecoveryCode();
        user.setPasswordResetCodeHash(passwordEncoder.encode(recoveryCode));
        user.setPasswordResetExpiresAt(LocalDateTime.now().plus(RECOVERY_CODE_LIFETIME));
        userRepository.save(user);
        recordActivity(adminUsername, "USER_RECOVERY_CODE_ISSUED",
                "Issued a one-time password recovery code for " + user.getUsername());
        logger.info("Admin {} issued a password recovery code for userId={}", adminUsername, userId);
        return new RecoveryCodeResponse(recoveryCode);
    }

    private String generateRecoveryCode() {
        StringBuilder code = new StringBuilder(RECOVERY_CODE_LENGTH);
        for (int index = 0; index < RECOVERY_CODE_LENGTH; index++) {
            code.append(RECOVERY_CODE_ALPHABET.charAt(SECURE_RANDOM.nextInt(RECOVERY_CODE_ALPHABET.length())));
        }
        return code.toString();
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
    public UserSummary approveUser(Long userId, String source, String adminUsername, BigDecimal dailySalary) {
        requireDailySalary(dailySalary);
        if ("LEGACY".equalsIgnoreCase(source)) {
            return approveLegacyUser(userId, adminUsername, dailySalary);
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
            recordActivity(adminUsername, "USER_APPROVED", "Approved registration for " + user.getUsername());
            logger.info("Admin {} approved registration for userId={}", adminUsername, userId);
        }
        return toSummary(user);
    }

    private UserSummary approveLegacyUser(Long registrationId, String adminUsername, BigDecimal dailySalary) {
        UserRegistration registration = legacyRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        if (registration.getEmail() != null && userRepository.existsByEmail(registration.getEmail())) {
            throw new IllegalArgumentException("A user account already exists for this email");
        }

        User user = new User();
        user.setFullName(registration.getFullName());
        user.setEmail(registration.getEmail());
        user.setDateOfBirth(registration.getDateOfBirth());
        user.setPassword(registration.getPassword());
        user.setRole(Role.USER);
        user.setActive(true);
        user.setDailySalary(dailySalary);
        User saved = userRepository.save(user);
        legacyRegistrationRepository.delete(registration);
        recordActivity(adminUsername, "USER_APPROVED", "Approved legacy registration for " + saved.getUsername());
        logger.info("Admin {} approved legacy registration id={} as userId={}", adminUsername, registrationId, saved.getId());
        return toSummary(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceLocation> getLocations() {
        return locationRepository.findAllByOrderByNameAsc();
    }

    @Transactional
    public AttendanceLocation createLocation(AttendanceLocationRequest request, String adminUsername) {
        AttendanceLocation location = new AttendanceLocation();
        applyLocationRequest(location, request);
        AttendanceLocation saved = locationRepository.save(location);
        recordActivity(adminUsername, "LOCATION_CREATED", "Created attendance location " + saved.getName());
        logger.info("Admin {} created attendance location id={}", adminUsername, saved.getId());
        return saved;
    }

    @Transactional
    public AttendanceLocation updateLocation(Long locationId, AttendanceLocationRequest request, String adminUsername) {
        AttendanceLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance location not found"));
        applyLocationRequest(location, request);
        AttendanceLocation saved = locationRepository.save(location);
        recordActivity(adminUsername, "LOCATION_UPDATED", "Updated attendance location " + saved.getName());
        logger.info("Admin {} updated attendance location id={}", adminUsername, locationId);
        return saved;
    }

    @Transactional
    public void deactivateLocation(Long locationId, String adminUsername) {
        AttendanceLocation location = locationRepository.findById(locationId)
                .orElseThrow(() -> new IllegalArgumentException("Attendance location not found"));
        location.setActive(false);
        locationRepository.save(location);
        recordActivity(adminUsername, "LOCATION_DEACTIVATED", "Deactivated attendance location " + location.getName());
        logger.info("Admin {} deactivated attendance location id={}", adminUsername, locationId);
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
        BigDecimal totalBalance = earnedPay == null ? null : earnedPay.subtract(advance).subtract(salaryPaid);
        return new UserSummary(user.getId(), user.getFullName(), null, user.getRole().name(),
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
                null, Role.USER.name(), null, false, registration.getCreatedAt());
        summary.setSource("LEGACY");
        return summary;
    }

    private void requireDailySalary(BigDecimal dailySalary) {
        if (dailySalary == null || dailySalary.signum() <= 0) {
            throw new IllegalArgumentException("A daily salary greater than zero is required");
        }
    }

    private User findAdmin(String adminUsername) {
        Matcher matcher = USERNAME_PATTERN.matcher(adminUsername == null ? "" : adminUsername);
        if (!matcher.matches()) {
            if (adminUsername == null || adminUsername.isBlank()) {
                throw new IllegalStateException("Authenticated administrator was not found");
            }
            return userRepository.findByEmail(adminUsername)
                    .filter(admin -> admin.getRole() == Role.ADMIN)
                    .orElseThrow(() -> new IllegalStateException("Authenticated administrator was not found"));
        }
        try {
            User admin = userRepository.findById(Long.parseLong(matcher.group(1)))
                    .orElseThrow(() -> new IllegalStateException("Authenticated administrator was not found"));
            if (admin.getRole() != Role.ADMIN) {
                throw new IllegalStateException("Authenticated administrator was not found");
            }
            return admin;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("Authenticated administrator was not found");
        }
    }

    private void recordActivity(String adminUsername, String action, String details) {
        User admin = findAdmin(adminUsername);
        AdminActivityLog activity = new AdminActivityLog();
        activity.setAdminId(admin.getId());
        activity.setAction(action);
        activity.setDetails(details);
        activityLogRepository.save(activity);
    }
}
