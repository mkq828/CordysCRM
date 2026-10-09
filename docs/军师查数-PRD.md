# 军师查数（会话军师接入数据库查询）PRD

## 背景与目标

会话军师（featureCode `ai_advisor`）原本只做「对话分析」：粘贴聊天记录或上传截图，AI 分析客户意向、异议与应对话术，不查数据库。

但销售同样需要「用大白话查客户数据」——例如「张三最近跟进到哪了」「我名下有哪些客户 7 天没跟进了」。所以给军师接入数据库查询，让它既能分析对话、也能查数。

与「AI 问数 BI」的分工：

- **问数 BI**：报表/看板型，全局聚合、只读、语义层 Text-to-SQL，面向「看整体数据」。
- **军师查数**：顾问型，查「本人 + 下级名下」的客户与跟进明细，自然语言回答，面向「单个销售查自己盘子里的数」。

## 数据范围（第一版）

| 数据域 | 字段 |
| --- | --- |
| 客户本体 | 客户名称、负责人、最近跟进时间、最近跟进人、是否在公海 |
| 跟进记录 | 客户名称、跟进时间、跟进方式、跟进内容、跟进人 |

- **金额类（合同/订单/回款）暂不开放**，第二版再议。
- 行级权限固定为「本人 + 递归下级」，不提供越级或跨部门视角。

## 后端方案（语义层白名单，复用问数 BI 安全思路）

`cn.cordys.crm.ai.advisor` 包：

- `AdvisorDataCatalog`：硬编码 3 个查询动作 + 参数白名单（LLM 只产出目录内 action 与参数，不产 SQL）：
  - `listCustomers`（客户列表，参数 name/sharedPool/staleDays）
  - `customerFollows`（某客户跟进，参数 name/withinDays）
  - `recentFollows`（最近跟进，参数 withinDays）
  - `none`（非查数问题 → 返回友好提示，`queried=false`）
- `AdvisorDataService`：编排流程
  1. 意图解析（一次 LLM，走 chatModels）
  2. 编译只读 SQL —— 无条件注入 `organization_id = ?` 与 `owner IN (本人+下级)`，值全部参数化
  3. 执行（`Connection.setReadOnly(true)` + 超时 + 上限 100 行）
  4. 姓名/时间/公海可读化（`BaseService.getUserNameMap`）
  5. 生成自然语言回答（二次 LLM）
- 行级权限：`DataScopeService.getUserIdsWithSubordinates(userId, orgId)`（本人 + 递归下级）。
- 落会话：与 analyze 一致，写 `agent_conversation` / `agent_message`，payload 存 `{columns, rows}`，刷新后历史可回读并渲染「数据来源」表。
- 额度：复用 `ai_advisor` featureCode，模型走 `resolveChatModels`（对话/通用）。

接口：`POST /agent/advisor/query`，请求 `{question, conversationId?}`，响应 `{answer, queried, columns, rows, conversationId}`。

## 前端方案

`salesAdvisor.vue`（会话军师组件）：

- 输入框上方加轻量切换：「分析对话」（默认，原能力不变）/「查客户数据」。
- 「查客户数据」模式：隐藏截图上传与客户关联下拉；placeholder 给示例问句；按钮文案变「查询」；提交走 `/agent/advisor/query`。
- 结果渲染：自然语言回答 + 可折叠「数据来源（N 条）」表格（n-data-table）。
- 与「分析对话」共用同一个左侧历史会话列表（featureCode 相同）。

## 关键决策

- **用「输入类型切换」而非「全自动识别」**：自动识别要么靠前端启发式规则（不可靠），要么靠一次额外 LLM 往返判断意图（token 成本 + 延迟）。切换方式零额外成本、意图明确、不破坏现有「分析对话」能力。
- **同一个对话框 + 同一个历史会话**，是对军师输入能力的增强，不是另起菜单/功能。

## 边界（本次不做）

- 金额类（合同/订单/回款）查询。
- 跨部门 / 越级视角（权限固定本人 + 下级）。
- 前端全自动意图识别（如后续确需，可加后端统一意图分流，但需接受一次额外 LLM 往返）。

## 验证

1. 后端 `./mvnw -f backend/pom.xml clean package -pl app -am -DskipTests`，重启后 `/agent/advisor/query` 返回 `queried=true` + answer + 数据来源。
2. 前端 5173：切到「查客户数据」，问「我名下有哪些客户 7 天没跟进了」，出自然语言回答 + 可折叠数据表；左侧历史出现该会话，刷新可回读。
