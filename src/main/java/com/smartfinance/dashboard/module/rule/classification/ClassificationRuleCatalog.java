package com.smartfinance.dashboard.module.rule.classification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartfinance.dashboard.module.rule.entity.CategoryRule;
import com.smartfinance.dashboard.module.rule.mapper.CategoryRuleMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class ClassificationRuleCatalog {

    private static final String DEFAULT_RULES_LOCATION = "classpath*:rule-defaults/*.json";

    private static final Comparator<CompiledRule> RULE_ORDER = Comparator
            .comparingInt(CompiledRule::priority)
            .thenComparingInt(CompiledRule::sourceRank)
            .thenComparing(CompiledRule::keywordSpecificity, Comparator.reverseOrder())
            .thenComparingLong(CompiledRule::order);

    private final CategoryRuleMapper categoryRuleMapper;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private volatile List<CompiledRule> defaultRules = List.of();
    private volatile List<CompiledRule> userRules = List.of();
    private volatile List<CompiledRule> activeRules = List.of();
    private volatile boolean userRulesLoaded;

    public ClassificationRuleCatalog(CategoryRuleMapper categoryRuleMapper, ObjectMapper objectMapper) {
        this.categoryRuleMapper = categoryRuleMapper;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    void initialize() {
        this.defaultRules = loadDefaultRules();
        rebuildActiveRules();
    }

    public List<CompiledRule> getActiveRules() {
        ensureUserRulesLoaded();
        return activeRules;
    }

    public synchronized void refreshUserRules() {
        this.userRules = loadUserRules();
        this.userRulesLoaded = true;
        rebuildActiveRules();
    }

    private synchronized void ensureUserRulesLoaded() {
        if (!userRulesLoaded) {
            refreshUserRules();
        }
    }

    private void rebuildActiveRules() {
        List<CompiledRule> mergedRules = new ArrayList<>(userRules.size() + defaultRules.size());
        mergedRules.addAll(userRules);
        mergedRules.addAll(defaultRules);
        mergedRules.sort(RULE_ORDER);
        this.activeRules = List.copyOf(mergedRules);
    }

    private List<CompiledRule> loadDefaultRules() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(DEFAULT_RULES_LOCATION);
            Arrays.sort(
                    resources,
                    Comparator.comparing(Resource::getFilename, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
            );

            List<CompiledRule> rules = new ArrayList<>();
            long order = 0L;
            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    List<DefaultRuleDefinition> definitions = objectMapper.readValue(
                            inputStream,
                            new TypeReference<List<DefaultRuleDefinition>>() {
                            }
                    );
                    if (definitions == null) {
                        continue;
                    }
                    for (DefaultRuleDefinition definition : definitions) {
                        CompiledRule compiledRule = CompiledRule.fromDefault(definition, order++);
                        if (compiledRule != null) {
                            rules.add(compiledRule);
                        }
                    }
                }
            }

            rules.sort(RULE_ORDER);
            return List.copyOf(rules);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load default classification rules", exception);
        }
    }

    private List<CompiledRule> loadUserRules() {
        LambdaQueryWrapper<CategoryRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CategoryRule::getEnabled, true)
                .orderByAsc(CategoryRule::getPriority)
                .orderByAsc(CategoryRule::getId);

        List<CategoryRule> entities = categoryRuleMapper.selectList(wrapper);
        List<CompiledRule> rules = new ArrayList<>(entities.size());
        long order = 0L;
        for (CategoryRule entity : entities) {
            CompiledRule compiledRule = CompiledRule.fromUser(entity, order++);
            if (compiledRule != null) {
                rules.add(compiledRule);
            }
        }

        rules.sort(RULE_ORDER);
        return List.copyOf(rules);
    }

    private record DefaultRuleDefinition(
            String ruleName,
            String sourceType,
            String matchExpression,
            String targetCategory,
            Integer priority,
            Boolean enabled
    ) {
    }

    public record CompiledRule(
            String ruleName,
            String sourceType,
            String targetCategory,
            List<String> keywords,
            int priority,
            int sourceRank,
            int keywordSpecificity,
            long order
    ) {

        private static final int USER_SOURCE_RANK = 0;
        private static final int DEFAULT_SOURCE_RANK = 1;

        private static CompiledRule fromDefault(DefaultRuleDefinition definition, long order) {
            if (definition == null || Boolean.FALSE.equals(definition.enabled())) {
                return null;
            }
            return fromValues(
                    definition.ruleName(),
                    definition.sourceType(),
                    definition.matchExpression(),
                    definition.targetCategory(),
                    definition.priority(),
                    DEFAULT_SOURCE_RANK,
                    order
            );
        }

        private static CompiledRule fromUser(CategoryRule entity, long order) {
            if (entity == null || Boolean.FALSE.equals(entity.getEnabled())) {
                return null;
            }
            return fromValues(
                    entity.getRuleName(),
                    null,
                    entity.getMatchExpression(),
                    entity.getTargetCategory(),
                    entity.getPriority(),
                    USER_SOURCE_RANK,
                    order
            );
        }

        private static CompiledRule fromValues(
                String ruleName,
                String sourceType,
                String matchExpression,
                String targetCategory,
                Integer priority,
                int sourceRank,
                long order
        ) {
            List<String> keywords = parseKeywords(matchExpression);
            String resolvedCategory = trimToEmpty(targetCategory);
            if (keywords.isEmpty() || resolvedCategory.isBlank()) {
                return null;
            }

            int specificity = keywords.stream()
                    .mapToInt(String::length)
                    .max()
                    .orElse(0);

            return new CompiledRule(
                    trimToEmpty(ruleName),
                    normalize(sourceType),
                    resolvedCategory,
                    List.copyOf(keywords),
                    priority == null ? 0 : priority,
                    sourceRank,
                    specificity,
                    order
            );
        }

        public boolean matchesSourceType(String requestSourceType) {
            if (sourceType == null || sourceType.isBlank()) {
                return true;
            }
            return sourceType.equals(normalize(requestSourceType));
        }

        public boolean matches(String searchText) {
            if (searchText == null || searchText.isBlank()) {
                return false;
            }
            return keywords.stream().anyMatch(searchText::contains);
        }

        private static List<String> parseKeywords(String matchExpression) {
            if (matchExpression == null || matchExpression.isBlank()) {
                return List.of();
            }
            return Arrays.stream(matchExpression.split("[\\r\\n|,;\\uFF0C\\uFF1B]+"))
                    .map(CompiledRule::normalize)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }

        private static String trimToEmpty(String value) {
            if (value == null) {
                return "";
            }
            return value.trim();
        }

        private static String normalize(String value) {
            if (value == null) {
                return "";
            }
            return value.trim().toLowerCase(Locale.ROOT);
        }
    }
}
