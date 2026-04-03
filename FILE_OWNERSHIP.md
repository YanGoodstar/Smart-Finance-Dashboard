# 文件归属与锁表

## 锁规则

- 锁状态值：`locked`、`shared-by-architect`、`pending-merge`、`frozen`、`free`
- 标记为 `locked` 的文件或目录范围，只能由指定负责人修改
- 标记为 `shared-by-architect` 的文件，必须先获得架构师批准才能修改
- 标记为 `frozen` 的范围，在本阶段默认不允许改动，除非架构师显式解锁
- 在已锁定模块目录下新建的文件，默认继承该模块负责人
- 共享契约文件对非默认负责人只读，除非架构师明确发起锁移交

## 共享文件

| 路径 / 范围 | 负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `pom.xml` | Architect | shared-by-architect | 依赖和插件变更只能由架构师处理 |
| `src/main/resources/application.yml` | Architect | shared-by-architect | 运行配置基线 |
| `src/main/java/com/smartfinance/dashboard/SmartFinanceDashboardApplication.java` | Architect | shared-by-architect | 应用入口 |
| `src/main/java/com/smartfinance/dashboard/common/**` | Architect | shared-by-architect | 通用响应、异常处理和公共配置 |
| `sql/init_schema.sql` | Architect | shared-by-architect | 最终共享 schema 脚本 |
| `sql/manual_update_v1.sql` | Architect | shared-by-architect | 第一阶段共享增量脚本 |
| `docs/backend-stage1-setup.md` | Architect | shared-by-architect | 第一阶段运行说明 |
| `docs/backend-stage2-plan.md` | Architect | locked | 第二阶段并行实施说明 |
| `docs/backend-stage3-plan.md` | Architect | locked | 第三阶段并行实施说明 |
| `docs/product-requirements-v1.md` | Architect | shared-by-architect | 产品需求基线 |
| `docs/solution-design-v1.md` | Architect | shared-by-architect | 方案设计基线 |
| `TASKS.md` | Architect | locked | 第三阶段全局任务看板 |
| `FILE_OWNERSHIP.md` | Architect | locked | 文件锁登记表 |
| `AGENT_RUNBOOK.md` | Architect | locked | Agent 工作手册与提示词 |

## Agent-1 归属

| 路径 / 范围 | 负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `src/main/java/com/smartfinance/dashboard/module/importjob/**` | Agent-1 | locked | 上传、解析、导入任务执行、导入摘要回写 |
| `src/main/java/com/smartfinance/dashboard/module/transaction/**` | Agent-1 | locked | 标准化交易入库、疑似重复标记、交易查询与分类修正 |
| `src/main/resources/mapper/importjob/**` | Agent-1 | locked | 如第三阶段引入 mapper XML，则归 Agent-1 |
| `src/main/resources/mapper/transaction/**` | Agent-1 | locked | 如第三阶段引入 mapper XML，则归 Agent-1 |
| `src/main/resources/import-templates/**` | Agent-1 | locked | 导入模板、示例文件和平台解析说明 |
| `sql/agent-1-stage3-import-source.sql` | Agent-1 | locked | Agent-1 第三阶段局部 SQL |

## Agent-2 归属

| 路径 / 范围 | 负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `src/main/java/com/smartfinance/dashboard/module/dashboard/**` | Agent-2 | locked | 看板趋势、预算提醒、未分类摘要聚合 |
| `src/main/java/com/smartfinance/dashboard/module/budget/**` | Agent-2 | locked | 预算进度、预算预警等级与预算聚合 |
| `src/main/java/com/smartfinance/dashboard/module/rule/**` | Agent-2 | locked | 默认规则目录、用户规则匹配、分类服务契约与实现 |
| `src/main/resources/mapper/dashboard/**` | Agent-2 | locked | 如第三阶段引入 mapper XML，则归 Agent-2 |
| `src/main/resources/mapper/budget/**` | Agent-2 | locked | 如第三阶段引入 mapper XML，则归 Agent-2 |
| `src/main/resources/mapper/rule/**` | Agent-2 | locked | 如第三阶段引入 mapper XML，则归 Agent-2 |
| `src/main/resources/rule-defaults/**` | Agent-2 | locked | 默认规则配置或种子文件 |
| `sql/agent-2-stage3-dashboard-budget.sql` | Agent-2 | locked | Agent-2 第三阶段局部 SQL |

## 本阶段冻结范围

| 路径 / 范围 | 负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `src/main/java/com/smartfinance/dashboard/module/health/**` | Architect | frozen | 第三阶段默认不改健康检查模块 |
| `postman/**` | Architect | frozen | 第三阶段默认不由 Agent 维护联调集合 |
| `target/**` | Architect | frozen | 构建产物目录，不作为开发改动范围 |

## 共享契约文件

| 路径 / 范围 | 默认负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `src/main/java/com/smartfinance/dashboard/module/transaction/entity/TransactionRecord.java` | Agent-1 | shared-by-architect | 导入、看板、预算共同依赖的交易事实模型 |
| `src/main/java/com/smartfinance/dashboard/module/transaction/enums/**` | Agent-1 | shared-by-architect | 交易方向、分类来源等共享语义 |
| `src/main/java/com/smartfinance/dashboard/module/transaction/dto/TransactionQueryRequest.java` | Agent-1 | shared-by-architect | 看板、预算、交易查询复用的筛选 DTO |
| `src/main/java/com/smartfinance/dashboard/module/rule/classification/**` | Agent-2 | shared-by-architect | 自动分类服务输入输出契约 |

## 锁移交流程

1. 通过 `CONTRACT_CHANGE` 消息发起锁移交申请。
2. 架构师评估影响范围，决定拒绝还是授予限时锁窗口。
3. 在锁窗口期间，当前负责人暂停对该文件或范围的修改。
4. 变更完成并合并，或申请被驳回后，锁状态回到默认负责人。

## 当前锁状态

- 基线分支：`yan`
- Agent-1 分支：`feature/agent-1-stage3-import-source-expansion`
- Agent-2 分支：`feature/agent-2-stage3-dashboard-budget-alerts`
- 当前没有临时锁移交
