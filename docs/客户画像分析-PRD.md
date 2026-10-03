# 客户画像分析（360° 画像）— PRD（第一版）

> 关联：[AI模块-落地清单.md](./AI模块-落地清单.md)（§功能13 嵌入型客户画像）
> 状态：待评审，评审通过后再动手
> 范围：客户详情页新增「客户画像」Tab（360° 只读聚合 + AI 洞察回读）。线索/商机画像、数据大屏聚合不在本版。

---

## 一、背景与目标

**现状问题**：销售打开客户详情，跟进、商机、订单、合同、回款分散在各 Tab，没有一个「一眼看全」的视角；会话军师分析过的结论也没有沉淀回客户身上，换个入口就丢了。

**目标**：客户详情页新增「客户画像」Tab，一眼看到：
1. **谁** — 客户基础信息 + 负责人 + 来源；
2. **值不值得跟** — 成交信号 / 意向评分（AI 洞察）；
3. **卡在哪** — 异议点 / 流失风险（AI 洞察）；
4. **下一步** — 候选跟进话术（AI 洞察）。

**定位**（roadmap §功能13）：嵌入型（不占菜单，长在客户详情页）、查看型（只读、订阅内不限次、只挂 G3 权限开关、不挂 G2 额度计量）、版本归属专业版+。

---

## 二、核心设计（关键决策，已拍板）

### 决策 1：画像 = 静态聚合 + AI 洞察，两者分开

- **静态聚合**：确定性，从 DB 现算（基础信息、跟进、商机、订单、合同、回款），零 token，任何版本/任何时刻都能看。
- **AI 洞察**：只读 `ai_analysis_result` 表里该客户最新一条分析结论（意向/信号/异议/情绪/竞品/流失/话术），由会话军师沉淀。

**理由**：查看型「不限次、不计量」的前提是不调模型——所以画像自己不能现生成，只能回读已有结论。

### 决策 2：AI 洞察的产生走「会话军师」，画像不单独加「生成」按钮

- 给会话军师补一个「关联客户」输入（`customerId`，可空）；分析完成即把结构化结论落 `ai_analysis_result`（`biz_type=customer`）。
- 画像 AI 洞察空态给「去会话军师分析」引导（跳智能工作台并预带该客户）。
- **理由**：对齐 roadmap 数据链「会话军师(单次对话) → ai_analysis_result → 客户画像(单客户360°)」；会话军师已是生成型（挂 G2 额度），画像保持查看型（只挂 G3），职责清晰、不重复计费。

### 决策 3：第一版只做「客户」，线索/商机后续复用同一套卡片

**理由**：客户数据最全（跟进+商机+订单+合同+回款），是 §功能13 的核心验收点；线索/商机数据稀疏，且画像卡片按 `sourceId + bizType` 复用，随二期加。

### 决策 4：feature_code = `ai_customer_profile`，注册为专业版+（PRO + ENTERPRISE）

- 查看型只做 G3 权限开关，不进 `AiQuotaService` 额度计量（不调模型、不计费）。
- **理由**：roadmap §1.2 查看型定义。

### 决策 5：`ai_analysis_result` 表按项目既有约定建

- 用 `organization_id`（不是 roadmap 草案里的 tenant_id）、`id VARCHAR(32)` 雪花串、`create_time BIGINT` 毫秒——与 `ai_usage_record` 一致。
- **理由**：与项目现有 AI 表/领域实体（`Customer extends BaseModel`，`organizationId`）保持一致。

### 决策 6：G3 权限校验用「版本→功能」实时映射，不用历史快照

- 新增 `EditionService.hasFeature(orgId, featureCode)`：取 `tenant_edition` 快照；**试用租户（无生效快照）视为放开**；已开通租户按 `listFeatureCodesByEditionCode(editionCode)` 实时判断。
- **理由**：`features_json` 是开通时快照，新增 feature 后老租户快照是旧的；实时映射能让「加一个功能，该版本所有租户自动可用」，不用逼每个租户重新开通。

---

## 三、数据模型（改动）

### 3.1 新表 `ai_analysis_result`（唯一新增表）

新版本迁移 `migration/1.30.0/ddl/V1.30.0_1__ai_analysis_result.sql`（不改已发布脚本）：

```sql
SET SESSION innodb_lock_wait_timeout = 7200;

-- AI 分析结果沉淀：会话军师等生成型能力把结构化结论按业务对象落此表，客户画像/数据大屏只读回读
CREATE TABLE ai_analysis_result
(
    id              VARCHAR(32) NOT NULL COMMENT '主键',
    organization_id VARCHAR(50) NOT NULL COMMENT '租户组织ID',
    biz_type        VARCHAR(32) NOT NULL COMMENT '业务对象类型: customer/clue/opportunity',
    biz_id          VARCHAR(32) NOT NULL COMMENT '业务对象ID(客户/线索/商机主键)',
    feature_code    VARCHAR(32) NOT NULL COMMENT '来源能力: ai_advisor=会话军师',
    title           VARCHAR(128) COMMENT '标题(如「会话军师分析」)',
    result_json     TEXT        COMMENT '结构化结论JSON(会话军师结构化字段)',
    model_code      VARCHAR(64) COMMENT '生成所用模型',
    create_time     BIGINT      COMMENT '创建时间(毫秒)',
    update_time     BIGINT      COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32) COMMENT '创建人',
    update_user     VARCHAR(32) COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_biz (organization_id, biz_type, biz_id, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
```

- 同一 `(organization_id, biz_type, biz_id)` 允许多条（历史沉淀），画像取**最新一条**。
- `result_json` 存会话军师 `SalesAdvisorAnalyzeResponse` 序列化结果，字段与现有 `agent_message.payload` 一致，前端复用同一渲染结构。

