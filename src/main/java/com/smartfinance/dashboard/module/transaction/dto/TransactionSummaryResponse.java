package com.smartfinance.dashboard.module.transaction.dto;

import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import com.smartfinance.dashboard.module.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionSummaryResponse(
        Long id,
        LocalDate transactionDate,
        BigDecimal amount,
        TransactionDirection direction,
        String merchantName,
        String summary,
        String autoCategory,
        String finalCategory,
        CategorySource categorySource,
        boolean suspectedDuplicate
) {
}
