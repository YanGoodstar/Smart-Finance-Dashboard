package com.smartfinance.dashboard.module.rule;

import com.smartfinance.dashboard.common.api.ApiResponse;
import com.smartfinance.dashboard.common.api.ErrorCode;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleListResponse;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleResponse;
import com.smartfinance.dashboard.module.rule.dto.CategoryRuleSaveRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PostMapping
    public ApiResponse<CategoryRuleResponse> createRule(@Valid @RequestBody CategoryRuleSaveRequest request) {
        return ApiResponse.success(ruleService.createRule(request));
    }

    @GetMapping
    public ApiResponse<CategoryRuleListResponse> listRules(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.success(ruleService.listRules(enabled));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryRuleResponse>> getRule(@PathVariable @Min(1) long id) {
        CategoryRuleResponse response = ruleService.getRule(id);
        if (response == null) {
            return notFound("Rule not found");
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryRuleResponse>> updateRule(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody CategoryRuleSaveRequest request
    ) {
        CategoryRuleResponse response = ruleService.updateRule(id, request);
        if (response == null) {
            return notFound("Rule not found");
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable @Min(1) long id) {
        if (!ruleService.deleteRule(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "Rule not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private ResponseEntity<ApiResponse<CategoryRuleResponse>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(ErrorCode.VALIDATION_ERROR, message));
    }
}
