package com.smartfinance.dashboard.module.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BudgetSaveRequest(
        @NotNull LocalDate budgetMonth,
        String category,
        @NotNull @DecimalMin(value = "0.00") BigDecimal amount
) {
}
