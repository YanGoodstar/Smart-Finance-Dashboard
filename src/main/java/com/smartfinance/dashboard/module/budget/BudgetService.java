package com.smartfinance.dashboard.module.budget;

import com.smartfinance.dashboard.module.budget.dto.BudgetListResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetProgressResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetSaveRequest;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;

import java.time.LocalDate;

public interface BudgetService {

    BudgetResponse createBudget(BudgetSaveRequest request);

    BudgetListResponse listBudgets(LocalDate budgetMonth);

    BudgetProgressResponse getBudgetProgress(
            LocalDate budgetMonth,
            int page,
            int size,
            LocalDate dateFrom,
            LocalDate dateTo,
            String finalCategory,
            CategorySource categorySource,
            String keyword
    );
}
