package com.attendease.dto;

import java.time.LocalDateTime;

public class AttendanceResponse {
    private Long attendanceId;
    private String status;
    private String message;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    public AttendanceResponse() {
    }

    public AttendanceResponse(Long attendanceId, String status, String message, LocalDateTime checkInTime, LocalDateTime checkOutTime) {
        this.attendanceId = attendanceId;
        this.status = status;
        this.message = message;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
    }

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }
}
