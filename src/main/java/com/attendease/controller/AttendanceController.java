package com.attendease.controller;

import com.attendease.dto.AttendanceRequest;
import com.attendease.dto.AttendanceResponse;
import com.attendease.dto.UserSummary;
import com.attendease.entity.AttendanceRecord;
import com.attendease.entity.User;
import com.attendease.repository.UserRepository;
import com.attendease.service.AdminService;
import com.attendease.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "https://androidpractiseui.netlify.app"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final AdminService adminService;
    private final UserRepository userRepository;

    public AttendanceController(AttendanceService attendanceService, AdminService adminService,
                                UserRepository userRepository) {
        this.attendanceService = attendanceService;
        this.adminService = adminService;
        this.userRepository = userRepository;
    }

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@Valid @RequestBody AttendanceRequest request) {
        try {
            return ResponseEntity.ok(attendanceService.markAttendance(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AttendanceResponse(null, "FAILED", e.getMessage(), null, null));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AttendanceRecord>> getUserAttendance(@PathVariable Long userId) {
        return ResponseEntity.ok(attendanceService.getUserAttendance(userId));
    }

    @GetMapping("/user/{userId}/compensation")
    public ResponseEntity<?> getUserCompensation(@PathVariable Long userId,
                                                 @AuthenticationPrincipal String authenticatedEmail,
                                                 @RequestParam(required = false) Integer year,
                                                 @RequestParam(required = false) Integer month) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        if (authenticatedEmail == null || !user.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only view your own compensation details"));
        }

        LocalDate currentDate = LocalDate.now();
        return ResponseEntity.ok(adminService.getUserSummary(userId,
                year == null ? currentDate.getYear() : year,
                month == null ? currentDate.getMonthValue() : month));
    }

    @GetMapping("/today")
    public ResponseEntity<List<AttendanceRecord>> getTodayAttendance() {
        return ResponseEntity.ok(attendanceService.getTodayAttendance());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getUsers() {
        return ResponseEntity.ok(attendanceService.getUsers());
    }
}
