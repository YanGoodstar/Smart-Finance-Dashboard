package com.smartfinance.dashboard.module.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfinance.dashboard.module.budget.BudgetService;
import com.smartfinance.dashboard.module.budget.dto.BudgetProgressResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardCategoryBreakdownResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardBudgetAlertResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardOverviewResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardSummaryResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardTrendPointResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardUnclassifiedSummaryResponse;
import com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationResult;
import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
import com.smartfinance.dashboard.module.transaction.dto.TransactionQueryRequest;
import com.smartfinance.dashboard.module.transaction.dto.TransactionSummaryResponse;
import com.smartfinance.dashboard.module.transaction.entity.TransactionRecord;
import com.smartfinance.dashboard.module.transaction.enums.TransactionDirection;
import com.smartfinance.dashboard.module.transaction.mapper.TransactionRecordMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.util.TreeMap;

/**
 * Dashboard aggregation implementation based on frozen transaction query semantics.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRecordMapper transactionRecordMapper;
    private final BudgetService budgetService;

    public DashboardServiceImpl(
            TransactionRecordMapper transactionRecordMapper,
            BudgetService budgetService
    ) {
        this.transactionRecordMapper = transactionRecordMapper;
        this.budgetService = budgetService;
    }

    @Override
    public DashboardOverviewResponse getOverview(TransactionQueryRequest request) {
        List<TransactionRecord> records = transactionRecordMapper.selectList(
                buildFilterWrapper(request)
        );
        List<TransactionRecord> unclassifiedRecords = transactionRecordMapper.selectList(
                buildFilterWrapper(copyForUnclassifiedSummary(request))
        );
        TransactionPageResponse recentTransactions = queryRecentTransactions(request);
        BudgetProgressResponse budgetProgress = budgetService.getBudgetProgress(
                null,
                0,
                1,
                request.getDateFrom(),
                request.getDateTo(),
                request.getFinalCategory(),
                request.getCategorySource(),
                request.getKeyword()
        );

        return new DashboardOverviewResponse(
                buildSummary(records),
                buildCategoryBreakdowns(records),
                recentTransactions,
                buildTrendPoints(records),
                buildBudgetAlert(budgetProgress),
                buildUnclassifiedSummary(unclassifiedRecords)
        );
    }

    private LambdaQueryWrapper<TransactionRecord> buildFilterWrapper(TransactionQueryRequest request) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();
        if (request.getDateFrom() != null) {
            wrapper.ge(TransactionRecord::getTransactionDate, request.getDateFrom());
        }
        if (request.getDateTo() != null) {
            wrapper.le(TransactionRecord::getTransactionDate, request.getDateTo());
        }
        if (request.getFinalCategory() != null && !request.getFinalCategory().isBlank()) {
            wrapper.eq(TransactionRecord::getFinalCategory, request.getFinalCategory().trim());
        }
        if (request.getCategorySource() != null) {
            wrapper.eq(TransactionRecord::getCategorySource, request.getCategorySource());
        }
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(query -> query
                    .like(TransactionRecord::getMerchantName, request.getKeyword())
                    .or()
                    .like(TransactionRecord::getSummary, request.getKeyword()));
        }
        wrapper.orderByDesc(TransactionRecord::getTransactionDate)
                .orderByDesc(TransactionRecord::getId);
        return wrapper;
    }

    private TransactionPageResponse queryRecentTransactions(TransactionQueryRequest request) {
        Page<TransactionRecord> pageRequest = Page.of(request.getPage() + 1L, request.getSize());
        Page<TransactionRecord> resultPage = transactionRecordMapper.selectPage(pageRequest, buildFilterWrapper(request));
        List<TransactionSummaryResponse> items = resultPage.getRecords().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new TransactionPageResponse(items, resultPage.getTotal(), request.getPage(), request.getSize());
    }

    private DashboardSummaryResponse buildSummary(List<TransactionRecord> records) {
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (TransactionRecord record : records) {
            if (record.getAmount() == null || record.getDirection() == null) {
                continue;
            }
            if (record.getDirection() == TransactionDirection.INCOME) {
                totalIncome = totalIncome.add(record.getAmount());
            } else if (record.getDirection() == TransactionDirection.EXPENSE) {
                totalExpense = totalExpense.add(record.getAmount());
            }
        }

        return new DashboardSummaryResponse(
                records.size(),
                totalIncome,
                totalExpense,
                totalIncome.subtract(totalExpense)
        );
    }

    private List<DashboardTrendPointResponse> buildTrendPoints(List<TransactionRecord> records) {
        Map<LocalDate, TrendBucket> buckets = new TreeMap<>();
        for (TransactionRecord record : records) {
            if (record.getTransactionDate() == null || record.getAmount() == null || record.getDirection() == null) {
                continue;
            }
            TrendBucket bucket = buckets.computeIfAbsent(record.getTransactionDate(), ignored -> new TrendBucket());
            if (record.getDirection() == TransactionDirection.INCOME) {
                bucket.incomeAmount = bucket.incomeAmount.add(record.getAmount());
            } else if (record.getDirection() == TransactionDirection.EXPENSE) {
                bucket.expenseAmount = bucket.expenseAmount.add(record.getAmount());
            }
        }

        List<DashboardTrendPointResponse> items = new ArrayList<>(buckets.size());
        for (Map.Entry<LocalDate, TrendBucket> entry : buckets.entrySet()) {
            items.add(new DashboardTrendPointResponse(
                    entry.getKey(),
                    entry.getValue().incomeAmount,
                    entry.getValue().expenseAmount,
                    entry.getValue().incomeAmount.subtract(entry.getValue().expenseAmount)
            ));
        }
        return items;
    }

    private DashboardBudgetAlertResponse buildBudgetAlert(BudgetProgressResponse budgetProgress) {
        return new DashboardBudgetAlertResponse(
                budgetProgress.configured(),
                budgetProgress.warningLevel(),
                budgetProgress.configured() ? budgetProgress.totalBudget() : null,
                budgetProgress.totalSpent(),
                budgetProgress.configured() ? budgetProgress.totalRemaining() : null,
                budgetProgress.configured() ? budgetProgress.usageRate() : null
        );
    }

    private DashboardUnclassifiedSummaryResponse buildUnclassifiedSummary(List<TransactionRecord> records) {
        BigDecimal incomeAmount = BigDecimal.ZERO;
        BigDecimal expenseAmount = BigDecimal.ZERO;

        for (TransactionRecord record : records) {
            if (record.getAmount() == null || record.getDirection() == null) {
                continue;
            }
            if (record.getDirection() == TransactionDirection.INCOME) {
                incomeAmount = incomeAmount.add(record.getAmount());
            } else if (record.getDirection() == TransactionDirection.EXPENSE) {
                expenseAmount = expenseAmount.add(record.getAmount());
            }
        }

        return new DashboardUnclassifiedSummaryResponse(
                !records.isEmpty(),
                records.size(),
                incomeAmount,
                expenseAmount
        );
    }

    private List<DashboardCategoryBreakdownResponse> buildCategoryBreakdowns(List<TransactionRecord> records) {
        Map<String, CategoryBucket> buckets = new LinkedHashMap<>();

        for (TransactionRecord record : records) {
            if (record.getDirection() != TransactionDirection.EXPENSE
                    || record.getAmount() == null
                    || record.getFinalCategory() == null
                    || record.getFinalCategory().isBlank()) {
                continue;
            }
            CategoryBucket bucket = buckets.computeIfAbsent(
                    record.getFinalCategory(),
                    ignored -> new CategoryBucket()
            );
            bucket.totalExpense = bucket.totalExpense.add(record.getAmount());
            bucket.transactionCount++;
        }

        List<DashboardCategoryBreakdownResponse> items = new ArrayList<>();
        for (Map.Entry<String, CategoryBucket> entry : buckets.entrySet()) {
            items.add(new DashboardCategoryBreakdownResponse(
                    entry.getKey(),
                    entry.getValue().totalExpense,
                    entry.getValue().transactionCount
            ));
        }
        items.sort(Comparator
                .comparing(DashboardCategoryBreakdownResponse::totalExpense).reversed()
                .thenComparing(DashboardCategoryBreakdownResponse::finalCategory));
        return items;
    }

    private TransactionQueryRequest copyForUnclassifiedSummary(TransactionQueryRequest request) {
        TransactionQueryRequest copiedRequest = new TransactionQueryRequest();
        copiedRequest.setPage(0);
        copiedRequest.setSize(request.getSize());
        copiedRequest.setDateFrom(request.getDateFrom());
        copiedRequest.setDateTo(request.getDateTo());
        copiedRequest.setFinalCategory(TransactionClassificationResult.UNCLASSIFIED_CATEGORY);
        copiedRequest.setCategorySource(request.getCategorySource());
        copiedRequest.setKeyword(request.getKeyword());
        return copiedRequest;
    }

    private static final class CategoryBucket {
        private BigDecimal totalExpense = BigDecimal.ZERO;
        private long transactionCount;
    }

    private static final class TrendBucket {
        private BigDecimal incomeAmount = BigDecimal.ZERO;
        private BigDecimal expenseAmount = BigDecimal.ZERO;
    }

    private TransactionSummaryResponse toSummaryResponse(TransactionRecord record) {
        return new TransactionSummaryResponse(
                record.getId(),
                record.getTransactionDate(),
                record.getAmount(),
                record.getDirection(),
                record.getMerchantName(),
                record.getSummary(),
                record.getAutoCategory(),
                record.getFinalCategory(),
                record.getCategorySource(),
                Boolean.TRUE.equals(record.getSuspectedDuplicate())
        );
    }
}
