# AI 问数 BI（功能 12）— PRD（第一版）

> 关联：[AI模块-落地清单.md](./AI模块-落地清单.md)（功能 12 问数 BI：大白话查数，Text-to-SQL）
> 状态：已拍板，进入实现
> 范围：**MVP = 财务域大白话查数**（客户 / 合同 / 回款三张表，语义层安全编译 SQL）。任意 SQL 直查、订单/跟进/线索数据域、AI 数据解读、历史查询列为 Phase 2。

---

## 一、背景与目标

**现状问题**：老板/管理层要看「上月各客户回款多少」「本月签了几个合同、总额多少」，现在得自己翻列表或导出 Excel 手动算，或者找管理员写 SQL。不会写代码的人完全够不着数据。

**目标**：租户在 AI 中心输入一句大白话（如「上月各客户回款金额 Top10」），系统自动：

1. **理解意图** — LLM 把大白话解析成「指标 + 维度 + 时间范围 + 排序 + TopN」的结构化查询意图；
2. **编译 SQL** — Java 端用白名单「指标/维度目录」把意图编译成**参数化、强制注入 `organization_id`** 的安全只读 SQL；
3. **执行** — 只读连接查库，返回结果集（行上限 500）；
4. **可视化** — 前端渲染表格 + 图表（柱/折/饼）；SQL 仅作为后端内部实现，不回显给用户（2026-10-08 拍板：不向使用者暴露 SQL）。

**定位**（roadmap 功能 12）：独立型（进「AI 中心」）、生成型（每次真调 LLM，挂 G2 额度 + G3 权限）、低 token 成本、企业版。

**端到端主场景**：企业版租户进「AI 中心 → 问数 BI」→ 输入「上月各客户回款金额 Top10」→ 点查询 → 返回「客户名 + 回款金额」降序前 10 的表格 + 柱状图。

---

## 二、核心设计（关键决策，已拍板）

### 决策 1：MVP 用「语义层」安全方案，不做任意 SQL 直查（vanna 原教旨后置）

- LLM **只负责意图解析**：把大白话 → 结构化 JSON（指标 + 维度 + 时间 + 过滤 + 排序 + TopN + 图表类型 + 标题）。
- Java 端用**白名单「指标/维度目录」**把意图编译成 SQL：所有表名/列名/函数来自目录硬编码，**只有值走 `?` 参数绑定**，并**无条件注入 `organization_id = ?`**。
- **理由**：SQL 注入、跨租户越权、删库改库三大风险在语义层被结构性消灭；任意 SQL 直查收益低（用户要的是「看数」不是「写 SQL」）、风险高。真正的 Text-to-SQL 直查（配只读 DB 账号 + 严格校验）放 Phase 2，且仅当 MVP 语义层覆盖不了高频问题时再评估。

### 决策 2：数据域 MVP = 客户 + 合同 + 回款，订单/跟进/线索后置

- roadmap 明确「财务/应收模块已建，正好接回款/合同数据」，所以 MVP 直接吃这三张已建好的表，零新数据底座。
- 订单、跟进、线索、产品等数据域，作为「指标/维度目录」的增量扩展项，后续按需加目录条目即可（编译层设计成表驱动，加指标不碰框架）。

### 决策 3：feature_code = `ai_ask_bi`，企业版，低 token

- roadmap 标注企业版、token 成本低。计费走**生成型**：每次查询 `checkQuota` → 执行 → `record`（同会话军师），LLM 只做**一次**意图解析调用。
- **MVP 不做独立「AI 数据解读」**（拿到结果后二次 LLM 总结），省一次调用、token 最低；结论由用户直接看表格/图表。Phase 2 再加「AI 解读」开关。

### 决策 4：SQL 执行走注入 `DataSource` 的 PreparedStatement，不引 MyBatis 动态拼 SQL / JdbcTemplate

- 项目无 JdbcTemplate、MyBatis 也不适合「任意 GROUP BY / 时间分桶 / LIMIT」的动态编译。直接注入 Spring Boot 自动配置的 `DataSource`，取只读连接 + `PreparedStatement` + `?` 绑定，`ResultSet` 映射成 `List<Map<String,Object>>`。
- 时间字段全库是 `BIGINT` 毫秒，编译时统一 `FROM_UNIXTIME(col / 1000, fmt)` 做日/周/月分桶。

### 决策 5：LLM 走对话模型（`resolveChatModels`），同步非流式

- 意图解析是「语义理解」，非「洞察/评估」，走 chat 路由（同获客内容/会话军师的消息输入），不走 `task_models`。
- 用**同步 `provider.chat(...)`** 一次拿完整 JSON，不流式（流式会切碎 JSON 难解析）。

