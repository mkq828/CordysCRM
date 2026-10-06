# 短视频生成（功能 7）— PRD（第一版）

> 关联：[AI模块-落地清单.md](./AI模块-落地清单.md)（功能 7 短视频/数字人/爆款复刻）
> 状态：待评审，评审通过后再动手
> 范围：**MVP = 口播视频生成**（主题/文案 → 口播文案优化 → TTS 配音 → 字幕 → FFmpeg 合成 → 下载发布）。数字人、声音克隆（功能 11）、爆款复刻列为 Phase 2。

---

## 一、背景与目标

**现状问题**：客户做餐饮/本地生活引流，要持续发抖音/视频号口播视频拉新，但「写文案 → 找配音 → 剪视频 → 加字幕 → 发布」全靠人工，一天出不了几条，选题和文案也没数据支撑。

**目标**：租户输入一个主题或一段素材文案，系统自动产出可下载的口播视频 + 各平台配套文案：

1. **写脚本** — 按主题生成口播文案（分句、带语气，复用功能 3 的选题/文案能力）；
2. **配音** — 逐句合成语音（中文多音色可选）；
3. **加字幕** — 按配音时间轴自动生成字幕；
4. **合成** — FFmpeg 把「背景 + 语音 + 字幕」合成 mp4；
5. **下载发布** — 租户手动下载、手动发布（**不做自动发布**，防封号）。

**定位**（roadmap 功能 7）：独立型（进「AI 中心」）、生成型（每次真调 LLM + TTS + 视频合成，挂 G2 额度 + G3 权限）、高成本走增值包。

**端到端主场景**：租户进「AI 中心 → 短视频」→ 输入主题（如「小龙虾店 3 周年店庆」）选音色/风格 → 点生成 → 异步排队出片 → 列表里预览/下载 mp4 → 复制配套文案去抖音发布。

---

## 二、核心设计（关键决策，已拍板）

### 决策 1：MVP 只做「口播视频（文本成片）」，数字人 + 声音克隆 + 爆款复刻全部后置

- **MVP 范围**：文案 → TTS 配音 → 字幕 → FFmpeg 合成 → 下载。底座参考已下载的 MoneyPrinterTurbo（Apache，纯文本成片）。
- **Phase 2（后置）**：2D 数字人（真人录制 + AI 口型驱动，SadTalker/MuseTalk）、声音克隆（功能 11）、爆款复刻（输入爆款文案/视频 → LLM 仿写结构）。
- **理由**：数字人 = GPU 成本高 + 恐怖谷风险，声音克隆 = 能力组件（单独没用户价值，应跟着数字人/外呼做）。先把「能稳定出可下载视频」这条主线跑通，成本最低、无卡脖子依赖，再上高成本部分。

### 决策 2：视频生成走「异步任务 + 轮询」，不走 SSE 长连接

- 视频生成 30s ~ 数分钟，远超 SSE 能挂住的时长。沿用项目已有模式（智能通话复盘 / 私信画像）：`@Async` 提交 + 前端每 3s 轮询任务状态。
- **理由**：把「同步 HTTP 阻塞」消掉；异步 + 轮询是项目内成熟模式，不新造基础设施。

### 决策 3：TTS 用阿里云百炼 CosyVoice（复用现有 ASR 同一把 key），不绕 edge-tts

- **MVP 即生产方案**：阿里云百炼 CosyVoice 语音合成（DashScope 多模态 TTS），HTTP 同步调用、中文音色多。复用通话复盘 ASR（paraformer-v2）已接入的**同一把「阿里云」百炼 API Key**（`agentModelService.resolveProviderApiKey(orgId, "阿里云")`），**不新增 key、不被「等 key」卡脖子**。
- **代码抽 `TtsProvider` 接口** + `AliyunCosyVoiceTtsProvider` + `TtsProviderFactory`，与 ASR 的 `AsrProvider` / `AsrProviderFactory` 完全同构；未来接火山/其他 TTS 只换实现、不改调用方。
- **为什么不用 edge-tts**：edge-tts 是逆向微软 Edge 网页版语音的非官方接口，Java 侧要手写 WebSocket 协议、且有被微软改动的风险；而阿里云百炼 TTS 是官方接口、HTTP 更简单，且 key 已经躺在系统里（ASR 在用）。原 PRD 担心的「等 key」不成立，直接走官方路线。

### 决策 4：FFmpeg 是服务器部署依赖，Java 侧 ProcessBuilder 调用，不自研视频引擎

- 合成（背景 + 语音 + 字幕）用 FFmpeg，服务器安装二进制；Java 侧 `ProcessBuilder` 调用，复用现有异步线程池。
- **理由**：视频合成是 FFmpeg 的标准能力，不自研、不引重依赖（如 jave2 的版本包袱）。

### 决策 5：feature_code = `ai_video`，版本按 seed 现状 = 专业版+；数字人用 `digital_human`（企业版）

