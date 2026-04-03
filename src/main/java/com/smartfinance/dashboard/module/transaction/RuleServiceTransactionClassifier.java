package com.smartfinance.dashboard.module.transaction;

import com.smartfinance.dashboard.module.rule.RuleService;
import com.smartfinance.dashboard.module.transaction.dto.NormalizedTransactionCandidate;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class RuleServiceTransactionClassifier {

    private static final String REQUEST_CLASS_NAME =
            "com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationRequest";
    private static final String UNCLASSIFIED_CATEGORY = "\u672A\u5206\u7C7B";

    private final ObjectProvider<RuleService> ruleServiceProvider;

    public RuleServiceTransactionClassifier(ObjectProvider<RuleService> ruleServiceProvider) {
        this.ruleServiceProvider = ruleServiceProvider;
    }

    public TransactionImportClassification classify(NormalizedTransactionCandidate candidate) {
        RuleService ruleService = ruleServiceProvider.getIfAvailable();
        if (ruleService == null) {
            return unclassified();
        }

        try {
            Class<?> requestClass = Class.forName(REQUEST_CLASS_NAME);
            Object request = buildRequest(requestClass, candidate);
            Method classifyMethod = ruleService.getClass().getMethod("classifyTransaction", requestClass);
            Object result = classifyMethod.invoke(ruleService, request);
            return mapResult(result);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return unclassified();
        } catch (InvocationTargetException exception) {
            Throwable targetException = exception.getTargetException();
            String message = targetException == null ? exception.getMessage() : targetException.getMessage();
            throw new IllegalStateException(message == null ? "RuleService classifyTransaction invocation failed" : message);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to invoke RuleService classifyTransaction");
        }
    }

    private Object buildRequest(Class<?> requestClass, NormalizedTransactionCandidate candidate)
            throws ReflectiveOperationException {
        Constructor<?> constructor = requestClass.getDeclaredConstructor(
                String.class,
                LocalDate.class,
                BigDecimal.class,
                com.smartfinance.dashboard.module.transaction.enums.TransactionDirection.class,
                String.class,
                String.class,
                String.class
        );
        return constructor.newInstance(
                candidate.sourceType(),
                candidate.transactionDate(),
                candidate.amount(),
                candidate.direction(),
                candidate.merchantName(),
                candidate.summary(),
                candidate.rawText()
        );
    }

    private TransactionImportClassification mapResult(Object result) throws ReflectiveOperationException {
        if (result == null) {
            return unclassified();
        }

        Method autoCategoryMethod = result.getClass().getMethod("autoCategory");
        Method finalCategoryMethod = result.getClass().getMethod("finalCategory");
        Method categorySourceMethod = result.getClass().getMethod("categorySource");

        String autoCategory = trimToNull((String) autoCategoryMethod.invoke(result));
        String finalCategory = trimToNull((String) finalCategoryMethod.invoke(result));
        CategorySource categorySource = (CategorySource) categorySourceMethod.invoke(result);

        if (finalCategory == null || categorySource == null) {
            return unclassified();
        }
        return new TransactionImportClassification(autoCategory, finalCategory, categorySource);
    }

    private TransactionImportClassification unclassified() {
        return new TransactionImportClassification(null, UNCLASSIFIED_CATEGORY, CategorySource.UNCLASSIFIED);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
