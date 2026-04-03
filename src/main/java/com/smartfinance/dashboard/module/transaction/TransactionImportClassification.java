package com.smartfinance.dashboard.module.transaction;

import com.smartfinance.dashboard.module.transaction.enums.CategorySource;

public record TransactionImportClassification(
        String autoCategory,
        String finalCategory,
        CategorySource categorySource
) {
}