### 3.2 feature_code 注册（DML）

新版本迁移 `migration/1.30.0/dml/V1.30.0_2__ai_customer_profile_feature.sql`：

- `sys_feature` 插入 `ai_customer_profile`（名称「客户画像分析」，类型查看型/订阅内权限）。
- `sys_edition_feature` 把该 feature 挂到 PRO 与 ENTERPRISE 两个版本（专业版+）。
- 参照既有 `1.16.0/dml/V1.16.0_2__edition_data.sql` 的 id/字段写法。

---

## 四、接口清单

### 4.1 新增：客户画像聚合（只读）

| 接口 | 说明 |
|---|---|
| `GET /customer/{id}/profile` | 客户画像聚合，一次返回静态 360° + 最新 AI 洞察 + `featureAvailable` |

请求：`{id}` 客户主键（路径参数）。

响应 `CustomerProfileResponse`：

```
{
  "available": true,                // 是否专业版+（feature 可用）；false 时前端渲染「升级开通」空态
  "customer": { name, owner, ... }, // 基础信息
  "follow": { totalCount, latestContent, latestTime, nextPlanTime, nextPlanContent },
  "opportunity": { totalCount, totalAmount, stages: [{stage, count}] },
  "order": { totalCount, totalAmount },
  "contract": { totalCount, totalAmount },
  "payment": { paidAmount, paymentRate },   // 已回款总额 / 合同总额
  "aiInsight": { ... } | null        // 最新一条 ai_analysis_result.result_json 反序列化；无则 null
}
```

### 4.2 改动：会话军师请求加 `customerId` + 分析结果回写

- `SalesAdvisorAnalyzeRequest` 加 `customerId`（可空，String）。
- `SalesAdvisorService.doAnalyze` 在 `parseAnalysis` + `enrichWithScriptLibrary` 之后、返回前：若 `customerId` 非空，写一条 `ai_analysis_result`（`biz_type=customer`、`biz_id=customerId`、`feature_code=ai_advisor`、`result_json=JSON(analysis)`、`model_code=model.getModelName()`）。同步 `/analyze` 与流式 `/analyze/stream` 共用 `doAnalyze`，天然都回写。
- 回写失败仅告警，不阻断会话军师主流程（画像可空态兜底）。

### 4.3 复用（行级权限校验）

- 画像接口进入前用 `ResourcePermissionService.checkResourcePermission`（或 `CustomerService.getWithDataPermissionCheck` 模式）校验当前用户对该客户有数据权限；无权限按现有约定抛错。

---

## 五、前端

- 客户详情 `customerOverviewDrawer.vue`：
  - `tabList` 增加 `{ name: 'profile', tab: t('customer.profile'), enable: true, permission: ['CUSTOMER_MANAGEMENT:READ'] }`（放在 `followRecord` 之前，作为默认视角之外的显眼位置；或置于 `contact` 后）。
  - `<template #right>` 增加 `<CrmCard v-else-if="activeTab === 'profile'" ...><CustomerProfilePanel :source-id="props.sourceId" /></CrmCard>` 分支。
- 新增业务组件 `views/customer/components/customerProfilePanel.vue`：
  - 顶部「AI 洞察」卡片：`aiInsight` 有值 → 渲染意向评分(数字/进度)、成交信号、异议点、流失风险、候选话术；空态 → 橙色说明「暂无 AI 洞察，去会话军师分析该客户」+ 按钮跳转智能工作台（带 `customerId` query）。
  - 下方「静态 360°」区块：跟进 / 商机 / 订单 / 合同 / 回款 汇总卡片（复用现有 `crm-*` 展示组件与设计变量）。
  - `available=false` → 整卡渲染「当前版本未开通客户画像，升级专业版解锁」空态。
- i18n：新增 key 同步维护 `locale/zh-CN.ts` 与 `en-US.ts`。
- 会话军师 `salesAdvisor.vue`：输入区增加可选「关联客户」选择（复用现有客户选择组件），透传 `customerId`。

---

## 六、权限与合规

1. **行级权限**：只允许对该客户有数据权限（本人/下属/协作）的人查看画像，复用现有 `DataScopeService`/`ResourcePermissionService`。
2. **只读**：画像接口无任何写操作；会话军师回写只新增 `ai_analysis_result`，不改客户本体。
3. **免责声明**：AI 洞察卡片固定橙色说明「AI 生成仅供参考，关键判断以人工为准」。
4. **数据合规**：画像只读本租户内该客户已有数据，不引入新的模型调用、不外发数据（回写时 `result_json` 为已脱敏结构化字段，不含手机号/身份证原文）。

---

## 七、验收要点

- 打开客户详情 → 切「客户画像」Tab：静态 360°（跟进次数/最近跟进、商机数与金额、订单/合同/回款汇总）一次可见。
- 若该客户已被会话军师分析过（带 customerId），AI 洞察卡回读展示意向评分/信号/异议/流失/候选话术，不重新调模型。
- 若未分析过：空态 + 「去会话军师分析」引导，点击跳智能工作台并预带该客户。
- 基础版租户（无 `ai_customer_profile` 权限）：显示「升级开通」空态，接口 `available=false`，不触发全局 403 跳转。
- 无该客户数据权限的人：接口按行级权限拒绝；不会看到他人客户画像。
- 会话军师分析并关联客户后，`ai_analysis_result` 正确落一条；再次打开画像即回读到最新一条。

---

## 八、明确不在本 PRD 范围

- 线索 / 商机详情页画像（二期，复用同一 `CustomerProfilePanel`）。
- 数据大屏聚合 `ai_analysis_result`（功能14，后续）。
- 画像内「生成/刷新画像」按钮（不引入，保持查看型不限次）。
- `ai_analysis_result` 的历史列表 / 手动清理（后续按需）。
