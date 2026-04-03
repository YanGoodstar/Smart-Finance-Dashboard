package com.smartfinance.dashboard.module.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleListResponse;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleResponse;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleSaveRequest;
import com.smartfinance.dashboard.module.rule.entity.CategoryRule;
import com.smartfinance.dashboard.module.rule.mapper.CategoryRuleMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RuleServiceImpl implements RuleService {

    private final CategoryRuleMapper categoryRuleMapper;

    public RuleServiceImpl(CategoryRuleMapper categoryRuleMapper) {
        this.categoryRuleMapper = categoryRuleMapper;
    }

    @Override
    public CategoryRuleResponse createRule(CategoryRuleSaveRequest request) {
        CategoryRule entity = new CategoryRule();
        applyRequest(entity, request);
        categoryRuleMapper.insert(entity);
        return toResponse(entity);
    }

    @Override
    public CategoryRuleListResponse listRules(Boolean enabled) {
        LambdaQueryWrapper<CategoryRule> wrapper = new LambdaQueryWrapper<>();
        if (enabled != null) {
            wrapper.eq(CategoryRule::getEnabled, enabled);
        }
        wrapper.orderByAsc(CategoryRule::getPriority)
                .orderByAsc(CategoryRule::getId);

        List<CategoryRuleResponse> items = categoryRuleMapper.selectList(wrapper).stream()
                .map(this::toResponse)
                .toList();
        return new CategoryRuleListResponse(items);
    }

    @Override
    public CategoryRuleResponse getRule(long id) {
        CategoryRule entity = categoryRuleMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        return toResponse(entity);
    }

    @Override
    public CategoryRuleResponse updateRule(long id, CategoryRuleSaveRequest request) {
        CategoryRule entity = categoryRuleMapper.selectById(id);
        if (entity == null) {
            return null;
        }
        entity.setId(id);
        applyRequest(entity, request);
        categoryRuleMapper.updateById(entity);
        return toResponse(entity);
    }

    @Override
    public boolean deleteRule(long id) {
        CategoryRule entity = categoryRuleMapper.selectById(id);
        if (entity == null) {
            return false;
        }
        return categoryRuleMapper.deleteById(id) > 0;
    }

    private void applyRequest(CategoryRule entity, CategoryRuleSaveRequest request) {
        entity.setRuleName(request.ruleName().trim());
        entity.setMatchExpression(request.matchExpression().trim());
        entity.setTargetCategory(request.targetCategory().trim());
        entity.setPriority(request.priority());
        entity.setEnabled(request.enabled());
    }

    private CategoryRuleResponse toResponse(CategoryRule entity) {
        return new CategoryRuleResponse(
                entity.getId(),
                entity.getRuleName(),
                entity.getMatchExpression(),
                entity.getTargetCategory(),
                entity.getPriority(),
                entity.getEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
