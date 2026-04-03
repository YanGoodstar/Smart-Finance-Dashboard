package com.smartfinance.dashboard.module.dashboard.dto;

import com.smartfinance.dashboard.module.transaction.enums.CategorySource;

import java.time.LocalDate;

/**
 * Dashboard-local adapter for the frozen transaction query contract.
 * Temporary adapter only.
 * Before merging into yan, replace this type with
 * module/transaction/dto/TransactionQueryRequest once the shared DTO
 * is present in the integrated baseline.
 */
public record TransactionQueryRequest(
        int page,
        int size,
        LocalDate dateFrom,
        LocalDate dateTo,
        String finalCategory,
        CategorySource categorySource,
        String keyword
) {
}
