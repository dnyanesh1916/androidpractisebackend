package com.attendease.service;

import com.attendease.dto.AdminSummary;
import com.attendease.dto.AttendanceRequest;
import com.attendease.dto.AttendanceResponse;
import com.attendease.dto.UserSummary;
import com.attendease.entity.AdminActivityLog;
import com.attendease.entity.AttendanceLocation;
import com.attendease.entity.AttendanceRecord;
import com.attendease.entity.User;
import com.attendease.repository.AdminActivityLogRepository;
import com.attendease.repository.AttendanceLocationRepository;
import com.attendease.repository.AttendanceRecordRepository;
import com.attendease.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final AttendanceLocationRepository attendanceLocationRepository;
    private final UserRepository userRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;

    @Value("${attendease.company.latitude:12.9716}")
    private double companyLatitude;

    @Value("${attendease.company.longitude:77.5946}")
    private double companyLongitude;

    @Value("${attendease.company.radius-meters:200}")
    private int radiusMeters;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
                             AttendanceLocationRepository attendanceLocationRepository,
                             UserRepository userRepository,
                             AdminActivityLogRepository adminActivityLogRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.attendanceLocationRepository = attendanceLocationRepository;
        this.userRepository = userRepository;
        this.adminActivityLogRepository = adminActivityLogRepository;
    }

    public AttendanceResponse markAttendance(AttendanceRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isActive()) {
            throw new IllegalArgumentException("User account is awaiting administrator approval");
        }

        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("Location is required");
        }

        boolean locationVerified = isWithinConfiguredRadius(request.getLatitude(), request.getLongitude());
        boolean faceVerified = request.getFaceImageBase64() != null && !request.getFaceImageBase64().isBlank();

        if (!locationVerified) {
            throw new IllegalArgumentException("Your location is outside the allowed attendance area");
        }

        if (!faceVerified) {
            throw new IllegalArgumentException("Face capture is required for attendance validation");
        }

        String type = request.getAttendanceType() == null ? "CHECK_IN" : request.getAttendanceType().trim().toUpperCase();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = LocalDateTime.of(today, LocalTime.MIDNIGHT);
        LocalDateTime endOfDay = LocalDateTime.of(today, LocalTime.MAX);

        boolean hasTodayRecord = attendanceRecordRepository.existsByUserIdAndCheckInTimeBetween(user.getId(), startOfDay, endOfDay);

        if ("CHECK_IN".equals(type)) {
            if (hasTodayRecord) {
                throw new IllegalArgumentException("User already checked in today");
            }

            AttendanceRecord record = new AttendanceRecord();
            record.setUserId(user.getId());
            record.setCheckInTime(LocalDateTime.now());
            record.setCheckInLatitude(request.getLatitude());
            record.setCheckInLongitude(request.getLongitude());
            record.setFaceVerified(faceVerified);
            record.setLocationVerified(locationVerified);
            record.setStatus("PRESENT");
            AttendanceRecord saved = attendanceRecordRepository.save(record);

            saveActivity(user.getId(), "CHECK_IN", "User checked in successfully");
            return new AttendanceResponse(saved.getId(), saved.getStatus(), "Check-in successful", saved.getCheckInTime(), null);
        }

        if ("CHECK_OUT".equals(type)) {
            AttendanceRecord latest = attendanceRecordRepository.findByUserIdOrderByCheckInTimeDesc(user.getId())
                    .stream()
                    .filter(record -> record.getCheckOutTime() == null)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No active check-in found for the user"));

            latest.setCheckOutTime(LocalDateTime.now());
            latest.setCheckOutLatitude(request.getLatitude());
            latest.setCheckOutLongitude(request.getLongitude());
            latest.setLocationVerified(locationVerified);
            latest.setFaceVerified(faceVerified);
            latest.setStatus("COMPLETED");
            AttendanceRecord saved = attendanceRecordRepository.save(latest);

            saveActivity(user.getId(), "CHECK_OUT", "User checked out successfully");
            return new AttendanceResponse(saved.getId(), saved.getStatus(), "Check-out successful", saved.getCheckInTime(), saved.getCheckOutTime());
        }

        throw new IllegalArgumentException("Attendance type must be CHECK_IN or CHECK_OUT");
    }

    public List<AttendanceRecord> getUserAttendance(Long userId) {
        return attendanceRecordRepository.findByUserIdOrderByCheckInTimeDesc(userId);
    }

    public List<AttendanceRecord> getTodayAttendance() {
        LocalDate today = LocalDate.now();
        return attendanceRecordRepository.findByCheckInTimeBetweenOrderByCheckInTimeDesc(
                LocalDateTime.of(today, LocalTime.MIDNIGHT),
                LocalDateTime.of(today, LocalTime.MAX)
        );
    }

    public List<UserSummary> getUsers() {
        List<User> users = userRepository.findAll();
        List<UserSummary> result = new ArrayList<>();
        for (User user : users) {
            result.add(new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole().name(), user.getProfileImage(), user.isActive()));
        }
        return result;
    }

    public AdminSummary getAdminSummary() {
        long totalUsers = userRepository.count();
        long totalPresent = attendanceRecordRepository.findAllByOrderByCheckInTimeDesc().stream()
                .filter(record -> record.getCheckInTime() != null && record.getCheckOutTime() == null)
                .count();

        long totalLate = attendanceRecordRepository.findAllByOrderByCheckInTimeDesc().stream()
                .filter(record -> record.getCheckInTime() != null && record.getCheckInTime().toLocalTime().isAfter(LocalTime.of(9, 15)))
                .count();

        long totalAbsent = Math.max(0, totalUsers - totalPresent);

        return new AdminSummary(totalUsers, totalPresent, totalLate, totalAbsent);
    }

    private boolean isWithinConfiguredRadius(double latitude, double longitude) {
        List<AttendanceLocation> activeLocations = attendanceLocationRepository.findAllByActiveTrue();
        if (!activeLocations.isEmpty()) {
            return activeLocations.stream().anyMatch(location -> isWithinRadius(
                    latitude, longitude, location.getLatitude(), location.getLongitude(), location.getRadiusMeters()));
        }
        if (attendanceLocationRepository.count() > 0) {
            return false;
        }
        return isWithinRadius(latitude, longitude, companyLatitude, companyLongitude, radiusMeters);
    }

    private boolean isWithinRadius(double latitude, double longitude, double centerLatitude,
                                   double centerLongitude, int allowedRadiusMeters) {
        double earthRadius = 6371000;
        double dLat = Math.toRadians(latitude - centerLatitude);
        double dLng = Math.toRadians(longitude - centerLongitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(centerLatitude)) * Math.cos(Math.toRadians(latitude))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double distance = 2 * earthRadius * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return distance <= allowedRadiusMeters;
    }

    private void saveActivity(Long userId, String action, String details) {
        AdminActivityLog log = new AdminActivityLog();
        log.setAdminId(userId);
        log.setAction(action);
        log.setDetails(details);
        adminActivityLogRepository.save(log);
    }
}
