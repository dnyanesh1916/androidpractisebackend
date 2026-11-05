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
    private BigDecimal dailySalary;
    private BigDecimal monthlyAdvance;
    private int workingDaysThisMonth;
    private int scheduledWorkingDaysThisMonth;
    private BigDecimal earnedPayThisMonth;
    private BigDecimal salaryPaidThisMonth;
    private BigDecimal totalBalanceThisMonth;

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
                       boolean active, LocalDateTime createdAt, BigDecimal dailySalary, int workingDaysThisMonth) {
        this(id, fullName, email, role, profileImage, active, createdAt);
        this.dailySalary = dailySalary;
        this.workingDaysThisMonth = workingDaysThisMonth;
    }

    public UserSummary(Long id, String fullName, String email, String role, String profileImage,
                       boolean active, LocalDateTime createdAt, BigDecimal dailySalary, BigDecimal monthlyAdvance,
                       int workingDaysThisMonth, int scheduledWorkingDaysThisMonth, BigDecimal salaryPaidThisMonth,
                       BigDecimal earnedPayThisMonth, BigDecimal totalBalanceThisMonth) {
        this(id, fullName, email, role, profileImage, active, createdAt, dailySalary, workingDaysThisMonth);
        this.monthlyAdvance = monthlyAdvance;
        this.scheduledWorkingDaysThisMonth = scheduledWorkingDaysThisMonth;
        this.salaryPaidThisMonth = salaryPaidThisMonth;
        this.earnedPayThisMonth = earnedPayThisMonth;
        this.totalBalanceThisMonth = totalBalanceThisMonth;
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

    public BigDecimal getDailySalary() {
        return dailySalary;
    }

    public void setDailySalary(BigDecimal dailySalary) {
        this.dailySalary = dailySalary;
    }

    public BigDecimal getMonthlyAdvance() {
        return monthlyAdvance;
    }

    public void setMonthlyAdvance(BigDecimal monthlyAdvance) {
        this.monthlyAdvance = monthlyAdvance;
    }

    public int getWorkingDaysThisMonth() {
        return workingDaysThisMonth;
    }

    public void setWorkingDaysThisMonth(int workingDaysThisMonth) {
        this.workingDaysThisMonth = workingDaysThisMonth;
    }

    public int getScheduledWorkingDaysThisMonth() {
        return scheduledWorkingDaysThisMonth;
    }

    public void setScheduledWorkingDaysThisMonth(int scheduledWorkingDaysThisMonth) {
        this.scheduledWorkingDaysThisMonth = scheduledWorkingDaysThisMonth;
    }

    public BigDecimal getEarnedPayThisMonth() {
        return earnedPayThisMonth;
    }

    public void setEarnedPayThisMonth(BigDecimal earnedPayThisMonth) {
        this.earnedPayThisMonth = earnedPayThisMonth;
    }

    public BigDecimal getSalaryPaidThisMonth() {
        return salaryPaidThisMonth;
    }

    public void setSalaryPaidThisMonth(BigDecimal salaryPaidThisMonth) {
        this.salaryPaidThisMonth = salaryPaidThisMonth;
    }

    public BigDecimal getTotalBalanceThisMonth() {
        return totalBalanceThisMonth;
    }

    public void setTotalBalanceThisMonth(BigDecimal totalBalanceThisMonth) {
        this.totalBalanceThisMonth = totalBalanceThisMonth;
    }
}
