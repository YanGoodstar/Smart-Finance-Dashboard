# 后端第三阶段并行开发方案

## 1. 阶段目标

第三阶段的目标是在第二阶段导入与自动分类主链路基础上，继续补齐 Smart Finance Dashboard 后端的看板增强、预算预警和多来源导入扩展，形成以下闭环：

```text
上传微信账单
-> 识别 WECHAT_CSV
-> 解析账单内容
-> 标准化交易候选数据
-> 自动分类
-> 写入 transaction_record
-> 更新 import_job 摘要
-> 聚合趋势统计 / 未分类摘要 / 预算提醒
-> 返回看板总览与预算进度结果
```

本阶段仍仅面向后端，不包含前端页面实现。

## 2. 范围边界

### 2.1 本阶段必须完成

1. 扩展上传接口 `sourceType`，支持 `WECHAT_CSV`
2. 落地微信账单 CSV 解析器与模板说明
3. 让微信账单复用现有自动分类、疑似重复识别、入库与导入摘要回写链路
4. 扩展 dashboard 总览接口，新增趋势统计
5. 扩展 dashboard 总览接口，新增未分类摘要
6. 扩展 budget 进度接口，新增预算预警等级
7. 为 `WECHAT_CSV` 补充默认规则资源
8. 输出第三阶段局部 SQL 与联调说明

### 2.2 本阶段明确不做

1. 不做前端页面开发
2. 不做历史交易回刷分类
3. 不做微信账单多模板全覆盖
4. 不做图表缓存表或预计算表
5. 不做预算历史快照
6. 不做复杂规则 DSL
7. 不做测试先行作为首批阻塞项

## 3. 并行拆分

### 3.1 Agent-1：导入来源扩展 / 微信解析 / 入库复用

负责范围：

1. `importjob` 模块下的上传入口与导入执行编排
2. `WECHAT_CSV` 解析器与模板说明
3. 微信账单到标准化候选 DTO 的映射
4. 微信账单复用现有自动分类、疑似重复、入库与导入摘要回写
5. 导入模块第三阶段局部 SQL 与联调说明

产出重点：

1. 上传接口继续保持 `POST /api/import-jobs/upload`
2. 新增 `WECHAT_CSV` 可联调样例
3. 返回可用于联调的微信导入结果

### 3.2 Agent-2：看板增强 / 预算预警 / 微信默认规则

负责范围：

1. dashboard 趋势统计
2. dashboard 未分类摘要
3. budget 预警等级与预算提醒
4. `WECHAT_CSV` 默认规则资源
5. dashboard / budget / rule 第三阶段局部 SQL 与联调说明

产出重点：

1. 冻结 dashboard 新增返回块契约
2. 冻结预算预警等级语义
3. 保证微信新导入交易能复用现有自动分类能力

## 4. 冻结的共享契约

### 4.1 上传接口

- 路径：`POST /api/import-jobs/upload`
- Content-Type：`multipart/form-data`
- 字段：
  - `file`
  - `sourceType`
- 第三阶段要求支持：
  - `ALIPAY_CSV`
  - `WECHAT_CSV`

### 4.2 看板概览接口

`DashboardOverviewResponse` 在现有基础上新增：

1. `trendPoints`
2. `budgetAlert`
3. `unclassifiedSummary`

#### trendPoints 单项字段

- `date`
- `incomeAmount`
- `expenseAmount`
- `netAmount`

#### budgetAlert 字段

- `configured`
- `warningLevel`
- `budgetAmount`
- `spentAmount`
- `remainingAmount`
- `usageRate`

#### unclassifiedSummary 字段

- `hasUnclassified`
- `transactionCount`
- `incomeAmount`
- `expenseAmount`

### 4.3 预算预警输出

第三阶段预算预警等级固定为：

- `NORMAL`
- `NEAR_LIMIT`
- `OVER_BUDGET`

固定阈值：

1. `usageRate < 0.8` -> `NORMAL`
2. `usageRate >= 0.8 && usageRate < 1.0` -> `NEAR_LIMIT`
3. `usageRate >= 1.0` -> `OVER_BUDGET`

未配置预算时：

1. `configured = false`
2. `warningLevel = null`

`BudgetProgressResponse` 顶层新增：

- `configured`
- `warningLevel`

分类预算进度项新增：

- `warningLevel`

### 4.4 查询与统计口径

- 趋势统计默认按天聚合
- 趋势、预算提醒、未分类摘要继续复用现有交易筛选语义
- 不在本阶段新建新的共享交易查询 DTO

复用字段为：

- `page`
- `size`
- `dateFrom`
- `dateTo`
- `finalCategory`
- `categorySource`
- `keyword`

### 4.5 历史数据边界

1. 第三阶段新增微信默认规则只影响后续新导入交易
2. 第三阶段不做历史交易回刷分类
3. 第三阶段不新增历史预算快照表

## 5. 预算预警与未分类语义

### 5.1 预算预警语义

1. 总预算针对当月全部支出
2. 分类预算仅针对对应分类支出
3. 未设置预算的分类只参与统计，不参与预警
4. 总预算和分类预算可以同时存在

### 5.2 未分类语义

1. 未分类是正式业务状态，不是系统异常
2. 未分类摘要只统计 `finalCategory = 未分类`
3. 看板需要给出未分类交易存在与否的可见提示
4. 未分类交易仍然可以继续在流水页筛选和修正

## 6. 分支与工作树建议

建议新建两套第三阶段工作树：

```powershell
git worktree add .worktrees/agent-1-stage3 -b feature/agent-1-stage3-import-source-expansion yan
git worktree add .worktrees/agent-2-stage3 -b feature/agent-2-stage3-dashboard-budget-alerts yan
```

如果工作树已存在，只需要在各自工作树里切到对应新分支。

## 7. 同步 yan 最新基线

如果 `yan` 本地已经有新提交：

```powershell
git -C F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage3 checkout feature/agent-1-stage3-import-source-expansion
git -C F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage3 merge yan

git -C F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage3 checkout feature/agent-2-stage3-dashboard-budget-alerts
git -C F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage3 merge yan
```

如果需要先拉远端：

```powershell
git fetch origin
git merge origin/yan
```

## 8. 联调验收口径

第三阶段完成后，至少需要通过以下后端联调场景：

1. `POST /api/import-jobs/upload` 可接收 `WECHAT_CSV` 文件
2. 微信导入完成后 `import_job` 状态、总数、成功数、失败数、疑似重复数可查询
3. 微信新导入交易可在 `/api/transactions` 查到
4. dashboard 总览可返回趋势统计、预算提醒和未分类摘要
5. budget 进度接口可返回 `configured` 与 `warningLevel`
6. 微信默认规则命中时，新导入交易可写入 `autoCategory` / `finalCategory`
7. 未命中规则时，交易仍正确保留 `finalCategory = 未分类`

## 9. 架构师集成注意事项

1. Agent-1 和 Agent-2 不直接改共享 SQL 脚本
2. Agent-1 不拥有 dashboard / budget / rule 模块定义权
3. Agent-2 不拥有 importjob / transaction 模块定义权
4. 如确需改动 `TransactionRecord.java`、枚举或共享 DTO，必须先发 `CONTRACT_CHANGE`
5. 第三阶段结束后，由架构师统一完成：
   - 分支差异复查
   - 局部 SQL 合并
   - 构建验证
   - Postman 集合更新
   - 冒烟联调
