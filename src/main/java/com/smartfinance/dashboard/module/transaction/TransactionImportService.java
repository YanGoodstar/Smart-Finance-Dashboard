package com.smartfinance.dashboard.module.transaction;

import com.smartfinance.dashboard.module.transaction.dto.NormalizedTransactionCandidate;

import java.util.List;

public interface TransactionImportService {

    TransactionImportResult importTransactions(Long importJobId, List<NormalizedTransactionCandidate> candidates);
}
