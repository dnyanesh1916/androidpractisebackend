package com.attendease.controller;

import com.attendease.dto.AttendanceRequest;
import com.attendease.dto.AttendanceResponse;
import com.attendease.dto.UserSummary;
import com.attendease.entity.AttendanceRecord;
import com.attendease.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "https://androidpractiseui.netlify.app"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
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

    @GetMapping("/today")
    public ResponseEntity<List<AttendanceRecord>> getTodayAttendance() {
        return ResponseEntity.ok(attendanceService.getTodayAttendance());
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSummary>> getUsers() {
        return ResponseEntity.ok(attendanceService.getUsers());
    }
}
