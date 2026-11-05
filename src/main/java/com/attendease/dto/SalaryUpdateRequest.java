package com.attendease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SalaryUpdateRequest {

    @NotNull(message = "Daily salary is required")
    @DecimalMin(value = "0.00", message = "Daily salary cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Daily salary can have at most 2 decimal places")
    private BigDecimal dailySalary;

    public BigDecimal getDailySalary() {
        return dailySalary;
    }

    public void setDailySalary(BigDecimal dailySalary) {
        this.dailySalary = dailySalary;
    }
}
