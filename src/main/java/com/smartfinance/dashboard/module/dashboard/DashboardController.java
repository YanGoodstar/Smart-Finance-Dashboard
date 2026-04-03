package com.smartfinance.dashboard.module.dashboard;

import com.smartfinance.dashboard.common.api.ApiResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardOverviewResponse;
import com.smartfinance.dashboard.module.transaction.dto.TransactionQueryRequest;
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

/**
 * Dashboard aggregation API.
 */
@Validated
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    public ApiResponse<DashboardOverviewResponse> getOverview(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String finalCategory,
            @RequestParam(required = false) CategorySource categorySource,
            @RequestParam(required = false) String keyword
    ) {
        TransactionQueryRequest request = new TransactionQueryRequest();
        request.setPage(page);
        request.setSize(size);
        request.setDateFrom(dateFrom);
        request.setDateTo(dateTo);
        request.setFinalCategory(finalCategory);
        request.setCategorySource(categorySource);
        request.setKeyword(keyword);

        return ApiResponse.success(
                dashboardService.getOverview(request)
        );
    }
}
