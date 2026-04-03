package com.smartfinance.dashboard.module.dashboard.dto;

import com.smartfinance.dashboard.module.budget.enums.BudgetWarningLevel;

import java.math.BigDecimal;

public record DashboardBudgetAlertResponse(
        boolean configured,
        BudgetWarningLevel warningLevel,
        BigDecimal budgetAmount,
        BigDecimal spentAmount,
        BigDecimal remainingAmount,
        BigDecimal usageRate
) {
}
