package com.attendease.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime checkInTime;

    private LocalDateTime checkOutTime;

    @Column(nullable = false)
    private double checkInLatitude;

    @Column(nullable = false)
    private double checkInLongitude;

    private double checkOutLatitude;
    private double checkOutLongitude;

    @Column(nullable = false)
    private boolean faceVerified;

    @Column(nullable = false)
    private boolean locationVerified;

    @Column(nullable = false)
    private String status = "PRESENT";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public double getCheckInLatitude() {
        return checkInLatitude;
    }

    public void setCheckInLatitude(double checkInLatitude) {
        this.checkInLatitude = checkInLatitude;
    }

    public double getCheckInLongitude() {
        return checkInLongitude;
    }

    public void setCheckInLongitude(double checkInLongitude) {
        this.checkInLongitude = checkInLongitude;
    }

    public double getCheckOutLatitude() {
        return checkOutLatitude;
    }

    public void setCheckOutLatitude(double checkOutLatitude) {
        this.checkOutLatitude = checkOutLatitude;
    }

    public double getCheckOutLongitude() {
        return checkOutLongitude;
    }

    public void setCheckOutLongitude(double checkOutLongitude) {
        this.checkOutLongitude = checkOutLongitude;
    }

    public boolean isFaceVerified() {
        return faceVerified;
    }

    public void setFaceVerified(boolean faceVerified) {
        this.faceVerified = faceVerified;
    }

    public boolean isLocationVerified() {
        return locationVerified;
    }

    public void setLocationVerified(boolean locationVerified) {
        this.locationVerified = locationVerified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
