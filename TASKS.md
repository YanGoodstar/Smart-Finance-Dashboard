# 任务看板

## 总览

- 协作模式：由架构师统一调度的双 Agent 并行开发
- 基线分支：`yan`
- Agent 分支：
  - `feature/agent-1-import-transaction`
  - `feature/agent-2-budget-dashboard-rule`
- 状态值：`todo`、`in_progress`、`blocked`、`review`、`done`
- 当前仓库范围：仅后端
- 规则：共享文件只能在架构师批准后修改

## 全局任务看板

| ID | 模块 | 任务 | 负责人 | 分支 | 状态 | 依赖 | 预计耗时 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ARC-01 | 共享契约 | 冻结交易事实模型、查询筛选字段和分类字段语义，供下游模块统一使用 | Architect | `yan` | todo | none | 0.25d |
| ARC-02 | 共享 SQL | 合并两个 Agent 的局部 SQL 增量脚本，生成最终共享脚本 | Architect | `yan` | todo | A1-05, A2-05 | 0.25d |
| ARC-03 | 集成验证 | 复查分支差异，解决共享契约冲突，并验证项目可构建 | Architect | `yan` | todo | A1-05, A2-05 | 0.5d |
| A1-01 | 导入 | 创建导入模块包结构、请求/响应 DTO 和 controller/service 入口 | Agent-1 | `feature/agent-1-import-transaction` | todo | ARC-01 | 0.5d |
| A1-02 | 导入 | 扩展导入任务模型，支持处理摘要与执行状态跟踪 | Agent-1 | `feature/agent-1-import-transaction` | todo | A1-01 | 0.5d |
| A1-03 | 交易 | 扩展交易查询契约，支持日期、分类、来源、关键字筛选，并供聚合模块复用 | Agent-1 | `feature/agent-1-import-transaction` | todo | ARC-01 | 0.5d |
| A1-04 | 交易 | 增加交易分类人工修正接口与服务流程 | Agent-1 | `feature/agent-1-import-transaction` | todo | A1-03 | 0.5d |
| A1-05 | SQL | 产出导入与交易模块的 Agent 局部 SQL 增量脚本 | Agent-1 | `feature/agent-1-import-transaction` | todo | A1-02, A1-04 | 0.5d |
| A2-01 | 预算 | 创建预算模块包结构、实体、DTO 和 controller/service 入口 | Agent-2 | `feature/agent-2-budget-dashboard-rule` | todo | ARC-01 | 0.5d |
| A2-02 | 规则 | 创建规则模块包结构与语义分类规则 CRUD 契约 | Agent-2 | `feature/agent-2-budget-dashboard-rule` | todo | ARC-01 | 0.5d |
| A2-03 | 看板 | 基于交易事实数据创建看板聚合契约与服务 | Agent-2 | `feature/agent-2-budget-dashboard-rule` | todo | A1-03 | 0.5d |
| A2-04 | 预算 | 实现月总预算与分类预算的计算契约 | Agent-2 | `feature/agent-2-budget-dashboard-rule` | todo | A1-03, A2-01 | 1.0d |
| A2-05 | SQL | 产出预算、看板、规则模块的 Agent 局部 SQL 增量脚本 | Agent-2 | `feature/agent-2-budget-dashboard-rule` | todo | A2-02, A2-04 | 0.5d |

## 依赖协调

### 冻结的共享契约

- 以下交易事实字段是两个 Agent 共用的事实真相：
  - `transactionDate`
  - `amount`
  - `direction`
  - `merchantName`
  - `summary`
  - `autoCategory`
  - `finalCategory`
  - `categorySource`
  - `suspectedDuplicate`
- 时间归属规则：所有看板和预算计算统一使用 `transactionDate`，按自然月归属
- 分类优先级规则：下游预算和看板统计只能使用 `finalCategory`

### 协调规则

1. Agent-1 必须先完成 `A1-03`，Agent-2 才能最终敲定看板与预算聚合契约。
2. Agent-2 可以先搭建 `budget`、`dashboard`、`rule` 模块骨架，但不能提前锁死查询语义。
3. SQL 修改必须先分别产出在各自的局部脚本中，最终共享脚本只能由架构师合并。
4. 任何共享契约变更，都必须先按下面的消息模板上报，再开始改代码。

## 通信协议

### 消息类型

- `INFO`：非阻塞的进度同步
- `BLOCKER`：当前任务无法继续，需要协调
- `CONTRACT_CHANGE`：共享字段、DTO、枚举或 SQL 结构需要变更

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
4. `BLOCKER` 消息优先级高于普通进度同步。

## 执行说明

- Agent-1 负责导入与交易主链路。
- Agent-2 负责预算、看板与规则模块。
- 架构师负责共享文件、最终合并和冲突仲裁。
- 未经锁移交批准，任何 Agent 不得修改 `pom.xml`、`application.yml` 或最终共享 SQL 脚本。
