-- Smart Finance Dashboard V1 manual incremental update script.
-- Apply this script only if the database schema was already initialized
-- from sql/init_schema.sql before the Agent-1 / Agent-2 integration changes.

-- Agent-1 incremental changes
ALTER TABLE import_job
    ADD COLUMN total_count INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN processed_count INT NOT NULL DEFAULT 0 AFTER total_count;

-- Agent-2 incremental changes: budget module
-- Total monthly budget is stored as category='__TOTAL__' so the
-- (budget_month, category) unique constraint can also cover total budgets.
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

-- Agent-2 incremental changes: rule module
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