### 决策 6：图表前端用轻量 SVG 组件，不引入 echarts

- 项目无 echarts。MVP 柱/折/饼用纯 SVG 画（复用视觉特效库的分组规范），零新依赖；数据大屏（功能 14）上线时再评估是否上 echarts 并复用同一数据接口。

---

## 三、数据模型（改动）

### 3.1 无新表

MVP 查询**无状态、即时返回**，不落库、不做历史记录，因此**不新增任何业务表**。历史查询（记录问题、可重跑、可收藏）放 Phase 2，届时再建 `ai_bi_query` 表。

### 3.2 feature_code 注册（DML）

新增迁移 `V1.XX.0_X__ai_ask_bi_feature.sql`，向 `sys_feature` 注册 `ai_ask_bi`（名称「AI 问数 BI」），版本归属 **ENTERPRISE**（对齐 roadmap 企业版）。代码补常量 `AiQuotaConstant.AI_ASK_BI = "ai_ask_bi"`。

### 3.3 指标/维度目录（代码常量，表驱动，非 SQL）

这是本功能的「数据底座」，全部硬编码在 Java 常量类 `BiCatalog` 中，LLM 只产出目录内的 key。

**指标目录（MVP 6 个）**

| key | 名称 | SQL 表达式 | 说明 |
|---|---|---|---|
| `contract_amount` | 合同金额 | `SUM(contract.amount)` | |
| `contract_count` | 合同数 | `COUNT(contract.id)` | |
| `contract_avg_amount` | 平均合同额 | `AVG(contract.amount)` | |
| `payment_amount` | 回款金额 | `SUM(cpr.record_amount)` | 全部回款记录 |
| `payment_verified_amount` | 已核销回款金额 | `SUM(cpr.record_amount)` | 附加 `cpr.verification_status='DONE'` |
| `customer_count` | 客户数 | `COUNT(customer.id)` | |

**维度目录（MVP）**

| key | 名称 | SQL 表达式 |
|---|---|---|
| `customer` | 客户 | `GROUP BY customer.id, customer.name`，取值 `customer.name` |
| `owner` | 负责人 | `GROUP BY owner`（合同/客户域自适应） |
| `time_day` / `time_week` / `time_month` | 时间（日/周/月） | `FROM_UNIXTIME(基准字段/1000, fmt)` |
| `contract_stage` | 合同阶段 | `contract.stage` |
| `contract_approval` | 审批状态 | `contract.approval_status` |
| `payment_verify_status` | 回款核销状态 | `cpr.verification_status` |

**时间基准字段**：合同签约时间 `contract.start_time`、回款时间 `cpr.record_end_time`（回退 `cpr.create_time`）、客户创建时间 `customer.create_time`、客户采集时间 `customer.collection_time`。

### 3.4 LLM 输出契约（查询意图 JSON）

```json
{
  "metric": "payment_amount",
  "dimension": "customer",
  "timeRange": { "field": "payment_time", "from": "2026-09-01", "to": "2026-09-30" },
  "filters": [{ "field": "payment_verify_status", "op": "=", "value": "DONE" }],
  "order": "DESC",
  "limit": 10,
  "chartType": "bar",
  "title": "上月各客户回款金额 Top10"
}
```

- `metric`/`dimension`/`timeRange.field`/`filters[].field` 必须命中目录 key，否则 LLM 重试或返回「该问题暂不支持」的可读提示。
- 无维度时返回单值（`limit` 忽略，结果 1 行）。

---

## 四、接口清单

### 4.1 Java 侧 REST（前端调用，前缀 `/agent/bi`）

| 接口 | 说明 |
|---|---|
| `GET /agent/bi/available` | 是否开通（G3：`editionService.hasFeature(orgId, AiQuotaConstant.AI_ASK_BI)`） |
| `GET /agent/bi/catalog` | 返回指标/维度/示例问题目录，供前端输入框下方「示例问题」chips 与「可查范围」引导 |
| `POST /agent/bi/query` | 提交大白话问题 → 同步返回 `{ title, columns[], rows[], chartType }`（不回显 SQL） |

查询请求/响应要点：

```
POST /agent/bi/query
{ "question": "上月各客户回款金额 Top10" }
→ checkQuota(orgId, userId) 校验额度
→ resolveChatModels → provider.chat → 解析成「查询意图 JSON」
→ BiCatalog 编译成参数化 SQL（注入 organization_id）
→ DataSource 只读连接执行（LIMIT ≤ 500，超时 10s）
→ record(orgId, AI_ASK_BI, modelCode, usage)
→ { "title": "上月各客户回款金额 Top10", "chartType": "bar",
    "columns": ["客户", "回款金额"],
    "rows": [["张三", 128000.0], ...] }
```

