# 任务看板

## 当前阶段

- 当前阶段：第三阶段看板增强、预算预警与多来源导入扩展
- 阶段目标：打通“微信账单导入 -> 自动分类 -> 交易入库 -> 趋势统计 -> 预算预警 -> 看板提醒”的后端闭环
- 基线分支：`yan`
- Agent 分支：
  - `feature/agent-1-stage3-import-source-expansion`
  - `feature/agent-2-stage3-dashboard-budget-alerts`
- 当前仓库范围：仅后端
- 本阶段约束：
  - 延续前两阶段协作方式，由架构师统一调度双 Agent 并行开发
  - 优先完成主链路代码、局部 SQL 和联调说明，不把测试作为首批阻塞项
  - 共享文件只能在架构师批准后修改
  - 第三阶段不做前端页面实现，只补齐后端接口与联调能力

## 第一阶段完成情况

- 已完成后端基础工程、数据库基础模型与核心模块骨架
- 已完成交易查询、预算 CRUD、规则 CRUD、看板总览基础接口
- 已完成第一阶段接口联调与本地 API 冒烟验证

## 第二阶段完成情况

- 已完成真实文件上传接口与 `ALIPAY_CSV` 导入主链路
- 已完成解析器 SPI、标准化交易转换、疑似重复标记与导入摘要回写
- 已完成默认规则与用户规则自动分类能力，并接入导入流程
- 已完成第二阶段合并、联调清单执行与 Postman 集合验证

## 第三阶段全局任务看板

| ID | 模块 | 任务 | 负责人 | 分支 | 状态 | 依赖 | 预计耗时 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ARC-31 | 共享契约 | 冻结第三阶段 `WECHAT_CSV`、看板趋势/预算提醒/未分类摘要契约和预算预警等级语义 | Architect | `yan` | todo | none | 0.25d |
| ARC-32 | 集成验证 | 复查双分支差异，解决共享契约冲突，完成第三阶段集成构建与联调 | Architect | `yan` | todo | A1-34, A2-35 | 0.5d |
| ARC-33 | 共享 SQL / 文档 | 评估并合并两个 Agent 的局部 SQL、模板说明、联调文档与运行说明 | Architect | `yan` | todo | A1-34, A2-35 | 0.25d |
| A1-31 | 导入来源扩展 | 扩展上传入口 `sourceType` 校验，新增 `WECHAT_CSV` 支持并接入解析器路由 | Agent-1 | `feature/agent-1-stage3-import-source-expansion` | todo | ARC-31 | 0.5d |
| A1-32 | 微信解析器 | 落地 `WECHAT_CSV` 解析器与模板说明，完成账单到标准化候选 DTO 的映射 | Agent-1 | `feature/agent-1-stage3-import-source-expansion` | todo | A1-31 | 1.0d |
| A1-33 | 导入链路复用 | 让微信账单复用现有自动分类、疑似重复识别、交易入库与导入摘要回写主链路 | Agent-1 | `feature/agent-1-stage3-import-source-expansion` | todo | A1-32, A2-34 | 0.75d |
| A1-34 | 局部 SQL / 联调 | 输出第三阶段导入模块局部 SQL、微信模板样例与导入联调说明 | Agent-1 | `feature/agent-1-stage3-import-source-expansion` | todo | A1-33 | 0.5d |
| A2-31 | 看板趋势 | 扩展 dashboard 总览接口，新增按天聚合的趋势统计 `trendPoints` | Agent-2 | `feature/agent-2-stage3-dashboard-budget-alerts` | todo | ARC-31 | 0.75d |
| A2-32 | 未分类摘要 | 扩展 dashboard 总览接口，新增 `unclassifiedSummary`，输出未分类笔数与金额摘要 | Agent-2 | `feature/agent-2-stage3-dashboard-budget-alerts` | todo | A2-31 | 0.5d |
| A2-33 | 预算预警 | 扩展预算进度返回，新增 `configured` 与 `warningLevel`，实现预算预警等级计算 | Agent-2 | `feature/agent-2-stage3-dashboard-budget-alerts` | todo | ARC-31 | 1.0d |
| A2-34 | 默认规则扩展 | 为 `WECHAT_CSV` 补充默认分类规则，保证微信新导入交易可复用现有自动分类链路 | Agent-2 | `feature/agent-2-stage3-dashboard-budget-alerts` | todo | A2-31 | 0.5d |
| A2-35 | 局部 SQL / 联调 | 输出第三阶段 dashboard / budget / rule 局部 SQL 或联调说明收口文件 | Agent-2 | `feature/agent-2-stage3-dashboard-budget-alerts` | todo | A2-32, A2-33, A2-34 | 0.25d |

## 冻结的第三阶段共享契约

