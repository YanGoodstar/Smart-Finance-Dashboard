# 任务看板

## 当前阶段

- 当前阶段：第二阶段导入与分类主链路开发
- 阶段目标：打通“上传账单文件 -> 解析标准化 -> 自动分类 -> 入库 -> 返回导入结果摘要”后端闭环
- 基线分支：`yan`
- Agent 分支：
  - `feature/agent-1-upload-parser`
  - `feature/agent-2-rule-classification`
- 当前仓库范围：仅后端
- 本阶段约束：
  - 延续第一阶段协作方式，由架构师统一调度双 Agent 并行开发
  - 优先完成主链路代码与 SQL，不把测试作为首批阻塞项
  - 共享文件只能在架构师批准后修改
  - 预算、看板模块在本阶段冻结，不作为默认改动范围

## 第一阶段完成情况

- 已完成后端基础工程、交易查询、预算、规则 CRUD、看板聚合、导入任务跟踪骨架
- 已完成第一阶段联调与本地 API 冒烟验证
- 已知第二阶段核心缺口：
  - 还没有真实文件上传入口
  - 还没有解析器框架和平台适配器
  - 还没有导入执行编排与交易批量入库
  - 还没有默认规则 + 用户规则的自动分类执行链路

## 第二阶段全局任务看板

| ID | 模块 | 任务 | 负责人 | 分支 | 状态 | 依赖 | 预计耗时 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ARC-21 | 共享契约 | 冻结第二阶段上传接口、分类服务输入输出、未分类语义和疑似重复判定口径 | Architect | `yan` | done | none | 0.25d |
| ARC-22 | 集成验证 | 复查双分支差异，解决共享契约冲突，完成第二阶段集成构建与联调 | Architect | `yan` | todo | A1-24, A2-24 | 0.5d |
| ARC-23 | 共享 SQL / 文档 | 评估并合并两个 Agent 的局部 SQL、联调文档和运行说明 | Architect | `yan` | todo | A1-24, A2-24 | 0.25d |
| A1-21 | 上传入口 | 新增账单文件上传接口与导入执行编排入口，完成 multipart 接收、文件校验、导入任务生命周期更新 | Agent-1 | `feature/agent-1-upload-parser` | todo | ARC-21 | 0.5d |
| A1-22 | 解析器 | 建立解析器 SPI / 模板识别骨架，并落地 `ALIPAY_CSV` 解析器与标准化候选 DTO | Agent-1 | `feature/agent-1-upload-parser` | todo | A1-21 | 1.0d |
| A1-23 | 入库执行 | 完成标准化交易入库、疑似重复识别、导入摘要统计回写，并串联 Agent-2 输出的分类服务 | Agent-1 | `feature/agent-1-upload-parser` | todo | A1-22, A2-22 | 1.0d |
| A1-24 | 局部 SQL / 联调 | 输出第二阶段导入模块局部 SQL 与导入联调说明，收口 Agent-1 范围文档 | Agent-1 | `feature/agent-1-upload-parser` | todo | A1-23 | 0.5d |
| A2-21 | 分类契约 | 建立默认规则目录、分类服务接口和输入输出 DTO，冻结供导入链路调用的分类契约 | Agent-2 | `feature/agent-2-rule-classification` | todo | ARC-21 | 0.5d |
| A2-22 | 规则匹配 | 实现默认规则 + 用户规则命中逻辑、优先级排序、启停过滤和未分类回退语义 | Agent-2 | `feature/agent-2-rule-classification` | todo | A2-21 | 1.0d |
| A2-23 | 规则集成 | 将分类服务接入现有 rule 模块服务层，保证新导入交易可复用该能力，且不回刷历史交易 | Agent-2 | `feature/agent-2-rule-classification` | todo | A2-22 | 0.75d |
| A2-24 | 局部 SQL / 联调 | 输出第二阶段规则模块局部 SQL 或种子说明，并补充规则联调说明 | Agent-2 | `feature/agent-2-rule-classification` | todo | A2-23 | 0.25d |

## 冻结的第二阶段共享契约

### 上传接口契约

- 第二阶段上传入口固定为：`POST /api/import-jobs/upload`
- 请求方式固定为：`multipart/form-data`
- 最小请求字段固定为：
  - `file`
  - `sourceType`
- 本轮只要求支持：`ALIPAY_CSV`

### 分类服务输入契约

- Agent-2 对外提供的分类输入字段冻结为：
  - `sourceType`
  - `transactionDate`
  - `amount`
  - `direction`
  - `merchantName`
  - `summary`
  - `rawText`

### 分类服务输出契约

- 分类输出字段冻结为：
  - `autoCategory`
  - `finalCategory`
  - `categorySource`
- 命中默认规则或用户规则时：
  - `finalCategory = autoCategory`
  - `categorySource = AUTO`
- 未命中任何规则时：
  - `autoCategory = null`
  - `finalCategory = 未分类`
  - `categorySource = UNCLASSIFIED`
- 人工修正链路继续沿用第一阶段语义：
  - `categorySource = MANUAL`

### 疑似重复判定口径

- 第二阶段默认疑似重复判定口径冻结为：
  - `transactionDate`
  - `amount`
  - `direction`
  - `merchantName`
- 命中上述四元组相同的历史交易时，标记 `suspectedDuplicate = true`
- 第二阶段只做“疑似重复标记”，不做硬拒绝入库

### 历史数据处理边界

- 第二阶段新增规则只影响“后续新导入交易”
- 第二阶段不做历史交易回刷分类
- 任何历史回刷能力都必须单独立项，不得在本轮顺手实现

## 依赖协调规则

1. Agent-2 必须先完成 `A2-21`，架构师确认分类契约后，Agent-1 才能开始 `A1-23` 的最终串联。
2. Agent-1 可以先搭建上传入口、解析器框架和导入执行骨架，但不能自行发明分类输出字段。
3. 如果 Agent-1 认为 `transaction_record` 或 `import_job` 必须新增字段，先在局部 SQL 脚本中提出，不得直接改最终共享脚本。
4. 如果 Agent-2 认为 `category_rule` 表结构不够，需要先发 `CONTRACT_CHANGE`，由架构师判断是否放进第二阶段。
5. 本阶段预算、看板模块默认冻结，任何触碰都必须先发 `BLOCKER` 或 `CONTRACT_CHANGE`。

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

- Agent-1 负责上传、解析、标准化、入库、导入摘要与疑似重复标记。
- Agent-2 负责默认规则、用户规则匹配、未分类回退和分类服务契约。
- 架构师负责共享文件、契约冻结、最终合并和冲突仲裁。
- 未经锁移交批准，任何 Agent 不得修改：
  - `pom.xml`
  - `src/main/resources/application.yml`
  - `sql/init_schema.sql`
  - `sql/manual_update_v1.sql`
  - `src/main/java/com/smartfinance/dashboard/common/**`
