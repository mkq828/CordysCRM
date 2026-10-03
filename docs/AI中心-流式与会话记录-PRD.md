# AI 中心：流式统一 + 会话记录 — PRD（第一版）

> 关联：[AI模块-落地清单.md](./AI模块-落地清单.md)（§6.2 三类分流：独立型 → 「AI 中心」一级菜单）
> 状态：待评审，评审通过后再动手
> 范围：只做「AI 中心入口 + 三个能力统一流式 + 左侧会话记录」。文档在线预览（#1）、引用 rerank（#3）不在此范围，单独立项。

---

## 一、背景与目标

**现状问题**：会话军师（`/agent/advisor/analyze`）、智能问答（`/agent/kb/ask`）、获客内容（`/agent/content/generate`）三个接口都是「**把模型流缓冲成整段 JSON 才返回**」，前端要等模型全部说完才看到结果，体感很慢。而哆咪 AI 对话（`/agent/chat/stream`）已经是 SSE 流式，所以「哆咪 AI 快、这三个慢」的差距不是模型（模型已是豆包），而是**流式通道没打通**。

**目标**：
1. 三个能力**统一改成 SSE 流式**，复用哆咪 AI 的流式底座，达到同样的「首字即出」体感。
2. 三个能力**加上左侧会话记录**（豆包式布局：左会话列表 + 右侧对话 + 底部输入），备注提问人 + 提问时间。
3. 按 roadmap §6.2 把这几个**独立型能力收拢进「AI 中心」一级入口**，而不是三个菜单各挂一个左栏。

---

## 二、现状盘点（可复用，不重搭）

| 能力 | 已有底座 | 位置 |
|---|---|---|
| 流式通道 | `AgentChatController` `/agent/chat/stream`，SSE 事件 `run/chunk/done/error`；`LlmProvider.chatStream` 已真流式（`stream:true`） | `backend/crm/.../ai/controller/AgentChatController.java` |
| 会话表 | `agent_conversation`（id/title/user_id/organization_id/时间/操作人）、`agent_message`（role/run_id/conversation_id/token/content/helpful/status/时间/操作人） | `migration/1.9.0/.../V1.9.0_2__ga_ddl.sql` |
| 会话 CRUD 接口 | `/agent-conversation/page`、`/get/{id}`、`/delete/{id}`、`/rename/{id}`、`/agent/chat/cancel`、`/agent/chat/file/upload` | `frontend/packages/lib-shared/api/requrls/ai.ts` |
| 前端会话布局 | `ai-chat` 组件（左 25% 会话列表：搜索/新建/重命名/删除/远程分页 + 右侧消息流 + 底部输入；block：思考/文本/进度/Markdown/错误/加载/附件/确认弹窗） | `frontend/packages/web/src/components/business/ai-chat/` |
| 前端 SSE 客户端 | `AgentChatStreamEvent`（`run/progress/chunk/confirm/error/done`）+ `getAgentConversationPage` 等 | `frontend/packages/lib-shared/models/ai.ts`、`api/modules/ai.ts` |

**结论**：本 PRD 的核心是「把三个能力接进这套现成底座」，不是新画 UI、不是新建会话体系。

---

## 三、数据模型（改动）

复用 `agent_conversation` + `agent_message`，两处增量（新版本迁移脚本，不改已发布脚本）：

### 3.1 `agent_conversation` 加 `feature_code`

```sql
ALTER TABLE agent_conversation
    ADD COLUMN feature_code VARCHAR(32) NOT NULL DEFAULT 'chat'
        COMMENT '能力类型：chat=哆咪AI对话, ai_advisor=会话军师, ai_kb=智能问答, ai_acquire=获客内容' AFTER title;
CREATE INDEX idx_feature_code_user ON agent_conversation (feature_code, user_id);
```

- 取值沿用 `AiQuotaConstant` 既有常量（`AI_CHAT` / `AI_ADVISOR` / `AI_KB` / `AI_ACQUIRE`），实现时以常量为准，不硬编码字符串。
- 左侧会话列表按 `feature_code` 分域过滤；切换能力 tab 即切换一套历史。

### 3.2 `agent_message` 加 `payload`

```sql
ALTER TABLE agent_message
    ADD COLUMN payload TEXT NULL COMMENT '能力结构化结果 JSON（如出处引用/分析字段/物料包/截图附件），纯文本对话为 NULL' AFTER content;
```

