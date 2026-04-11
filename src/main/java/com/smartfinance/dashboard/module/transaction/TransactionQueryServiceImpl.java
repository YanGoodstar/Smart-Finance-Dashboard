package com.smartfinance.dashboard.module.transaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartfinance.dashboard.module.transaction.dto.TransactionPageResponse;
import com.smartfinance.dashboard.module.transaction.dto.TransactionQueryRequest;
import com.smartfinance.dashboard.module.transaction.dto.TransactionSummaryResponse;
import com.smartfinance.dashboard.module.transaction.dto.UpdateTransactionCategoryRequest;
import com.smartfinance.dashboard.module.transaction.dto.UpdateTransactionCategoryResponse;
import com.smartfinance.dashboard.module.transaction.entity.TransactionRecord;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import com.smartfinance.dashboard.module.transaction.mapper.TransactionRecordMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private final TransactionRecordMapper transactionRecordMapper;

    public TransactionQueryServiceImpl(TransactionRecordMapper transactionRecordMapper) {
        this.transactionRecordMapper = transactionRecordMapper;
    }

    @Override
    public TransactionPageResponse queryTransactions(TransactionQueryRequest request) {
        Page<TransactionRecord> pageRequest = Page.of(request.getPage() + 1L, request.getSize());
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();

        if (request.getDateFrom() != null) {
            wrapper.ge(TransactionRecord::getTransactionDate, request.getDateFrom());
        }
        if (request.getDateTo() != null) {
            wrapper.le(TransactionRecord::getTransactionDate, request.getDateTo());
        }
        if (request.getFinalCategory() != null && !request.getFinalCategory().isBlank()) {
            wrapper.eq(TransactionRecord::getFinalCategory, request.getFinalCategory());
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

        Page<TransactionRecord> resultPage = transactionRecordMapper.selectPage(pageRequest, wrapper);
        List<TransactionSummaryResponse> items = resultPage.getRecords().stream()
                .map(this::toSummaryResponse)
                .toList();

        return new TransactionPageResponse(items, resultPage.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    public UpdateTransactionCategoryResponse updateTransactionCategory(
            Long transactionId,
            UpdateTransactionCategoryRequest request
    ) {
        TransactionRecord record = transactionRecordMapper.selectById(transactionId);
        if (record == null) {
            return null;
        }

        record.setFinalCategory(request.finalCategory().trim());
        record.setCategorySource(CategorySource.MANUAL);
        transactionRecordMapper.updateById(record);

        TransactionRecord persistedRecord = transactionRecordMapper.selectById(transactionId);
        TransactionRecord responseRecord = persistedRecord != null ? persistedRecord : record;
        return new UpdateTransactionCategoryResponse(
                responseRecord.getId(),
                responseRecord.getFinalCategory(),
                responseRecord.getCategorySource()
        );
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
