# 文件归属与锁表

## 锁规则

- 锁状态值：`locked`、`shared-by-architect`、`pending-merge`、`free`
- 标记为 `locked` 的文件或目录范围，只能由指定负责人修改
- 标记为 `shared-by-architect` 的文件，必须先获得架构师批准才能修改
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
| `TASKS.md` | Architect | locked | 全局任务看板 |
| `FILE_OWNERSHIP.md` | Architect | locked | 文件锁登记表 |

## Agent-1 归属

| 路径 / 范围 | 负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `src/main/java/com/smartfinance/dashboard/module/importjob/**` | Agent-1 | locked | 导入任务领域对象、DTO、服务与控制器 |
| `src/main/java/com/smartfinance/dashboard/module/transaction/**` | Agent-1 | locked | 交易查询与分类修正流程 |
| `src/main/resources/mapper/transaction/**` | Agent-1 | locked | 如后续引入 mapper XML，则归 Agent-1 |
| `src/main/resources/mapper/importjob/**` | Agent-1 | locked | 如后续引入 mapper XML，则归 Agent-1 |
| `sql/agent-1-import-transaction.sql` | Agent-1 | locked | Agent-1 局部 SQL 增量脚本 |

## Agent-2 归属

| 路径 / 范围 | 负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `src/main/java/com/smartfinance/dashboard/module/budget/**` | Agent-2 | locked | 预算实体、DTO、服务与控制器 |
| `src/main/java/com/smartfinance/dashboard/module/dashboard/**` | Agent-2 | locked | 看板聚合与接口 |
| `src/main/java/com/smartfinance/dashboard/module/rule/**` | Agent-2 | locked | 规则 CRUD 与语义匹配管理 |
| `src/main/resources/mapper/budget/**` | Agent-2 | locked | 如后续引入 mapper XML，则归 Agent-2 |
| `src/main/resources/mapper/dashboard/**` | Agent-2 | locked | 如后续引入 mapper XML，则归 Agent-2 |
| `src/main/resources/mapper/rule/**` | Agent-2 | locked | 如后续引入 mapper XML，则归 Agent-2 |
| `sql/agent-2-budget-dashboard-rule.sql` | Agent-2 | locked | Agent-2 局部 SQL 增量脚本 |

## 共享契约文件

| 路径 / 范围 | 默认负责人 | 锁状态 | 说明 |
| --- | --- | --- | --- |
| `src/main/java/com/smartfinance/dashboard/module/transaction/entity/TransactionRecord.java` | Agent-1 | shared-by-architect | 共享交易事实模型 |
| `src/main/java/com/smartfinance/dashboard/module/transaction/dto/**` | Agent-1 | shared-by-architect | 被看板和预算逻辑共同消费 |
| `src/main/java/com/smartfinance/dashboard/module/transaction/enums/**` | Agent-1 | shared-by-architect | 分类与收支方向语义 |

## 锁移交流程

1. 通过 `CONTRACT_CHANGE` 消息发起锁移交申请。
2. 架构师评估影响范围，决定拒绝还是授予限时锁窗口。
3. 在锁窗口期间，当前负责人暂停对该文件或范围的修改。
4. 变更完成并合并，或申请被驳回后，锁状态回到默认负责人。

## 当前锁状态

- Agent-1 分支：`feature/agent-1-import-transaction`
- Agent-2 分支：`feature/agent-2-budget-dashboard-rule`
- 共享文件仍由架构师控制
- 当前没有临时锁移交