- `content` 保持为**流式文本/markdown**（列表预览、历史回放、复制都用它）。
- `payload` 存能力特定的结构化结果，前端据此重建右侧富卡片；`NULL` 时按纯 markdown 渲染（向后兼容哆咪 AI 对话）。
- USER 消息：`content` = 输入摘要（如「行业：X，产品：Y」或粘贴文本），`payload` = 原始参数（含 `picIds` 截图附件 id）。

### 3.3 会话标题

- 新建会话时 `title` 取「首条 USER 消息前 24 字」；沿用现有 `/agent-conversation/rename` 支持手动改名。

---

## 四、流式协议（统一 SSE，扩展哆咪 AI 协议）

沿用 `AgentChatController` 的 `text/event-stream` 写法（`event:` + `data:` 逐行），事件类型在现有 `run/chunk/done/error` 基础上**新增 `step`**（多步能力进度）。前端复用 `ai-chat` 的 SSE 客户端，事件映射到现有 block。

| 事件 | 数据 | 前端映射 |
|---|---|---|
| `run` | `{conversationId, runId, userMessageId, assistantMessageId, featureCode}` | 建会话/消息上下文 |
| `step` | `{stage, title, status}` | `AiProgressBlock`（获客内容分步） |
| `chunk` | 纯文本增量（多行 markdown 逐行） | `AiTextBlock`/`AiMarkdownBlock` 打字机 |
| `done` | `{conversationId, runId, assistantMessageId, totalTokens, input, output, payload}` | 收尾 + 按 `payload` 渲染富卡片 |
| `error` | `{message}` | `AiErrorBlock` |
| `cancel` | 复用 `/agent/chat/cancel` | 中断本轮 |

`payload` 是能力结构化结果 JSON（见 §六各能力的右栏形态）。流式中途失败/取消，`agent_message.status` 落 `stopped`，正常结束落 `done`（复用现有字段）。

---

## 五、接口清单

### 5.1 新增（SSE 流式，三个）

| 接口 | 说明 |
|---|---|
| `POST /agent/center/advisor/stream` | 会话军师流式分析 |
| `POST /agent/center/kb/stream` | 智能问答流式回答 |
| `POST /agent/center/content/stream` | 获客内容流式生成（多步） |

三者统一走「额度校验 → 租户模型解析（`AgentModelService.resolveChatModels`）→ 流式调用 → 记账」，照抄 `AgentChatService.chat` 的骨架，只替换各自的 prompt/解析/记账 feature_code。

### 5.2 复用（会话 CRUD，加 `featureCode` 过滤）

- `POST /agent-conversation/page`（请求加 `featureCode`，返回该能力下当前用户的会话列表，含 title/createUser/createTime）
- `GET /agent-conversation/get/{id}`（会话 + 消息列表，消息带 `payload`）
- `GET /agent-conversation/delete/{id}`
- `POST /agent-conversation/rename/{id}`
- `POST /agent/chat/cancel`（流式中断）
- `POST /agent/chat/file/upload`（军师截图上传，复用现有临时附件链路）

### 5.3 保留不动（非流式 / 写动作）

- `POST /agent/advisor/analyze`（保留，供「一键转跟进」落库动作继续用；或拆出独立 `POST /agent/advisor/follow-up`）
- `POST /agent/kb/doc/page`、`/upload`、`/delete/{id}`（知识库文档维护，与问答解耦，不变）

---

## 六、三个能力的右栏形态

三个能力右侧内容区不同，但都落在 `ai-chat` 的 block 体系内；历史回放按 `payload` 重建同一套卡片（不重新调模型）。

### 6.1 会话军师（`ai_advisor`）

- **输入区**：多行粘贴文本 + 截图上传（复用 `AiComposer` + `AiAttachmentList`）。
- **输出**：
  - 流式 chunk：一段「分析结论叙述」markdown（实时打字机）。
  - `done.payload`：结构化分析卡 = `{intentScore, signals[], objections[], emotion, competitorMentions[], churnRisk, suggestedScripts[], scriptRecommendations[]}`。
  - 卡片底部「一键转跟进」按钮（写动作，点击调 §5.3 的落库接口，不走流式）。