### 上传接口契约

- 上传入口继续固定为：`POST /api/import-jobs/upload`
- 请求方式继续固定为：`multipart/form-data`
- 最小请求字段继续固定为：
  - `file`
  - `sourceType`
- 第三阶段要求支持：
  - `ALIPAY_CSV`
  - `WECHAT_CSV`

### 看板概览契约

- `DashboardOverviewResponse` 在现有基础上新增以下返回块：
  - `trendPoints`
  - `budgetAlert`
  - `unclassifiedSummary`
- `trendPoints` 单项字段冻结为：
  - `date`
  - `incomeAmount`
  - `expenseAmount`
  - `netAmount`
- `budgetAlert` 字段冻结为：
  - `configured`
  - `warningLevel`
  - `budgetAmount`
  - `spentAmount`
  - `remainingAmount`
  - `usageRate`
- `unclassifiedSummary` 字段冻结为：
  - `hasUnclassified`
  - `transactionCount`
  - `incomeAmount`
  - `expenseAmount`

### 预算预警契约

- 第三阶段预算预警等级冻结为：
  - `NORMAL`
  - `NEAR_LIMIT`
  - `OVER_BUDGET`
- 预警阈值冻结为：
  - `usageRate < 0.8` -> `NORMAL`
  - `usageRate >= 0.8 && usageRate < 1.0` -> `NEAR_LIMIT`
  - `usageRate >= 1.0` -> `OVER_BUDGET`
- 未配置预算时：
  - `configured = false`
  - `warningLevel = null`
- `BudgetProgressResponse` 顶层新增：
  - `configured`
  - `warningLevel`
- 分类预算进度项新增：
  - `warningLevel`

### 查询与统计口径

- 趋势统计默认按天聚合
- 趋势、预算提醒、未分类摘要继续复用现有交易筛选语义：
  - `page`
  - `size`
  - `dateFrom`
  - `dateTo`
  - `finalCategory`
  - `categorySource`
  - `keyword`
- 不在第三阶段新建新的共享交易查询 DTO

### 历史数据处理边界

- 第三阶段新增的微信默认规则只影响“后续新导入交易”
- 第三阶段不做历史交易回刷分类
- 第三阶段不做预算历史快照表
- 第三阶段不做图表缓存表或预计算表

## 依赖协调规则

1. 架构师先冻结 `WECHAT_CSV`、看板新增字段和预算预警等级语义，Agent 再开始编码。
2. Agent-1 可以先完成 `A1-31` 和 `A1-32`，不依赖 Agent-2。
3. 若微信样例账单需要导入后立即命中默认分类，`A1-33` 的最终联调依赖 `A2-34`。
4. Agent-2 可以修改 `dashboard`、`budget`、`rule` 模块，但不得直接改 `TransactionQueryRequest.java`、`TransactionRecord.java` 或 `transaction/enums/**`；如确需调整，必须先发 `CONTRACT_CHANGE`。
5. Agent-1 可以修改 `importjob`、`transaction` 模块，但不得直接改 `rule`、`budget`、`dashboard` 模块。
6. 如果任一 Agent 认为需要新增共享 SQL 或改最终 schema，先在局部 SQL 中提出，不得直接修改 `sql/init_schema.sql`。

## 通信协议

### 消息类型

- `INFO`：非阻塞进度同步
- `BLOCKER`：当前任务无法继续，需要协调
- `CONTRACT_CHANGE`：共享 DTO、接口、枚举、SQL 结构或跨模块边界需要变更

### 消息模板

```text
[TYPE] From: <Agent-1|Agent-2>
Branch: <branch-name>
Module: <module-name>
Need: <需要什么 / 改了什么>
Impact: <影响哪些文件、模块或契约>
Requested By: <date-time>
```

### 路由规则

1. Agent 之间不直接跨模块沟通，统一先发给架构师。
2. 架构师负责判断消息是普通同步、阻塞问题还是共享契约变更。
3. 如果涉及共享契约，架构师必须先更新 `TASKS.md` 和 `FILE_OWNERSHIP.md`，再转发调度指令。
4. `BLOCKER` 优先级高于 `INFO`。

## 执行说明

- Agent-1 负责导入来源扩展、微信 CSV 解析、标准化、入库复用与模板联调说明。
- Agent-2 负责看板趋势、未分类提醒、预算预警和微信默认规则扩展。
- 架构师负责共享文件、契约冻结、最终合并和冲突仲裁。
- 未经锁移交批准，任何 Agent 不得修改：
  - `pom.xml`
  - `src/main/resources/application.yml`
  - `sql/init_schema.sql`
  - `sql/manual_update_v1.sql`
  - `src/main/java/com/smartfinance/dashboard/common/**`
