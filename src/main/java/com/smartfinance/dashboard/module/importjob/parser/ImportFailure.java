package com.smartfinance.dashboard.module.importjob.parser;

public record ImportFailure(
        Integer rowNumber,
        String message
) {
}
