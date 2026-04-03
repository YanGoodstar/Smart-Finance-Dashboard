package com.smartfinance.dashboard.module.importjob.dto;

import com.smartfinance.dashboard.module.importjob.entity.ImportJobStatus;

import java.time.LocalDateTime;

public record ImportJobResponse(
        Long id,
        String sourceType,
        ImportJobStatus status,
        ImportJobExecutionResponse execution,
        ImportJobProcessingSummaryResponse processingSummary,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
