package com.smartfinance.dashboard.module.transaction;

import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
import com.smartfinance.dashboard.module.transaction.dto.TransactionQueryRequest;
import com.smartfinance.dashboard.module.transaction.dto.UpdateTransactionCategoryRequest;
import com.smartfinance.dashboard.module.transaction.dto.UpdateTransactionCategoryResponse;

public interface TransactionQueryService {

    TransactionPageResponse queryTransactions(TransactionQueryRequest request);

    UpdateTransactionCategoryResponse updateTransactionCategory(
            Long transactionId,
            UpdateTransactionCategoryRequest request
    );
}
