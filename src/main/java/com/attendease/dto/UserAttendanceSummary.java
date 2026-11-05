package com.attendease.dto;

import java.time.LocalDateTime;

public record UserAttendanceSummary(
        Long attendanceId,
        String status,
        LocalDateTime checkInTime,
        LocalDateTime checkOutTime) {
}
