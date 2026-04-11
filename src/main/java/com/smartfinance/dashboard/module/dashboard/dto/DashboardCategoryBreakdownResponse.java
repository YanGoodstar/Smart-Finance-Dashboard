package com.smartfinance.dashboard.module.dashboard.dto;

import java.math.BigDecimal;

public record DashboardCategoryBreakdownResponse(
        String finalCategory,
        BigDecimal totalExpense,
        long transactionCount
) {
}
