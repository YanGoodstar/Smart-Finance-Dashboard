package com.smartfinance.dashboard.module.dashboard.dto;

import java.math.BigDecimal;

public record DashboardUnclassifiedSummaryResponse(
        boolean hasUnclassified,
        long transactionCount,
        BigDecimal incomeAmount,
        BigDecimal expenseAmount
) {
}
