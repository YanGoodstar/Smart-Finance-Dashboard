package com.smartfinance.dashboard.module.importjob.dto;

public record ImportJobProcessingSummaryResponse(
        int totalCount,
        int processedCount,
        int successCount,
        int failedCount,
        int suspectedDuplicateCount,
        String errorSummary
) {
}