### 4.2 复用（既有能力）

- G3 门控：`EditionService.hasFeature`（同 `LeadCrawlController` 用法）。
- G2 计量：查询前 `AiQuotaService.checkQuota`、查询后 `AiQuotaService.record`（同会话军师）。
- G1 模型路由：意图解析走 `resolveChatModels`（对话模型）。
- LLM 调用：`llmProviderFactory.get(provider).chat(...)`（同步，复用既有 `LlmChatRequest`/`LlmUsage`）。
- 只读执行：注入 `DataSource`，`getConnection()` 后设只读 + 超时，`PreparedStatement` 参数绑定。

---

## 五、前端

- 「AI 中心」新增「问数 BI」入口（企业版），入口可见性由 `/available` 门控；未开通渲染「升级解锁」空态，不触发全局 403 跳转。
- 新增页面 `views/askBi/index.vue`，结构：
  1. **搜索区**：大白话输入框（回车提交）+「查询」按钮用 `AiActionButton`（大号）；下方「示例问题」chips（来自 `/catalog`，如「上月各客户回款金额 Top10」「本月新签合同额」「近 6 个月回款趋势」）；生成中思考提示用 `ShimmerText` 流光扫字。
  2. **结果区**：标题 + 图表（柱/折/饼，SVG）+ 数据表格（Naive UI `n-data-table`）；不展示 SQL。
- i18n：新增 key 同步维护 `locale/zh-CN.ts` 与 `en-US.ts`。
- 图表组件：`components/pure/effects/` 下新增轻量 SVG 柱/折/饼（分组 + 数字编号，符合视觉特效库规范），不引 echarts。

---

## 六、权限与合规

1. **多租户隔离（硬约束）**：编译层无条件注入 `organization_id = ?`，LLM 意图不含租户 id；任何表/列标识符不来自用户输入。
2. **只读（硬约束）**：只生成 `SELECT`，连接设只读，无任何写路径。
3. **行级权限**：MVP 按 `organization_id` 全租户数据（聚合报表默认全租户可见，与数据大屏一致）；「负责人」维度供用户自行下钻过滤。owner 级行级权限（上级看下级）在数据大屏一起做，MVP 不展开。
4. **结果防护**：`LIMIT` 上限 500、查询超时 10s，防大结果拖库。
5. **额度与合规**：每次查询走 G2 计量；喂 LLM 的只有「大白话问题」文本，不含数据库数据（数据在 Java 侧编译执行，不进模型上下文）。

---

## 七、验收要点（端到端）

- 企业版租户进「AI 中心 → 问数 BI」：`/available=true`，页面可打开；基础版看到「升级解锁」空态且接口不抛 403。
- 输入「上月各客户回款金额 Top10」→ 返回表格（客户 + 回款金额降序前 10）+ 柱状图（不展示 SQL，后端仍强制 `organization_id` 过滤）。
- 输入「本月新签合同额」→ 返回单值（本月合同金额），无维度不报错。
- 输入「; DROP TABLE x」或「删掉所有客户」等恶意/写操作 → 系统只做意图解析，命中不了目录则返回「该问题暂不支持，请尝试：…」的可读提示，**不执行任何非 SELECT**。
- 结果行数超 500 自动截断且前端有提示。
- 每次查询扣额度：租户「次数」看板 + admin「成本」看板可见消耗。
- 数据实时：改一条回款后重新查询，金额随之变化（直查 MySQL，非缓存）。

---

## 八、明确不在本 PRD 范围

- **任意 SQL 直查**（vanna 原教旨、连 MySQL 手写 SQL）——Phase 2，仅当语义层覆盖不了高频问题再评估，配只读 DB 账号 + 严格校验。
- **订单 / 跟进 / 线索 / 产品数据域**——Phase 2，作为指标/维度目录的增量扩展。
- **AI 数据解读**（拿到结果后二次 LLM 生成自然语言结论）——Phase 2，省 token 后置。
- **历史查询 / 收藏 / 定时报表**——Phase 2，需建 `ai_bi_query` 表。
- **导出 CSV / 图片分享**——Phase 2。
- **数据大屏 AI 数字人问答复用**（roadmap 提到的「复用」）——数据大屏（功能 14）上线时接同一套 Text-to-SQL 能力，MVP 不反向依赖大屏。
