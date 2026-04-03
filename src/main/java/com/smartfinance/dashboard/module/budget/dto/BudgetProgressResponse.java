package com.smartfinance.dashboard.module.budget.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BudgetProgressResponse(
        LocalDate budgetMonth,
        BigDecimal totalBudget,
        BigDecimal totalSpent,
        BigDecimal totalRemaining,
        BigDecimal usageRate,
        List<BudgetCategoryProgressResponse> items,
        long total,
        int page,
        int size
) {
}
