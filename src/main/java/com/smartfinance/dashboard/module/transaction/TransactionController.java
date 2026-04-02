package com.smartfinance.dashboard.module.transaction;

import com.smartfinance.dashboard.common.api.ApiResponse;
import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    public TransactionController(TransactionQueryService transactionQueryService) {
        this.transactionQueryService = transactionQueryService;
    }

    @GetMapping
    public ApiResponse<TransactionPageResponse> listTransactions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String finalCategory,
            @RequestParam(required = false) CategorySource categorySource,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
                transactionQueryService.queryTransactions(
                        page,
                        size,
                        dateFrom,
                        dateTo,
                        finalCategory,
                        categorySource,
                        keyword
                )
        );
    }
}
