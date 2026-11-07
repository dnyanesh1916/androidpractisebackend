package com.attendease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class UserApprovalRequest {

    @DecimalMin(value = "0.01", message = "Daily salary must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Daily salary can have at most 2 decimal places")
    private BigDecimal dailySalary;

    public BigDecimal getDailySalary() {
        return dailySalary;
    }

    public void setDailySalary(BigDecimal dailySalary) {
        this.dailySalary = dailySalary;
    }
}
