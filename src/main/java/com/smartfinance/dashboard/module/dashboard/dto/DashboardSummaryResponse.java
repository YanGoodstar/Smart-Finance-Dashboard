package com.smartfinance.dashboard.module.dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long transactionCount,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netAmount
) {
}
