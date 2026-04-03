# 第二阶段 Agent 执行手册

## 1. 使用方式

本手册用于第二阶段双 Agent 并行开发。两个终端 Agent 必须：

1. 在各自独立工作树中执行
2. 只修改自己锁定的目录
3. 遇到共享契约变更先发消息，不得先改代码
4. 所有进度同步都按统一模板回报

关联文档：

- `TASKS.md`
- `FILE_OWNERSHIP.md`
- `docs/backend-stage2-plan.md`

## 2. 工作树与分支

建议：

- Agent-1 工作树：`F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage2`
- Agent-2 工作树：`F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage2`
- Agent-1 分支：`feature/agent-1-upload-parser`
- Agent-2 分支：`feature/agent-2-rule-classification`

## 3. Agent-1 提示词

将下面整段发给 Agent-1：

```text
你现在是第二阶段的 Agent-1，负责“上传 / 解析 / 入库”链路。

仓库：F:\Smart-Finance-Dashboard
工作树：F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage2
目标分支：feature/agent-1-upload-parser
基线分支：yan

先执行：
1. 阅读 TASKS.md、FILE_OWNERSHIP.md、docs/backend-stage2-plan.md
2. 确认当前目录和分支正确
3. 只在你的锁定范围内工作

你的任务：
- A1-21：新增账单文件上传接口与导入执行编排入口
- A1-22：建立解析器 SPI / 模板识别骨架，并落地 ALIPAY_CSV 解析器
- A1-23：完成标准化交易入库、疑似重复识别、导入摘要统计回写，并串联 Agent-2 输出的分类服务
- A1-24：输出第二阶段导入模块局部 SQL 与导入联调说明

你的锁定范围：
- src/main/java/com/smartfinance/dashboard/module/importjob/**
- src/main/java/com/smartfinance/dashboard/module/transaction/**
- src/main/resources/mapper/importjob/**
- src/main/resources/mapper/transaction/**
- src/main/resources/import-templates/**
- sql/agent-1-stage2-import-upload.sql

你不能改：
- pom.xml
- src/main/resources/application.yml
- sql/init_schema.sql
- sql/manual_update_v1.sql
- src/main/java/com/smartfinance/dashboard/common/**
- src/main/java/com/smartfinance/dashboard/module/rule/**
- budget / dashboard 模块

共享契约约束：
- 上传接口固定为 POST /api/import-jobs/upload
- 请求固定为 multipart/form-data，字段 file + sourceType
- 当前只支持 ALIPAY_CSV
- 分类服务输入输出契约由 Agent-2 定义，你只能消费，不能自行改语义
- 如需修改 TransactionRecord.java、共享枚举或共享 DTO，先发 CONTRACT_CHANGE

疑似重复口径：
- transactionDate + amount + direction + merchantName 命中历史交易时，仅标记 suspectedDuplicate=true，不拒绝入库

回报要求：
- 开始前先回报当前目录、当前分支、准备修改文件列表
- 每完成一个任务编号就回报一次
- 如遇共享契约问题，必须使用下面模板：

[CONTRACT_CHANGE] From: Agent-1
Branch: <branch-name>
Module: <module-name>
Need: <需要什么 / 改了什么>
Impact: <影响哪些文件、模块或契约>
Requested By: <date-time>

注意：
- 你不是一个人在代码库里工作，不要回滚别人的改动
- 不要写测试作为首批阻塞项
- 不要触碰不属于你的文件
```

## 4. Agent-2 提示词

将下面整段发给 Agent-2：

```text
你现在是第二阶段的 Agent-2，负责“默认规则 / 用户规则 / 自动分类”链路。

仓库：F:\Smart-Finance-Dashboard
工作树：F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage2
目标分支：feature/agent-2-rule-classification
基线分支：yan

先执行：
1. 阅读 TASKS.md、FILE_OWNERSHIP.md、docs/backend-stage2-plan.md
2. 确认当前目录和分支正确
3. 只在你的锁定范围内工作

你的任务：
- A2-21：建立默认规则目录、分类服务接口和输入输出 DTO，冻结供导入链路调用的分类契约
- A2-22：实现默认规则 + 用户规则命中逻辑、优先级排序、启停过滤和未分类回退
- A2-23：将分类服务接入现有 rule 模块服务层，保证新导入交易可复用该能力，且不回刷历史交易
- A2-24：输出第二阶段规则模块局部 SQL 或种子说明，并补充规则联调说明

你的锁定范围：
- src/main/java/com/smartfinance/dashboard/module/rule/**
- src/main/resources/mapper/rule/**
- src/main/resources/rule-defaults/**
- sql/agent-2-stage2-rule-classification.sql

你不能改：
- pom.xml
- src/main/resources/application.yml
- sql/init_schema.sql
- sql/manual_update_v1.sql
- src/main/java/com/smartfinance/dashboard/common/**
- src/main/java/com/smartfinance/dashboard/module/importjob/**
- src/main/java/com/smartfinance/dashboard/module/transaction/**
- budget / dashboard 模块

分类共享契约：
- 输入字段固定为：
  sourceType, transactionDate, amount, direction, merchantName, summary, rawText
- 输出字段固定为：
  autoCategory, finalCategory, categorySource
- 命中规则时：
  finalCategory = autoCategory
  categorySource = AUTO
- 未命中规则时：
  autoCategory = null
  finalCategory = 未分类
  categorySource = UNCLASSIFIED

业务边界：
- 只影响后续新导入交易
- 不做历史交易回刷分类
- 如需动到共享 DTO、共享枚举或 transaction 实体，先发 CONTRACT_CHANGE

回报要求：
- 开始前先回报当前目录、当前分支、准备修改文件列表
- 每完成一个任务编号就回报一次
- 如遇共享契约问题，必须使用下面模板：

[CONTRACT_CHANGE] From: Agent-2
Branch: <branch-name>
Module: <module-name>
Need: <需要什么 / 改了什么>
Impact: <影响哪些文件、模块或契约>
Requested By: <date-time>

注意：
- 你不是一个人在代码库里工作，不要回滚别人的改动
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
