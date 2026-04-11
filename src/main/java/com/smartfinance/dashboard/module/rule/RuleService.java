package com.smartfinance.dashboard.module.rule;

import com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationRequest;
import com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationResult;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleListResponse;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleResponse;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleSaveRequest;

public interface RuleService {

    CategoryRuleResponse createRule(CategoryRuleSaveRequest request);

    CategoryRuleListResponse listRules(Boolean enabled);

    CategoryRuleResponse getRule(long id);

    CategoryRuleResponse updateRule(long id, CategoryRuleSaveRequest request);

    boolean deleteRule(long id);

    TransactionClassificationResult classifyTransaction(TransactionClassificationRequest request);
}
