# 第三阶段 Agent 执行手册

## 1. 使用方式

本手册用于第三阶段双 Agent 并行开发。两个终端 Agent 必须：

1. 在各自独立工作树中执行
2. 只修改自己锁定的目录
3. 遇到共享契约变更先发消息，不得先改代码
4. 所有进度同步都按统一模板回报

关联文档：

- `TASKS.md`
- `FILE_OWNERSHIP.md`
- `docs/backend-stage3-plan.md`

## 2. 工作树与分支

建议：

- Agent-1 工作树：`F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage3`
- Agent-2 工作树：`F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage3`
- Agent-1 分支：`feature/agent-1-stage3-import-source-expansion`
- Agent-2 分支：`feature/agent-2-stage3-dashboard-budget-alerts`

如果工作树已存在，只需要在各自工作树里切到对应新分支。

## 3. Agent-1 提示词

将下面整段发给 Agent-1：

```text
你现在是第三阶段的 Agent-1，负责“导入来源扩展 / 微信账单解析 / 导入联调收口”链路。

仓库：F:\Smart-Finance-Dashboard
工作树：F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage3
目标分支：feature/agent-1-stage3-import-source-expansion
基线分支：yan

注意：
1. 你不是一个人在代码库里工作，不要回滚别人的改动
2. 本轮以这份提示词作为第三阶段执行基线；旧阶段文档只作背景参考
3. 不要先写测试，不要把测试作为首批阻塞项
4. 没有明确授权前，不要改共享文件和 Agent-2 范围

先执行：
1. 阅读 TASKS.md、FILE_OWNERSHIP.md、docs/backend-stage3-plan.md
2. 确认当前目录和分支正确
3. 只在你的锁定范围内工作

你的任务：
- A1-31：扩展 sourceType，新增 WECHAT_CSV 支持与上传校验
- A1-32：新增微信账单 CSV 解析器，并接入现有解析器 SPI / resolver
- A1-33：让微信账单复用现有自动分类、疑似重复识别、入库、import_job 摘要回写主链路
- A1-34：输出第三阶段导入模块局部 SQL、模板样例和联调说明

你的锁定范围：
- src/main/java/com/smartfinance/dashboard/module/importjob/**
- src/main/java/com/smartfinance/dashboard/module/transaction/**
- src/main/resources/mapper/importjob/**
- src/main/resources/mapper/transaction/**
- src/main/resources/import-templates/**
- sql/agent-1-stage3-import-source.sql

你不能改：
- pom.xml
- src/main/resources/application.yml
- sql/init_schema.sql
- sql/manual_update_v1.sql
- src/main/java/com/smartfinance/dashboard/common/**
- src/main/java/com/smartfinance/dashboard/module/rule/**
- src/main/java/com/smartfinance/dashboard/module/budget/**
- src/main/java/com/smartfinance/dashboard/module/dashboard/**
- TASKS.md
- FILE_OWNERSHIP.md
- AGENT_RUNBOOK.md

共享契约约束：
- 上传接口继续固定为 POST /api/import-jobs/upload
- 请求继续固定为 multipart/form-data，字段 file + sourceType
- 第三阶段必须支持 ALIPAY_CSV 和 WECHAT_CSV
- 你只能消费 Agent-2 暴露的 rule 服务能力，不要改 rule 模块
- 如需修改 TransactionRecord.java、共享枚举或共享 DTO，先发 CONTRACT_CHANGE

实现边界：
- 本轮只支持微信 CSV，不支持 PDF、图片、Excel
- 本轮只要求支持联调用微信账单样例模板，不追求覆盖所有微信账单变体
- 微信账单解析后继续映射到现有标准化候选字段
- 不为微信样例扩展共享实体

导入链路要求：
- ImportParserResolver 必须能根据 sourceType 路由到微信解析器
- 微信解析后的候选数据必须继续走：
  - RuleService#classifyTransaction(...)
  - suspectedDuplicate 判断
  - transaction_record 入库
  - import_job 状态 / 统计回写

回报要求：
- 开始前先回报当前目录、当前分支、当前任务编号、准备修改文件列表
- 每完成一个任务编号就回报一次
- 如遇共享契约问题，必须使用下面模板：

[CONTRACT_CHANGE] From: Agent-1
Branch: <branch-name>
Module: <module-name>
Need: <需要什么 / 改了什么>
Impact: <影响哪些文件、模块或契约>
Requested By: <date-time>

注意：
- 不要写测试作为首批阻塞项
- 不要触碰不属于你的文件
```

## 4. Agent-2 提示词

将下面整段发给 Agent-2：

