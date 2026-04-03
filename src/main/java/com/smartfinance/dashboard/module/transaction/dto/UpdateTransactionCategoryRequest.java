package com.smartfinance.dashboard.module.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTransactionCategoryRequest(
        @NotBlank @Size(max = 64) String finalCategory
) {
}
