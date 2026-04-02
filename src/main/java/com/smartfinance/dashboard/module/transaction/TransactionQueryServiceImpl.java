package com.smartfinance.dashboard.module.transaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
import com.smartfinance.dashboard.module.transaction.dto.TransactionSummaryResponse;
import com.smartfinance.dashboard.module.transaction.entity.TransactionRecord;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import com.smartfinance.dashboard.module.transaction.mapper.TransactionRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private final TransactionRecordMapper transactionRecordMapper;

    public TransactionQueryServiceImpl(TransactionRecordMapper transactionRecordMapper) {
        this.transactionRecordMapper = transactionRecordMapper;
    }

    @Override
    public TransactionPageResponse queryTransactions(
            int page,
            int size,
            LocalDate dateFrom,
            LocalDate dateTo,
            String finalCategory,
            CategorySource categorySource,
            String keyword
    ) {
        Page<TransactionRecord> pageRequest = Page.of(page + 1L, size);
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();

        if (dateFrom != null) {
            wrapper.ge(TransactionRecord::getTransactionDate, dateFrom);
        }
        if (dateTo != null) {
            wrapper.le(TransactionRecord::getTransactionDate, dateTo);
        }
        if (finalCategory != null && !finalCategory.isBlank()) {
            wrapper.eq(TransactionRecord::getFinalCategory, finalCategory);
        }
        if (categorySource != null) {
            wrapper.eq(TransactionRecord::getCategorySource, categorySource);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(query -> query
                    .like(TransactionRecord::getMerchantName, keyword)
                    .or()
                    .like(TransactionRecord::getSummary, keyword));
        }

        wrapper.orderByDesc(TransactionRecord::getTransactionDate)
                .orderByDesc(TransactionRecord::getId);

        Page<TransactionRecord> resultPage = transactionRecordMapper.selectPage(pageRequest, wrapper);
        List<TransactionSummaryResponse> items = resultPage.getRecords().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new TransactionPageResponse(items, resultPage.getTotal(), page, size);
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
                record.getSuspectedDuplicate()
        );
    }
}
