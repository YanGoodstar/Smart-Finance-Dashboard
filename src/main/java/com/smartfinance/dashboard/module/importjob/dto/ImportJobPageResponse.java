package com.smartfinance.dashboard.module.importjob.dto;

import java.util.List;

public record ImportJobPageResponse(
        List<ImportJobResponse> items,
        long total,
        int page,
        int size
) {
}
