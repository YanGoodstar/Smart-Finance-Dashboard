package com.smartfinance.dashboard.module.transaction.dto;

import com.smartfinance.dashboard.module.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NormalizedTransactionCandidate(
        int rowNumber,
        String sourceType,
        LocalDate transactionDate,
        BigDecimal amount,
        TransactionDirection direction,
        String merchantName,
        String summary,
        String rawText
) {
}
