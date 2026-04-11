package com.smartfinance.dashboard.module.importjob.parser;

import com.smartfinance.dashboard.module.transaction.dto.NormalizedTransactionCandidate;

import java.util.List;

public record ImportParseResult(
        int totalRows,
        List<NormalizedTransactionCandidate> candidates,
        List<ImportFailure> failures
) {
}
