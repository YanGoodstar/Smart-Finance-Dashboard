package com.smartfinance.dashboard.module.rule.classification;

import com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationRequest;
import com.smartfinance.dashboard.module.rule.classification.dto.TransactionClassificationResult;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
public class RuleClassificationServiceImpl implements RuleClassificationService {

    private final ClassificationRuleCatalog classificationRuleCatalog;

    public RuleClassificationServiceImpl(ClassificationRuleCatalog classificationRuleCatalog) {
        this.classificationRuleCatalog = classificationRuleCatalog;
    }

    @Override
    public TransactionClassificationResult classify(TransactionClassificationRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        ClassificationContext context = ClassificationContext.from(request);
        return classificationRuleCatalog.getActiveRules().stream()
                .filter(rule -> rule.matchesSourceType(context.sourceType()))
                .filter(rule -> rule.matches(context.searchText()))
                .findFirst()
                .map(rule -> TransactionClassificationResult.auto(rule.targetCategory()))
                .orElse(TransactionClassificationResult.unclassified());
    }

    private record ClassificationContext(String sourceType, String searchText) {

        private static ClassificationContext from(TransactionClassificationRequest request) {
            return new ClassificationContext(
                    normalize(request.sourceType()),
                    normalize(String.join(
                            "\n",
                            defaultString(request.merchantName()),
                            defaultString(request.summary()),
                            defaultString(request.rawText())
                    ))
            );
        }

        private static String defaultString(String value) {
            return value == null ? "" : value;
        }

        private static String normalize(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