```text
你现在是第三阶段的 Agent-2，负责“看板增强 / 预算预警 / 微信默认分类规则”链路。

仓库：F:\Smart-Finance-Dashboard
工作树：F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage3
目标分支：feature/agent-2-stage3-dashboard-budget-alerts
基线分支：yan

注意：
1. 你不是一个人在代码库里工作，不要回滚别人的改动
2. 本轮以这份提示词作为第三阶段执行基线；旧阶段文档只作背景参考
3. 不要先写测试，不要把测试作为首批阻塞项
4. 没有明确授权前，不要改共享文件和 Agent-1 范围

先执行：
1. 阅读 TASKS.md、FILE_OWNERSHIP.md、docs/backend-stage3-plan.md
2. 确认当前目录和分支正确
3. 只在你的锁定范围内工作

你的任务：
- A2-31：扩展 dashboard 总览返回，新增趋势统计 trendPoints
- A2-32：扩展 dashboard 总览返回，新增未分类摘要 unclassifiedSummary
- A2-33：扩展预算进度返回，新增 configured 与 warningLevel
- A2-34：补充 WECHAT_CSV 默认规则资源，并保证微信新导入交易可复用现有分类链路
- A2-35：输出第三阶段 dashboard / budget / rule 局部 SQL 或联调说明收口文件

你的锁定范围：
- src/main/java/com/smartfinance/dashboard/module/dashboard/**
- src/main/java/com/smartfinance/dashboard/module/budget/**
- src/main/java/com/smartfinance/dashboard/module/rule/**
- src/main/resources/mapper/dashboard/**
- src/main/resources/mapper/budget/**
- src/main/resources/mapper/rule/**
- src/main/resources/rule-defaults/**
- sql/agent-2-stage3-dashboard-budget.sql

你不能改：
- pom.xml
- src/main/resources/application.yml
- sql/init_schema.sql
- sql/manual_update_v1.sql
- src/main/java/com/smartfinance/dashboard/common/**
- src/main/java/com/smartfinance/dashboard/module/importjob/**
- src/main/java/com/smartfinance/dashboard/module/transaction/**
- TASKS.md
- FILE_OWNERSHIP.md
- AGENT_RUNBOOK.md

共享契约约束：
- dashboard 新增返回块固定为：
  - trendPoints
  - budgetAlert
  - unclassifiedSummary
- 预算预警等级固定为：
  - NORMAL
  - NEAR_LIMIT
  - OVER_BUDGET
- 预警阈值固定为：
  - usageRate < 0.8 -> NORMAL
  - usageRate >= 0.8 且 < 1.0 -> NEAR_LIMIT
  - usageRate >= 1.0 -> OVER_BUDGET
- 未配置预算时：
  - configured = false
  - warningLevel = null
- 如需修改 TransactionQueryRequest.java、TransactionRecord.java 或共享枚举，先发 CONTRACT_CHANGE

业务边界：
- 趋势统计默认按天聚合
- 趋势、预算提醒、未分类摘要继续复用现有交易筛选语义
- 未分类摘要只统计 finalCategory = 未分类 的交易
- 只影响后续新导入交易，不做历史交易回刷分类
- 只补 WECHAT_CSV 默认规则，不引入复杂 DSL

回报要求：
- 开始前先回报当前目录、当前分支、当前任务编号、准备修改文件列表
- 每完成一个任务编号就回报一次
- 如遇共享契约问题，必须使用下面模板：

[CONTRACT_CHANGE] From: Agent-2
Branch: <branch-name>
Module: <module-name>
Need: <需要什么 / 改了什么>
Impact: <影响哪些文件、模块或契约>
Requested By: <date-time>

注意：
- 不要写测试作为首批阻塞项
- 不要触碰不属于你的文件
```

## 5. 统一进度回报格式

普通进度同步：

```text
[INFO] From: <Agent-1|Agent-2>
Branch: <branch-name>
Module: <module-name>
Need: <当前完成了什么 / 下一步做什么>
Impact: <本轮改了哪些文件或范围>
Requested By: <date-time>
```

阻塞消息：

```text
[BLOCKER] From: <Agent-1|Agent-2>
Branch: <branch-name>
Module: <module-name>
Need: <当前卡点是什么>
Impact: <阻塞了哪些任务>
Requested By: <date-time>
```

## 6. 如何同步 yan 最新提交

如果 `yan` 有新提交，需要在各自工作树执行：

```powershell
git checkout <自己的分支>
git merge yan
```

如果还没有本地最新远端引用，先执行：

```powershell
git fetch origin
git merge origin/yan
```

## 7. 架构师集成规则

1. 两个 Agent 都完成并提交后，再开始合并
2. 先合并无共享契约风险的一侧，再处理有共享边界的一侧
3. 合并前必须检查：
   - 本地 SQL 是否只在局部脚本中改动
   - 是否越权修改共享文件
   - 是否触发未上报的 CONTRACT_CHANGE
4. 最终共享脚本、共享文档和异常处理由架构师统一收口
