package com.smartfinance.dashboard.module.budget;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfinance.dashboard.module.budget.dto.BudgetCategoryProgressResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetListResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetProgressResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetResponse;
import com.smartfinance.dashboard.module.budget.dto.BudgetSaveRequest;
import com.smartfinance.dashboard.module.budget.enums.BudgetWarningLevel;
import com.smartfinance.dashboard.module.budget.entity.BudgetPlan;
import com.smartfinance.dashboard.module.budget.mapper.BudgetPlanMapper;
import com.smartfinance.dashboard.module.transaction.entity.TransactionRecord;
import com.smartfinance.dashboard.module.transaction.enums.CategorySource;
import com.smartfinance.dashboard.module.transaction.enums.TransactionDirection;
import com.smartfinance.dashboard.module.transaction.mapper.TransactionRecordMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BudgetServiceImpl implements BudgetService {

    private static final String TOTAL_CATEGORY = "__TOTAL__";

    private final BudgetPlanMapper budgetPlanMapper;
    private final TransactionRecordMapper transactionRecordMapper;

    public BudgetServiceImpl(
            BudgetPlanMapper budgetPlanMapper,
            TransactionRecordMapper transactionRecordMapper
    ) {
        this.budgetPlanMapper = budgetPlanMapper;
        this.transactionRecordMapper = transactionRecordMapper;
    }

    @Override
    public BudgetResponse createBudget(BudgetSaveRequest request) {
        LocalDate normalizedBudgetMonth = normalizeBudgetMonth(request.budgetMonth());
        String normalizedCategory = normalizeCategory(request.category());
        ensureBudgetNotExists(normalizedBudgetMonth, normalizedCategory);

        BudgetPlan entity = new BudgetPlan();
        entity.setBudgetMonth(normalizedBudgetMonth);
        entity.setCategory(normalizedCategory);
        entity.setAmount(request.amount());
        budgetPlanMapper.insert(entity);
        return toResponse(entity);
    }

    @Override
    public BudgetListResponse listBudgets(LocalDate budgetMonth) {
        LambdaQueryWrapper<BudgetPlan> wrapper = new LambdaQueryWrapper<>();
        if (budgetMonth != null) {
            wrapper.eq(BudgetPlan::getBudgetMonth, normalizeBudgetMonth(budgetMonth));
        }
        wrapper.orderByAsc(BudgetPlan::getBudgetMonth)
                .orderByAsc(BudgetPlan::getCategory)
                .orderByAsc(BudgetPlan::getId);

        List<BudgetResponse> items = budgetPlanMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
        return new BudgetListResponse(items);
    }

    @Override
    public BudgetProgressResponse getBudgetProgress(
            LocalDate budgetMonth,
            int page,
            int size,
            LocalDate dateFrom,
            LocalDate dateTo,
            String finalCategory,
            CategorySource categorySource,
            String keyword
    ) {
        LocalDate resolvedBudgetMonth = resolveBudgetMonth(budgetMonth, dateFrom, dateTo);
        LocalDate resolvedDateFrom = dateFrom != null ? dateFrom : resolvedBudgetMonth.withDayOfMonth(1);
        LocalDate resolvedDateTo = dateTo != null ? dateTo : resolvedBudgetMonth.withDayOfMonth(resolvedBudgetMonth.lengthOfMonth());

        List<BudgetPlan> plans = loadBudgetPlans(resolvedBudgetMonth, finalCategory);
        List<BudgetPlan> categoryPlans = plans.stream()
                .filter(plan -> !isTotalCategory(plan.getCategory()))
                .sorted(Comparator.comparing(BudgetPlan::getCategory))
                .toList();

        List<TransactionRecord> expenseRecords = transactionRecordMapper.selectList(
                buildExpenseFilterWrapper(resolvedDateFrom, resolvedDateTo, finalCategory, categorySource, keyword)
        );

        BigDecimal totalSpent = sumAmounts(expenseRecords);
        Map<String, BigDecimal> spentByCategory = buildSpentByCategory(expenseRecords);
        List<BudgetCategoryProgressResponse> allItems = buildCategoryProgressItems(categoryPlans, spentByCategory);
        List<BudgetCategoryProgressResponse> pagedItems = paginate(allItems, page, size);
        BudgetConfiguration budgetConfiguration = resolveBudgetConfiguration(plans, categoryPlans, finalCategory);
        BigDecimal totalBudget = budgetConfiguration.budgetAmount();
        BigDecimal usageRate = calculateUsageRate(totalSpent, totalBudget, budgetConfiguration.configured());
        BudgetWarningLevel warningLevel = resolveWarningLevel(usageRate, budgetConfiguration.configured());

        return new BudgetProgressResponse(
                resolvedBudgetMonth,
                totalBudget,
                totalSpent,
                calculateRemainingAmount(totalBudget, totalSpent, budgetConfiguration.configured()),
                usageRate,
                budgetConfiguration.configured(),
                warningLevel,
                pagedItems,
                allItems.size(),
                page,
                size
        );
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return TOTAL_CATEGORY;
        }
        return category.trim();
    }

    private LocalDate normalizeBudgetMonth(LocalDate budgetMonth) {
        return budgetMonth.withDayOfMonth(1);
    }

    private LocalDate resolveBudgetMonth(LocalDate budgetMonth, LocalDate dateFrom, LocalDate dateTo) {
        if (budgetMonth != null) {
            return normalizeBudgetMonth(budgetMonth);
        }
        if (dateFrom != null) {
            return normalizeBudgetMonth(dateFrom);
        }
        if (dateTo != null) {
            return normalizeBudgetMonth(dateTo);
        }
        return YearMonth.now().atDay(1);
    }

    private void ensureBudgetNotExists(LocalDate budgetMonth, String category) {
        LambdaQueryWrapper<BudgetPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BudgetPlan::getBudgetMonth, budgetMonth)
                .eq(BudgetPlan::getCategory, category);

        Long count = budgetPlanMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            if (isTotalCategory(category)) {
                throw new IllegalArgumentException("Total budget for the month already exists");
            }
            throw new IllegalArgumentException("Category budget for the month already exists");
        }
    }

    private List<BudgetPlan> loadBudgetPlans(LocalDate budgetMonth, String finalCategory) {
        LambdaQueryWrapper<BudgetPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BudgetPlan::getBudgetMonth, budgetMonth);
        if (finalCategory != null && !finalCategory.isBlank()) {
            wrapper.eq(BudgetPlan::getCategory, normalizeCategory(finalCategory));
        }
        wrapper.orderByAsc(BudgetPlan::getCategory)
                .orderByAsc(BudgetPlan::getId);
        return budgetPlanMapper.selectList(wrapper);
    }

    private LambdaQueryWrapper<TransactionRecord> buildExpenseFilterWrapper(
            LocalDate dateFrom,
            LocalDate dateTo,
            String finalCategory,
            CategorySource categorySource,
            String keyword
    ) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TransactionRecord::getDirection, TransactionDirection.EXPENSE);
        if (dateFrom != null) {
            wrapper.ge(TransactionRecord::getTransactionDate, dateFrom);
        }
        if (dateTo != null) {
            wrapper.le(TransactionRecord::getTransactionDate, dateTo);
        }
        if (finalCategory != null && !finalCategory.isBlank()) {
            wrapper.eq(TransactionRecord::getFinalCategory, finalCategory.trim());
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
        return wrapper;
    }

    private BigDecimal sumAmounts(List<TransactionRecord> records) {
        BigDecimal total = BigDecimal.ZERO;
        for (TransactionRecord record : records) {
            if (record.getAmount() != null) {
                total = total.add(record.getAmount());
            }
        }
        return total;
    }

    private Map<String, BigDecimal> buildSpentByCategory(List<TransactionRecord> records) {
        Map<String, BigDecimal> spentByCategory = new LinkedHashMap<>();
        for (TransactionRecord record : records) {
            if (record.getFinalCategory() == null || record.getFinalCategory().isBlank() || record.getAmount() == null) {
                continue;
            }
            spentByCategory.merge(record.getFinalCategory(), record.getAmount(), BigDecimal::add);
        }
        return spentByCategory;
    }

    private List<BudgetCategoryProgressResponse> buildCategoryProgressItems(
            List<BudgetPlan> categoryPlans,
            Map<String, BigDecimal> spentByCategory
    ) {
        List<BudgetCategoryProgressResponse> items = new ArrayList<>();
        for (BudgetPlan plan : categoryPlans) {
            BigDecimal actualSpent = spentByCategory.getOrDefault(plan.getCategory(), BigDecimal.ZERO);
            BigDecimal usageRate = calculateUsageRate(actualSpent, plan.getAmount(), true);
            items.add(new BudgetCategoryProgressResponse(
                    toApiCategory(plan.getCategory()),
                    plan.getAmount(),
                    actualSpent,
                    plan.getAmount().subtract(actualSpent),
                    usageRate,
                    resolveWarningLevel(usageRate, true)
            ));
        }
        return items;
    }

    private List<BudgetCategoryProgressResponse> paginate(
            List<BudgetCategoryProgressResponse> items,
            int page,
            int size
    ) {
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private BudgetConfiguration resolveBudgetConfiguration(
            List<BudgetPlan> allPlans,
            List<BudgetPlan> categoryPlans,
            String finalCategory
    ) {
        if (finalCategory != null && !finalCategory.isBlank()) {
            return categoryPlans.stream()
                    .findFirst()
                    .map(plan -> new BudgetConfiguration(true, plan.getAmount()))
                    .orElseGet(() -> new BudgetConfiguration(false, null));
        }

        for (BudgetPlan plan : allPlans) {
            if (isTotalCategory(plan.getCategory())) {
                return new BudgetConfiguration(true, plan.getAmount());
            }
        }

        if (categoryPlans.isEmpty()) {
            return new BudgetConfiguration(false, null);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BudgetPlan plan : categoryPlans) {
            if (plan.getAmount() != null) {
                total = total.add(plan.getAmount());
            }
        }
        return new BudgetConfiguration(true, total);
    }

    private BigDecimal calculateUsageRate(BigDecimal actualSpent, BigDecimal budgetAmount, boolean configured) {
        if (!configured || budgetAmount == null) {
            return null;
        }
        if (BigDecimal.ZERO.compareTo(budgetAmount) == 0) {
            return actualSpent.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ONE : BigDecimal.ZERO;
        }
        return actualSpent.divide(budgetAmount, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRemainingAmount(BigDecimal totalBudget, BigDecimal totalSpent, boolean configured) {
        if (!configured || totalBudget == null) {
            return null;
        }
        return totalBudget.subtract(totalSpent);
    }

    private BudgetWarningLevel resolveWarningLevel(BigDecimal usageRate, boolean configured) {
        if (!configured || usageRate == null) {
            return null;
        }
        if (usageRate.compareTo(BigDecimal.ONE) >= 0) {
            return BudgetWarningLevel.OVER_BUDGET;
        }
        if (usageRate.compareTo(new BigDecimal("0.8")) >= 0) {
            return BudgetWarningLevel.NEAR_LIMIT;
        }
        return BudgetWarningLevel.NORMAL;
    }

    private BudgetResponse toResponse(BudgetPlan entity) {
        return new BudgetResponse(
                entity.getId(),
                entity.getBudgetMonth(),
                toApiCategory(entity.getCategory()),
                entity.getAmount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String toApiCategory(String storedCategory) {
        if (isTotalCategory(storedCategory)) {
            return null;
        }
        return storedCategory;
    }

    private boolean isTotalCategory(String storedCategory) {
        return TOTAL_CATEGORY.equals(storedCategory);
    }

    private record BudgetConfiguration(boolean configured, BigDecimal budgetAmount) {
    }
}
