package com.smartfinance.dashboard.module.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardCategoryBreakdownResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardOverviewResponse;
import com.smartfinance.dashboard.module.dashboard.dto.DashboardSummaryResponse;
import com.smartfinance.dashboard.module.dashboard.dto.TransactionQueryRequest;
import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
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

/**
 * Dashboard aggregation implementation based on frozen transaction query semantics.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRecordMapper transactionRecordMapper;

    public DashboardServiceImpl(TransactionRecordMapper transactionRecordMapper) {
        this.transactionRecordMapper = transactionRecordMapper;
    }

    @Override
    public DashboardOverviewResponse getOverview(TransactionQueryRequest request) {
        List<TransactionRecord> records = transactionRecordMapper.selectList(
                buildFilterWrapper(request)
        );
        TransactionPageResponse recentTransactions = queryRecentTransactions(request);

        return new DashboardOverviewResponse(
                buildSummary(records),
                buildCategoryBreakdowns(records),
                recentTransactions
        );
    }

    private LambdaQueryWrapper<TransactionRecord> buildFilterWrapper(TransactionQueryRequest request) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();
        if (request.dateFrom() != null) {
            wrapper.ge(TransactionRecord::getTransactionDate, request.dateFrom());
        }
        if (request.dateTo() != null) {
            wrapper.le(TransactionRecord::getTransactionDate, request.dateTo());
        }
        if (request.finalCategory() != null && !request.finalCategory().isBlank()) {
            wrapper.eq(TransactionRecord::getFinalCategory, request.finalCategory().trim());
        }
        if (request.categorySource() != null) {
            wrapper.eq(TransactionRecord::getCategorySource, request.categorySource());
        }
        if (request.keyword() != null && !request.keyword().isBlank()) {
            wrapper.and(query -> query
                    .like(TransactionRecord::getMerchantName, request.keyword())
                    .or()
                    .like(TransactionRecord::getSummary, request.keyword()));
        }
        wrapper.orderByDesc(TransactionRecord::getTransactionDate)
                .orderByDesc(TransactionRecord::getId);
        return wrapper;
    }

    private TransactionPageResponse queryRecentTransactions(TransactionQueryRequest request) {
        Page<TransactionRecord> pageRequest = Page.of(request.page() + 1L, request.size());
        Page<TransactionRecord> resultPage = transactionRecordMapper.selectPage(pageRequest, buildFilterWrapper(request));
        List<TransactionSummaryResponse> items = resultPage.getRecords().stream()
                .map(this::toSummaryResponse)
                .toList();
        return new TransactionPageResponse(items, resultPage.getTotal(), request.page(), request.size());
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

    private static final class CategoryBucket {
        private BigDecimal totalExpense = BigDecimal.ZERO;
        private long transactionCount;
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
