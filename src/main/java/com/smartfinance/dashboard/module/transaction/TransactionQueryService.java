package com.smartfinance.dashboard.module.transaction;

import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;

import java.time.LocalDate;

public interface TransactionQueryService {

    TransactionPageResponse queryTransactions(
            int page,
            int size,
            LocalDate dateFrom,
            LocalDate dateTo,
            String finalCategory,
            CategorySource categorySource,
            String keyword
    );
}
