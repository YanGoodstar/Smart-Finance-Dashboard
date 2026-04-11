package com.smartfinance.dashboard.module.importjob.dto;

import java.time.LocalDateTime;

public record ImportJobExecutionResponse(
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        boolean running,
        boolean terminal
) {
}
