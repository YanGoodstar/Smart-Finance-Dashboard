package com.smartfinance.dashboard.module.rule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CategoryRuleSaveRequest(
        @NotBlank String ruleName,
        @NotBlank String matchExpression,
        @NotBlank String targetCategory,
        @NotNull @Min(0) Integer priority,
        @NotNull Boolean enabled
) {
}
