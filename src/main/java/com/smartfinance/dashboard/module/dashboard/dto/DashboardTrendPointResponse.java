package com.smartfinance.dashboard.module.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardTrendPointResponse(
        LocalDate date,
        BigDecimal incomeAmount,
        BigDecimal expenseAmount,
        BigDecimal netAmount
) {
}