- `ai_video` 已在 `sys_feature` 注册且已挂 PRO + ENTERPRISE（seed `V1.16.0_2__edition_data.sql`）；MVP 直接复用，**无需新增 DML**，只补 `AiQuotaConstant.AI_VIDEO = "ai_video"` 常量。
- `digital_human` 已挂 ENTERPRISE，Phase 2 数字人用。
- **与 roadmap 的差异已对齐**：roadmap 把「功能 7」整体标「企业版」，但 seed 把「短视频生成」和「数字人/剪辑/爆款复刻」拆成了两个 feature code，前者专业版+、后者企业版。**我拍板按 seed 现状走**：短视频文本成片是「走量」功能、成本可控，打包进订阅价卖专业版更符合「专业版走量主力」的商业定位；数字人（GPU 高成本）才归企业版 + 增值包。

### 决策 6：计费 = 生成型，挂 G2 额度 + G3 权限；增值包加购 Phase 2 细化

- 每次生成计 1 次额度，走 `AiQuotaService`（同会话军师/竞品洞察计量入口），生成前校验、生成后记 `ai_usage_record`。
- **增值包加购**（TTS/视频合成的真实成本转嫁租户）留 Phase 2 计费细化；MVP 先挂「订阅内额度 + 企业版/专业版权限」，跑通闭环。

### 决策 7：数据模型单表 `ai_video_task`，视频文件走现有附件系统

- 一张任务表存「输入脚本 + 状态机 + 进度 + 输出附件 ID + 错误」，不单设「脚本/语音/字幕」中间表（中间产物是过程文件，不需要检索）。
- 输出的 mp4 落现有附件系统（`sys_attachment`），列表回显/下载复用附件下载接口。
- 租户隔离用 `organization_id`、主键 `id VARCHAR(32)` 雪花串、`create_time BIGINT` 毫秒——与 `ai_call_record`/`ai_dm_profile` 等既有 AI 表一致。

### 决策 8：爆款复刻 = Phase 2，核心是 LLM 文案仿写 + 复用同一视频管线

- 爆款复刻不是「分析视频画面」，而是「输入爆款文案/视频链接 → LLM 提取其选题结构/钩子/节奏 → 仿写同类口播文案 → 走同一套合成管线」。成本主要在 LLM 文案，管线零改动。
- 单独放 Phase 2，不在 MVP 排，避免把「视频链接解析」这个不确定依赖提前引入。

---

## 三、数据模型（改动）

### 3.1 新表（DDL，版本 1.36.0）

新版本迁移 `migration/1.36.0/ddl/V1.36.0_1__ai_video_task.sql`：

```sql
SET SESSION innodb_lock_wait_timeout = 7200;

-- 短视频生成（功能 7 MVP）：口播视频生成任务
CREATE TABLE ai_video_task
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    title           VARCHAR(256) COMMENT '主题/任务标题',
    script_text     TEXT         COMMENT '口播文案全文(分句)',
    voice           VARCHAR(32)  COMMENT '音色标识',
    style           VARCHAR(32)  COMMENT '风格/模板标识',
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/SCRIPTING/TTS/SYNTH/DONE/FAILED',
    progress        INT          NOT NULL DEFAULT 0 COMMENT '进度百分比(0-100)',
    attachment_id   VARCHAR(32)  COMMENT '输出视频附件ID(附件系统)',
    error_msg       VARCHAR(512) COMMENT '失败原因',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_status (organization_id, status, create_time)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
```

### 3.2 feature_code 注册

- **无需新增 DML**：`ai_video` 已在 `V1.16.0_2__edition_data.sql` 注册并挂 PRO + ENTERPRISE。
- 代码补常量：`AiQuotaConstant.AI_VIDEO = "ai_video";`（该 code 已在 `AI_FEATURE_CODES` 中预留）。

### 3.3 代码改动（非 SQL）

- 新增 `cn.cordys.crm.ai.video` 包（controller / service / dto / domain / client）。
- 新增 `TtsProvider` 接口 + `AliyunCosyVoiceTtsProvider` 实现 + `TtsProviderFactory`（与 ASR 同构），复用「阿里云」模型的百炼 API Key。
- 新增 `VideoSynthesizer`：FFmpeg 合成（ProcessBuilder），输入分句音频 + 字幕 + 背景，输出 mp4。
- 脚本生成复用功能 3（获客内容）的 LLM 能力，走 `resolveChatModels` 对话模型路由（口播文案属内容生成，非洞察/评估，与功能 3 一致，不用 `task_models`）。

---

## 四、接口清单

### 4.1 Java 侧 REST（前端调用，前缀 `/agent/video`）

| 接口 | 说明 |
|---|---|
| `GET /agent/video/available` | 是否开通（G3：`editionService.hasFeature(orgId, AiQuotaConstant.AI_VIDEO)`） |
| `POST /agent/video/tasks` | 提交生成任务（**异步**：立即返回 `taskId` + `status=PENDING`） |
| `GET /agent/video/tasks` | 本租户任务列表（分页，含状态/进度/缩略或标题） |
| `GET /agent/video/tasks/{id}` | 任务详情（进度、错误、输出附件 id） |
| `DELETE /agent/video/tasks/{id}` | 删除任务（同时删附件，仅本人） |

> 视频下载走现有附件下载接口（`attachment_id`），不单设下载端点。

提交任务请求/响应要点：

