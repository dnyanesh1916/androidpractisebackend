package com.attendease.dto;

public class AdminSummary {
    private long totalUsers;
    private long totalPresent;
    private long totalLate;
    private long totalAbsent;

    public AdminSummary() {
    }

    public AdminSummary(long totalUsers, long totalPresent, long totalLate, long totalAbsent) {
        this.totalUsers = totalUsers;
        this.totalPresent = totalPresent;
        this.totalLate = totalLate;
        this.totalAbsent = totalAbsent;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalPresent() {
        return totalPresent;
    }

    public void setTotalPresent(long totalPresent) {
        this.totalPresent = totalPresent;
    }

    public long getTotalLate() {
        return totalLate;
    }

    public void setTotalLate(long totalLate) {
        this.totalLate = totalLate;
    }

    public long getTotalAbsent() {
        return totalAbsent;
    }

    public void setTotalAbsent(long totalAbsent) {
        this.totalAbsent = totalAbsent;
    }
}
