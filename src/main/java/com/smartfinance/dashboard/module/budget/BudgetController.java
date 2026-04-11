package com.smartfinance.dashboard.module.budget;

import com.smartfinance.dashboard.common.api.ApiResponse;
import com.smartfinance.dashboard.common.api.ErrorCode;
import com.smartfinance.dashboard.module.budget.dto.BudgetListResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetProgressResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetSaveRequest;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Validated
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> createBudget(@Valid @RequestBody BudgetSaveRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(budgetService.createBudget(request)));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, exception.getMessage()));
        }
    }

    @GetMapping
    public ApiResponse<BudgetListResponse> listBudgets(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate budgetMonth
    ) {
        return ApiResponse.success(budgetService.listBudgets(budgetMonth));
    }

    @GetMapping("/progress")
    public ApiResponse<BudgetProgressResponse> getBudgetProgress(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate budgetMonth,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String finalCategory,
            @RequestParam(required = false) CategorySource categorySource,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.success(
                budgetService.getBudgetProgress(
                        budgetMonth,
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
