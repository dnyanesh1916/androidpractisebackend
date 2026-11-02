package com.attendease.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

public class UserApprovalRequest {

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
