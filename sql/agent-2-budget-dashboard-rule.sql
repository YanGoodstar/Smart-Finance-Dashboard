-- Agent-2 local incremental SQL for A2-05 closure.
-- Covered schema in this file:
-- 1. budget_plan: budget module persistence
-- 2. category_rule: rule module persistence
-- 3. dashboard: no new table or field in this round
-- Dashboard aggregation and budget progress calculation reuse existing
-- transaction_record data and do not require additional DDL here.

-- Budget module
-- Total monthly budget is persisted with category='__TOTAL__'
-- so the (budget_month, category) unique constraint is effective.
CREATE TABLE budget_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    budget_month DATE NOT NULL,
    category VARCHAR(64) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE UNIQUE INDEX uk_budget_plan_month_category
    ON budget_plan (budget_month, category);

CREATE INDEX idx_budget_plan_month
    ON budget_plan (budget_month);

-- Rule module
CREATE TABLE category_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_name VARCHAR(128) NOT NULL,
    match_expression VARCHAR(512) NOT NULL,
    target_category VARCHAR(64) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
);

CREATE INDEX idx_category_rule_enabled_priority
    ON category_rule (enabled, priority, id);

CREATE UNIQUE INDEX uk_category_rule_name
    ON category_rule (rule_name);

-- Dashboard module:
-- No table, view, or field is introduced in this phase.
