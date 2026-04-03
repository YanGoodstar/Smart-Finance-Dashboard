package com.smartfinance.dashboard.module.transaction;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfinance.dashboard.module.transaction.dto.NormalizedTransactionCandidate;
import com.smartfinance.dashboard.module.transaction.entity.TransactionRecord;
import com.smartfinance.dashboard.module.transaction.mapper.TransactionRecordMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionImportServiceImpl implements TransactionImportService {

    private static final int MAX_MERCHANT_NAME_LENGTH = 255;
    private static final int MAX_SUMMARY_LENGTH = 500;

    private final TransactionRecordMapper transactionRecordMapper;
    private final RuleServiceTransactionClassifier ruleServiceTransactionClassifier;

    public TransactionImportServiceImpl(
            TransactionRecordMapper transactionRecordMapper,
            RuleServiceTransactionClassifier ruleServiceTransactionClassifier
    ) {
        this.transactionRecordMapper = transactionRecordMapper;
        this.ruleServiceTransactionClassifier = ruleServiceTransactionClassifier;
    }

    @Override
    public TransactionImportResult importTransactions(Long importJobId, List<NormalizedTransactionCandidate> candidates) {
        int successCount = 0;
        int failedCount = 0;
        int suspectedDuplicateCount = 0;
        List<String> failureMessages = new ArrayList<>();

        for (NormalizedTransactionCandidate candidate : candidates) {
            try {
                TransactionImportClassification classification = ruleServiceTransactionClassifier.classify(candidate);

                TransactionRecord record = new TransactionRecord();
                record.setImportJobId(importJobId);
                record.setTransactionDate(candidate.transactionDate());
                record.setAmount(candidate.amount());
                record.setDirection(candidate.direction());
                record.setMerchantName(truncate(candidate.merchantName(), MAX_MERCHANT_NAME_LENGTH));
                record.setSummary(truncate(candidate.summary(), MAX_SUMMARY_LENGTH));
                record.setRawText(candidate.rawText());
                record.setAutoCategory(classification.autoCategory());
                record.setFinalCategory(classification.finalCategory());
                record.setCategorySource(classification.categorySource());
                record.setSuspectedDuplicate(isSuspectedDuplicate(candidate));
                transactionRecordMapper.insert(record);

                successCount++;
                if (Boolean.TRUE.equals(record.getSuspectedDuplicate())) {
                    suspectedDuplicateCount++;
                }
            } catch (Exception exception) {
                failedCount++;
                failureMessages.add(buildFailureMessage(candidate.rowNumber(), exception));
            }
        }

        return new TransactionImportResult(
                candidates.size(),
                candidates.size(),
                successCount,
                failedCount,
                suspectedDuplicateCount,
                List.copyOf(failureMessages)
        );
    }

    private boolean isSuspectedDuplicate(NormalizedTransactionCandidate candidate) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getTransactionDate, candidate.transactionDate())
                .eq(TransactionRecord::getAmount, candidate.amount())
                .eq(TransactionRecord::getDirection, candidate.direction())
                .eq(TransactionRecord::getMerchantName, candidate.merchantName());
        return transactionRecordMapper.selectCount(wrapper) > 0;
    }

    private String buildFailureMessage(int rowNumber, Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "transaction import failed";
        }
        return "row " + rowNumber + ": " + message;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
