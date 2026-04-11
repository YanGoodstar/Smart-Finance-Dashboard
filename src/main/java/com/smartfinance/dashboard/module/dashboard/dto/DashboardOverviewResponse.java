package com.smartfinance.dashboard.module.dashboard.dto;

import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;

import java.util.List;

/**
 * Dashboard aggregation response built from frozen transaction query semantics.
 */
public record DashboardOverviewResponse(
        DashboardSummaryResponse summary,
        List<DashboardCategoryBreakdownResponse> categoryBreakdowns,
        TransactionPageResponse recentTransactions,
        List<DashboardTrendPointResponse> trendPoints,
        DashboardBudgetAlertResponse budgetAlert,
        DashboardUnclassifiedSummaryResponse unclassifiedSummary
) {
}
