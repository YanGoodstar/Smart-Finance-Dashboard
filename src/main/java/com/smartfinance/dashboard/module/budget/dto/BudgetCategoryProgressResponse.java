package com.smartfinance.dashboard.module.budget.dto;

import java.math.BigDecimal;

public record BudgetCategoryProgressResponse(
        String category,
        BigDecimal budgetAmount,
        BigDecimal actualSpent,
        BigDecimal remainingAmount,
        BigDecimal usageRate
) {
}
