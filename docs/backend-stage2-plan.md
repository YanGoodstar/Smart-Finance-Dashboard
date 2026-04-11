# 后端第二阶段并行开发方案

## 1. 阶段目标

第二阶段的目标是打通 Smart Finance Dashboard 后端的导入与自动分类主链路，形成以下闭环：

```text
上传账单文件
-> 识别 sourceType
-> 解析账单内容
-> 标准化交易候选数据
-> 自动分类
-> 写入 transaction_record
-> 更新 import_job 摘要
-> 返回导入结果
```

本阶段仅面向后端，不包含前端页面实现。

## 2. 范围边界

### 2.1 本阶段必须完成

1. 新增真实文件上传接口
2. 新增导入执行编排入口
3. 落地解析器框架
4. 支持 `ALIPAY_CSV` 单一来源类型
5. 将账单内容标准化为内部交易结构
6. 基于默认规则 + 用户规则完成自动分类
7. 将分类后的交易写入 `transaction_record`
8. 标记疑似重复交易
9. 回写 `import_job` 的处理摘要和状态

### 2.2 本阶段明确不做

1. 不做前端上传页
2. 不做多平台模板全覆盖
3. 不做历史交易回刷分类
4. 不做复杂规则 DSL
5. 不做预算、看板二次扩展
6. 不做测试先行作为首批阻塞项

## 3. 并行拆分

### 3.1 Agent-1：上传 / 解析 / 入库

负责范围：

1. `importjob` 模块下的上传接口和导入执行编排
2. 解析器 SPI、模板识别骨架、`ALIPAY_CSV` 解析器
3. 标准化候选 DTO
4. 交易批量入库与疑似重复识别
5. 导入结果摘要回写

产出重点：

1. `POST /api/import-jobs/upload`
2. 同步完成一次上传导入
3. 返回可用于联调的导入任务结果

### 3.2 Agent-2：默认规则 / 用户规则 / 自动分类

负责范围：

1. 默认规则目录
2. 用户规则加载与启停过滤
3. 分类服务接口与输入输出 DTO
4. 规则命中优先级与未分类回退
5. 对导入链路暴露可复用的分类能力

产出重点：

1. 冻结分类服务契约
2. 自动分类结果统一输出
3. 不回刷历史交易，只影响后续新导入数据

## 4. 冻结的共享契约

### 4.1 上传接口

- 路径：`POST /api/import-jobs/upload`
- Content-Type：`multipart/form-data`
- 字段：
  - `file`
  - `sourceType`
- 当前仅支持：`ALIPAY_CSV`

### 4.2 分类服务输入

- `sourceType`
- `transactionDate`
- `amount`
- `direction`
- `merchantName`
- `summary`
- `rawText`

### 4.3 分类服务输出

- `autoCategory`
- `finalCategory`
- `categorySource`

固定语义：

1. 命中规则：
   - `finalCategory = autoCategory`
   - `categorySource = AUTO`
2. 未命中规则：
   - `autoCategory = null`
   - `finalCategory = 未分类`
   - `categorySource = UNCLASSIFIED`

## 5. 疑似重复口径

本阶段疑似重复仅做标记，不做拒绝入库。默认判定口径：

1. `transactionDate`
2. `amount`
3. `direction`
4. `merchantName`

当上述四元组命中历史交易时，写入：

- `suspectedDuplicate = true`

## 6. 分支与工作树建议

建议新建两套第二阶段工作树：

```powershell
git worktree add .worktrees/agent-1-stage2 -b feature/agent-1-upload-parser yan
git worktree add .worktrees/agent-2-stage2 -b feature/agent-2-rule-classification yan
```

如果工作树已存在，只需要在各自工作树里切到对应新分支。

## 7. 同步 yan 最新基线

如果 `yan` 本地已经有新提交：

```powershell
git -C F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage2 checkout feature/agent-1-upload-parser
git -C F:\Smart-Finance-Dashboard\.worktrees\agent-1-stage2 merge yan

git -C F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage2 checkout feature/agent-2-rule-classification
git -C F:\Smart-Finance-Dashboard\.worktrees\agent-2-stage2 merge yan
```

如果需要先拉远端：

```powershell
git fetch origin
git merge origin/yan
```

## 8. 联调验收口径

第二阶段完成后，至少需要通过以下后端联调场景：

1. `POST /api/import-jobs/upload` 可接收 `ALIPAY_CSV` 文件
2. 导入完成后 `import_job` 状态、总数、成功数、失败数、疑似重复数可查询
3. 新导入交易可在 `/api/transactions` 查到
4. 规则命中时写入 `autoCategory` / `finalCategory`
5. 未命中规则时写入 `finalCategory = 未分类`
6. 人工修正接口仍然可覆盖自动分类结果

## 9. 架构师集成注意事项

1. Agent-1 和 Agent-2 不直接改共享 SQL 脚本
2. Agent-1 消费 Agent-2 的分类契约，但不拥有其定义权
3. 如确需改动 `TransactionRecord.java`、枚举或共享 DTO，必须先发 `CONTRACT_CHANGE`
4. 第二阶段结束后，由架构师统一完成：
   - 分支差异复查
   - 局部 SQL 合并
   - 构建验证
   - 冒烟联调
