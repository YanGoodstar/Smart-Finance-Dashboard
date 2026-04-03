package com.smartfinance.dashboard.module.rule.dto;

import java.time.LocalDateTime;

public record CategoryRuleResponse(
        Long id,
        String ruleName,
        String matchExpression,
        String targetCategory,
        Integer priority,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
