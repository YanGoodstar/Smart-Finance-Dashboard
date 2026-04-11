package com.smartfinance.dashboard.module.importjob.dto;

import com.smartfinance.dashboard.module.importjob.entity.ImportJobStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record UpdateImportJobTrackingRequest(
        ImportJobStatus status,
        @Min(0) Integer totalCount,
        @Min(0) Integer processedCount,
        @Min(0) Integer successCount,
        @Min(0) Integer failedCount,
        @Min(0) Integer suspectedDuplicateCount,
        @Size(max = 1000) String errorSummary,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
