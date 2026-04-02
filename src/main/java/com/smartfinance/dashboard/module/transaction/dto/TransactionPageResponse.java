package com.smartfinance.dashboard.module.transaction.dto;

import java.util.List;

public record TransactionPageResponse(
        List<TransactionSummaryResponse> items,
        long total,
        int page,
        int size
) {
}
