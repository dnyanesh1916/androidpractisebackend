package com.attendease.controller;

import com.attendease.dto.AdminSummary;
import com.attendease.entity.AdminActivityLog;
import com.attendease.repository.AdminActivityLogRepository;
import com.attendease.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200", "https://androidpractiseui.netlify.app"}, allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AdminController {

    private final AttendanceService attendanceService;
    private final AdminActivityLogRepository adminActivityLogRepository;

    public AdminController(AttendanceService attendanceService, AdminActivityLogRepository adminActivityLogRepository) {
        this.attendanceService = attendanceService;
        this.adminActivityLogRepository = adminActivityLogRepository;
    }

    @GetMapping("/summary")
    public ResponseEntity<AdminSummary> getSummary() {
        return ResponseEntity.ok(attendanceService.getAdminSummary());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<AdminActivityLog>> getActivity() {
        return ResponseEntity.ok(adminActivityLogRepository.findAllByOrderByCreatedAtDesc());
    }
}
