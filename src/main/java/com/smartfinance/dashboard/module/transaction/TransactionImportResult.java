package com.smartfinance.dashboard.module.transaction;

import java.util.List;

public record TransactionImportResult(
        int totalCount,
        int processedCount,
        int successCount,
        int failedCount,
        int suspectedDuplicateCount,
        List<String> failureMessages
) {
}
