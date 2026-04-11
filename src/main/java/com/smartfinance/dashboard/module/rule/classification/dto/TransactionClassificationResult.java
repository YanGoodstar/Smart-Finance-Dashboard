package com.smartfinance.dashboard.module.rule.classification.dto;

import com.smartfinance.dashboard.module.transaction.enums.CategorySource;

public record TransactionClassificationResult(
        String autoCategory,
        String finalCategory,
        CategorySource categorySource
) {

    public static final String UNCLASSIFIED_CATEGORY = "\u672A\u5206\u7C7B";

    public static TransactionClassificationResult auto(String autoCategory) {
        String normalizedCategory = normalizeCategory(autoCategory);
        return new TransactionClassificationResult(
                normalizedCategory,
                normalizedCategory,
                CategorySource.AUTO
        );
    }

    public static TransactionClassificationResult unclassified() {
        return new TransactionClassificationResult(
                null,
                UNCLASSIFIED_CATEGORY,
                CategorySource.UNCLASSIFIED
        );
    }

    private static String normalizeCategory(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}
