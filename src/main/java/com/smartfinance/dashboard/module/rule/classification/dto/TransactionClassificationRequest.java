package com.smartfinance.dashboard.module.rule.classification.dto;

import com.smartfinance.dashboard.module.transaction.enums.TransactionDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionClassificationRequest(
        String sourceType,
        LocalDate transactionDate,
        BigDecimal amount,
        TransactionDirection direction,
        String merchantName,
        String summary,
        String rawText
) {
    public TransactionClassificationRequest {
        sourceType = trimToNull(sourceType);
        merchantName = trimToNull(merchantName);
        summary = trimToNull(summary);
        rawText = trimToNull(rawText);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
