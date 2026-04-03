package com.smartfinance.dashboard.module.rule.classification;

import com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationRequest;
import com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationResult;

public interface RuleClassificationService {

    TransactionClassificationResult classify(TransactionClassificationRequest request);
}
