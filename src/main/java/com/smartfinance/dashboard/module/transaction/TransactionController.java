package com.smartfinance.dashboard.module.transaction;

import com.smartfinance.dashboard.common.api.ApiResponse;
import com.smartfinance.dashboard.common.api.ErrorCode;
import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
import com.smartfinance.dashboard.module.transaction.dto.TransactionQueryRequest;
import com.smartfinance.dashboard.module.transaction.dto.UpdateTransactionCategoryRequest;
import com.smartfinance.dashboard.module.transaction.dto.UpdateTransactionCategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    public TransactionController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    @GetMapping
    public ApiResponse<TransactionPageResponse> listTransactions(@Valid @ModelAttribute TransactionQueryRequest request) {
        return ApiResponse.success(transactionQueryService.queryTransactions(request));
    }

    @PatchMapping("/{transactionId}/category")
    public ResponseEntity<ApiResponse<UpdateTransactionCategoryResponse>> updateTransactionCategory(
            @PathVariable Long transactionId,
            @Valid @RequestBody UpdateTransactionCategoryRequest request
    ) {
        UpdateTransactionCategoryResponse response = transactionQueryService.updateTransactionCategory(transactionId, request);
        if (response == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "Transaction not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
