package com.smartfinance.dashboard.module.budget.dto;

import com.smartfinance.dashboard.module.budget.enums.BudgetWarningLevel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BudgetProgressResponse(
        LocalDate budgetMonth,
        BigDecimal totalBudget,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        BigDecimal usageRate,
        boolean configured,
        BudgetWarningLevel warningLevel,
        List<BudgetCategoryProgressResponse> items,
        long total,
        int page,
        int size
) {
}
