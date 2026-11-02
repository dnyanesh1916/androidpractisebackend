package com.attendease.dto;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public class UserSummary {
    private Long id;
    private String fullName;
    private String email;
    private String role;
    private String profileImage;
    private boolean active;
    private LocalDateTime createdAt;
    private String source = "USER";
    private BigDecimal monthlySalary;
    private int workingDaysThisMonth;

    public UserSummary() {
    }

    public UserSummary(Long id, String fullName, String email, String role, String profileImage, boolean active) {
        this(id, fullName, email, role, profileImage, active, null);
    }

    public UserSummary(Long id, String fullName, String email, String role, String profileImage, boolean active, LocalDateTime createdAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.profileImage = profileImage;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UserSummary(Long id, String fullName, String email, String role, String profileImage,
                       boolean active, LocalDateTime createdAt, BigDecimal monthlySalary, int workingDaysThisMonth) {
        this(id, fullName, email, role, profileImage, active, createdAt);
        this.monthlySalary = monthlySalary;
        this.workingDaysThisMonth = workingDaysThisMonth;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }

    public int getWorkingDaysThisMonth() {
        return workingDaysThisMonth;
    }

    public void setWorkingDaysThisMonth(int workingDaysThisMonth) {
        this.workingDaysThisMonth = workingDaysThisMonth;
    }
}
