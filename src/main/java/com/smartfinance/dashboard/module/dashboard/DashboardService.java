package com.smartfinance.dashboard.module.dashboard;

import com.smartfinance.dashboard.module.dashboard.dto.DashboardOverviewResponse;
import com.smartfinance.dashboard.module.dashboard.dto.TransactionQueryRequest;

public interface DashboardService {

    DashboardOverviewResponse getOverview(TransactionQueryRequest request);
}
