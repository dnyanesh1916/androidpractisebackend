package com.attendease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SalaryUpdateRequest {

    @NotNull(message = "Monthly salary is required")
    @DecimalMin(value = "0.00", message = "Monthly salary cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Monthly salary can have at most 2 decimal places")
    private BigDecimal monthlySalary;

    public BigDecimal getMonthlySalary() {
        return monthlySalary;
    }

    public void setMonthlySalary(BigDecimal monthlySalary) {
        this.monthlySalary = monthlySalary;
    }
}
