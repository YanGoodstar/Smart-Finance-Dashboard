package com.smartfinance.dashboard.module.transaction.dto;

import com.smartfinance.dashboard.module.transaction.enums.CategorySource;

public record UpdateTransactionCategoryResponse(
        Long transactionId,
        String finalCategory,
        CategorySource categorySource
) {
}