- **流式策略**：prompt 改「两段式」——先输出叙述（chunk 给前端），再输出结构化 JSON（服务端解析后进 payload）。若不改 prompt，则退化为「loading 块 + 结束时渲染结构化卡」。

### 6.2 智能问答（`ai_kb`）

- **输入区**：单行/多行问题（复用 `AiComposer`，去掉附件）。
- **输出**：
  - 流式 chunk：`answer` markdown 正文（实时打字机）。
  - `done.payload`：`{citations: [{docName, snippet}]}`，渲染「出处引用」卡（文档名 + 原文片段，可复制）。
- **流式策略 + 顺带修引用过粗**：模型只输出 `answer` 纯文本（流式）；**出处改为服务端确定性回填**——用提问关键词从召回块中就近截取相关 1–2 句作为 `snippet`，不再让模型返回 500 字大块。这同时解决「引用只有一句相关却带出整段」的问题（即你提的 #3 片段化），无需 rerank。

### 6.3 获客内容（`ai_acquire`）

- **输入区**：行业 / 产品卖点 / 平台（抖音·小红书·朋友圈）/ 条数（小表单，复用 `AiComposer` 的头部或独立轻表单）。
- **输出**（多步，用 `step` + `AiProgressBlock`）：
  - `step 选题` → chunk 选题文本 → `step 文案` → chunk 文案 → `step 配图/封面/标签/口播` → chunk。
  - `done.payload`：`contents[]`（`AiContentItem`：topic/title/copy/imageCopy/coverCopy/hashtags/bestTime/script）渲染为「可发布物料包」卡，每条可复制/导出。
- **流式策略**：每一步一个模型调用，边出边推 `step` + `chunk`，用户全程看到进度，不再「一坨等到底」。

---

## 七、导航与入口（AI 中心）

- 新增「AI 中心」一级菜单（机器人图标），内部**顶部能力 tabs**：会话军师 / 智能问答 / 获客内容 / 哆咪 AI 对话（现智能体并入）。
- 左侧会话列表按 `featureCode` 分域：切 tab 切一套历史；列表项展示「标题 + 提问人 + 相对时间」。
- 工作台 smart 页的「会话军师」卡片改为**跳转 AI 中心入口**（去重，避免同一能力两套 UI）。
- 企业知识库菜单保留「文档库/上传」入口（文档维护与问答解耦），问答入口并入 AI 中心。

---

## 八、分阶段实施建议

1. **阶段 A（流式）**：三个 SSE 接口 + 前端 `ai-chat` 接入，先把「慢」解决。各能力沿用现有菜单入口，暂不迁导航。
2. **阶段 B（会话记录）**：`feature_code`/`payload` 迁移 + 会话列表按能力分域 + 标题/提问人/时间展示。
3. **阶段 C（AI 中心）**：收拢一级入口 + 能力 tabs + 工作台去重。

> 阶段 A/B 可合并不必强行拆；阶段 C 依赖产品/导航评审。

---

## 九、验收要点

- 三个能力均**首字即出**，不再整段等待；豆包模型下首字延迟与哆咪 AI 对话相当。
- 左侧会话列表能看到历史（标题/提问人/时间），点击回放完整卡片（含出处/分析字段/物料包），不重新调模型。
- 切换能力 tab 各自独立一套历史，互不串扰。
- 智能问答出处只回填**相关片段**，不再带出无关整段。
- 额度/模型未配置/流式中断（cancel）等边界与现有行为一致；`agent_message.status` 正确落 `done/stopped`。

---

## 十、待拍板决策点

1. **会话军师**：整体迁入 AI 中心，还是工作台留快捷入口（双入口）？
2. **智能问答出处**：是否接受「服务端按关键词就近截取片段」替代「模型输出 citations」（我推荐：接受，顺带修 #3，且不用加 rerank）？
3. **会话表**：是否同意三个能力共用 `agent_conversation`/`agent_message`（加 `feature_code`/`payload`），而不是新建独立表（我推荐：共用，历史一致、复用现成 CRUD）？
4. **模型路由**：确认军师/问答/获客三个 `feature_code` 都已路由到豆包（快模型），不落到推理模型。

---

## 十一、明确不在本 PRD 范围

- 文档在线预览（需先补「存原文件」+ 附件转存，单独立项）。
- 引用 rerank / 真向量化检索（文档量上来后再做，见 roadmap §功能 4 来源）。