```
POST /agent/video/tasks
{ "title": "小龙虾店 3 周年店庆", "voice": "zh-CN-XiaoxiaoNeural", "style": "koushuo", "extraText": "可选补充素材" }
→ 走 G2 额度校验 → 立即返回 { "taskId": "...", "status": "PENDING" }
→ 后台 @Async：脚本(LLM) → 分句 TTS → 字幕 → FFmpeg 合成 → 附件落库 → status=DONE + attachment_id
```

轮询任务列表响应要点（前端每 3s）：

```
GET /agent/video/tasks
→ [{ "taskId": "...", "title": "...", "status": "SCRIPTING", "progress": 30 }, ...]
```

### 4.2 复用（既有能力）

- G3 门控：`EditionService.hasFeature`（同 `LeadCrawlController` 用法）。
- G2 计量：生成前 `AiQuotaService` 校验、生成后记 `ai_usage_record`（同会话军师/竞品洞察）。
- G1 模型路由：脚本生成走对话模型（`resolveChatModels`，与功能 3 获客内容一致）。
- 附件落库/回显/下载：复用现有附件系统（视频 mp4 上传走 `file.dir` 落盘 + `sys_attachment` 记录）。
- 异步线程池：复用现有 `@Async` 线程池（同通话复盘/私信画像）。

---

## 五、前端

- 「AI 中心」新增「短视频」入口（独立型），入口可见性由 `/available` 门控；未开通渲染「升级解锁」空态，不触发全局 403 跳转。
- 新增页面 `views/workbench/video/index.vue`（平行于 `views/workbench/callReview`、`dmProfile`），两个区块：
  1. **生成区**：主题输入框（必填）+ 补充素材（可选）+ 音色下拉（默认女声）+ 风格下拉（口播/促销）+ 「生成视频」按钮用 `AiActionButton`（大号）；生成中思考/进度提示用 `ShimmerText` 流光扫字 + 进度条。
  2. **任务列表区**：分页表格（标题/状态/进度/创建时间/操作），状态「进行中/已完成/失败」；已完成可「预览 + 下载」；失败回显 `error_msg`。
- 每个任务卡片/行带「配套文案」入口：出片后顺带生成抖音/小红书/朋友圈三套文案（复用功能 3），可复制。
- i18n：新增 key 同步维护 `locale/zh-CN.ts` 与 `en-US.ts`。
- 合规提示固定在页面底部橙色小字：AI 生成内容仅供参考、请人工审核后再发布；不自动发布，防止账号风控/封号。

---

## 六、权限与合规

1. **多租户隔离**：`ai_video_task` 按 `organization_id` 隔离，接口只取本租户数据。
2. **行级权限**：任务列表/详情/删除只操作本人创建的任务（`create_user` 校验）。
3. **数据合规（PIPL / 生成内容合规）**：文案生成喂模型的「主题/补充素材」不强制含个人信息；若租户输入含客户信息，沿用 G1 脱敏链路。
4. **AI 生成标识 + 免责**：口播视频为 AI 生成，前端固定「AI 生成仅供参考，发布前请人工审核」；视频可内置角标水印（可选，Phase 2）。
5. **不自动发布**：只产出文件 + 配套文案，**不接抖音/视频号自动发布**（防封号），发布动作始终由租户在平台侧手动完成。
6. **TTS 音色版权**：用阿里云 CosyVoice 官方合成音色（非克隆真人音色），规避声音克隆的授权问题；声音克隆（功能 11）必须带授权声明，另案。

---

## 七、验收要点（端到端）

- 专业版/企业版租户进「AI 中心 → 短视频」：`/available=true`，页面可打开；基础版租户看到「升级解锁」空态且接口不抛 403。
- 输入主题点生成 → 任务进入「进行中」，前端进度条实时刷新（脚本 → 配音 → 合成），结束后列表出现「已完成」视频。
- 完成的视频可在线预览、可下载 mp4（文件确实落 `file.dir` + 附件表，重启后仍可下载）。
- 失败场景有可读提示：如 LLM/TTS/FFmpeg 异常，任务归 `FAILED` 且 `error_msg` 回显在前端。
- 生成走 G2 额度：租户「次数」看板 + admin「成本」看板能看到消耗。
- 删除任务后：列表消失、对应附件文件被清理。
- 配套文案：完成后可复制抖音/小红书/朋友圈三套文案。

---

## 八、明确不在本 PRD 范围

- **数字人**（2D 真人感口型驱动，SadTalker/MuseTalk）——Phase 2，feature_code=`digital_human`（企业版）。
- **声音克隆**（功能 11）——能力组件，跟着数字人/机器人外呼一起做，不单独立项。
- **爆款复刻**（输入爆款视频/文案 → LLM 仿写）——Phase 2，复用同一视频管线。
- **自动发布**（抖音/视频号一键发布）——永久不做（防封号），只产出文件 + 文案。
- **自动配视频素材**（MoneyPrinterTurbo 的素材库检索/下载）——Phase 2；MVP 用固定背景模板（纯色/品牌图）。
- **增值包加购的计费细化**（TTS/合成成本转嫁的账务）——Phase 2。
