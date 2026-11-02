package com.attendease.controller;

import com.attendease.dto.AdminSummary;
import com.attendease.dto.AttendanceLocationRequest;
import com.attendease.dto.SalaryUpdateRequest;
import com.attendease.dto.UserApprovalRequest;
import com.attendease.dto.UserSummary;
import com.attendease.entity.AdminActivityLog;
import com.attendease.entity.AttendanceLocation;
import com.attendease.service.AdminService;
import com.attendease.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "https://androidpractiseui.netlify.app"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AdminController {

    private final AttendanceService attendanceService;
    private final AdminService adminService;

    public AdminController(AttendanceService attendanceService, AdminService adminService) {
        this.attendanceService = attendanceService;
        this.adminService = adminService;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminSummary> getSummary() {
        return ResponseEntity.ok(attendanceService.getAdminSummary());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<AdminActivityLog>> getActivity() {
        return ResponseEntity.ok(adminService.getActivity());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getUsers(@RequestParam(required = false) Integer year,
                                                      @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        return ResponseEntity.ok(adminService.getUsers(year == null ? now.getYear() : year,
                month == null ? now.getMonthValue() : month));
    }

    @PatchMapping("/users/{userId}/approve")
    public ResponseEntity<UserSummary> approveUser(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "USER") String source,
                                                   @Valid @RequestBody(required = false) UserApprovalRequest request,
                                                   @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.approveUser(userId, source, adminEmail,
                request == null ? null : request.getMonthlySalary()));
    }

    @PatchMapping("/users/{userId}/salary")
    public ResponseEntity<UserSummary> updateSalary(@PathVariable Long userId,
                                                    @Valid @RequestBody SalaryUpdateRequest request,
                                                    @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.updateSalary(userId, request, adminEmail));
    }

    @GetMapping("/locations")
    public ResponseEntity<List<AttendanceLocation>> getLocations() {
        return ResponseEntity.ok(adminService.getLocations());
    }

    @PostMapping("/locations")
    public ResponseEntity<AttendanceLocation> createLocation(@Valid @RequestBody AttendanceLocationRequest request,
                                                             @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createLocation(request, adminEmail));
    }

    @PutMapping("/locations/{locationId}")
    public ResponseEntity<AttendanceLocation> updateLocation(@PathVariable Long locationId,
                                                             @Valid @RequestBody AttendanceLocationRequest request,
                                                             @AuthenticationPrincipal String adminEmail) {
        return ResponseEntity.ok(adminService.updateLocation(locationId, request, adminEmail));
    }

    @DeleteMapping("/locations/{locationId}")
    public ResponseEntity<Void> deactivateLocation(@PathVariable Long locationId,
                                                   @AuthenticationPrincipal String adminEmail) {
        adminService.deactivateLocation(locationId, adminEmail);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return Map.of("message", message);
    }
}
