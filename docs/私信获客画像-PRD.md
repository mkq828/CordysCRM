# 私信获客画像（功能 8）— PRD（第一版）

> 关联：[AI模块-落地清单.md](./AI模块-落地清单.md)（功能 8 私信获客画像）
> 状态：待评审，评审通过后再动手
> 范围：抖音单平台私信获客画像（登录态迁移 → 读私信 → AI 画像 → 进客户池）。快手/小红书 Phase 2 复用同一套采集适配器。

---

## 一、背景与目标

**现状问题**：客户（餐饮引流私域）在抖音接私信，要人工一条条翻私信、记手机号、凭感觉判断「这人靠不靠谱」，再手动录进 CRM。效率低、漏单多、画像靠猜。

**目标**：租户绑定自己的抖音账号后，系统自动：

1. **读私信** — 拉取私信会话列表 + 消息原文（含群聊/私聊、粉丝数、抖音号）；
2. **提留资** — 正则提取手机号、微信号（平台不暴露，只能从私信正文里用户自留的号码提）；
3. **AI 画像** — 性别 / 年龄 / 城市 / 餐饮品类 / 喜好 / 私聊话术汇总（LLM 推断 + 置信度）；
4. **进客户池** — 一键把画像结果导入 CRM 线索池，接着走正常跟进。

**定位**（roadmap 功能 8）：独立型（进「AI 中心」）、生成型（画像每次调 LLM，挂 G2 额度 + G3 权限）、版本专业版+（Beta）。

**端到端主场景**：租户在自己电脑的浏览器里扫码登录抖音 → 一键导出 Cookie 粘贴进 CRM 绑定 → 点「开始采集」→ 私信自动落成画像列表 → 勾选导入线索池。

---

## 二、核心设计（关键决策，已拍板）

### 决策 1：登录 = 登录态迁移（Cookie 为主、扫码为备）

- 服务器上「扫码后二次验证」的根因，不是采集代码没做反检测，而是「在机房 IP 上扫码登录」这个动作本身命中抖音风控（机房 IP + 自动化指纹被判定为异常登录环境）。16.test 代码里已有 stealth 补丁和 `inject_cookies` 接口，但扫码登录在服务器上依然会被追加二次验证。
- **主路径（丝滑）**：租户在自己电脑的真实浏览器登录抖音 → 一键导出 Cookie（含 localStorage）→ 粘贴进 CRM 绑定 → 服务器只「带 Cookie 读私信」，不再做扫码登录，二次验证自然消失。
- **备路径（仅小号测试）**：服务器扫码（`/qrcode` + `/login-status`），界面注明「机房环境可能触发二次验证，仅建议小号测试用」。
- **理由**：把「登录」这个高风险动作从服务器挪回租户自己的可信环境，是唯一能稳定绕开二次验证的路；Cookie 迁移方案 16.test 已具备注入能力，改动最小。

### 决策 2：集成边界 = Python 只做「采集适配器」，画像走 CRM 自己的 G1 模型层

- **Python 16.test**：只负责扫码/Cookie 注入/读私信，返回「原始会话 + 消息」，不再自己做画像。
- **Java CRM**：负责账号绑定编排、G1 模型路由做画像、G2 额度计量、G3 权限门控、落库、导入线索池。
- **理由**：落地清单里画像属于 G1（模型可配置路由）+ G2（额度计量）+ G3（feature_code 权限）的范畴，这三层都在 Java 侧；若沿用 Python 里写死的 DeepSeek，等于绕开了模型路由和额度计量，和其他 AI 功能不一致，也无法计费。

### 决策 3：feature_code = `dm_profile`，专业版+，挂 G2 额度 + G3 权限

- `dm_profile` 已预留在 `AiQuotaConstant.AI_FEATURE_CODES`；补齐常量 `AI_DM_PROFILE = "dm_profile"` 并在 DML 里注册到 PRO 与 ENTERPRISE（专业版+）。
- 画像每次调 LLM，走生成型口径：挂 G2 额度计量（每「次」折算 token）+ G3 版本开关。
- **理由**：功能 8 在 roadmap 里标注「高 token 成本」，画像是真调模型，按生成型计费；版本归属专业版+（Beta）。

### 决策 4：数据模型两张表——账号绑定表 + 画像结果表

- `ai_dm_account`：租户绑定的平台账号（平台 + Cookie 密文 + 状态机 + 最近采集时间）。
- `ai_dm_profile`：私信联系人画像结果（原始消息 + 正则提取 + 结构化画像 + 导入线索池关联）。
- 租户隔离用 `organization_id`、主键 `id VARCHAR(32)` 雪花串、`create_time BIGINT` 毫秒——与 `ai_call_record` 等既有 AI 表一致。
- **理由**：账号（登录态）与画像（结果）生命周期不同，分开存；一张表混存会导致「解绑账号」和「看画像列表」互相牵扯。

### 决策 5：Cookie 属敏感登录凭据，加密落库、不回显

- Cookie 明文（含 `sessionid_ss`/`uid_tt`）用 `EncryptUtils.aesEncrypt` 加密后存 `ai_dm_account.cookie_ciphertext`，接口只收不吐；解绑即清空。
- **理由**：Cookie 等同于抖音账号登录态，泄露即账号失控；与项目已有的 AES 加解密工具一致，解密仅采集时在内存中使用。

### 决策 6：每账号一个持久化浏览器 profile，按需启动、采集完释放

- Beta 第一版限制「单租户单账号」，Python 侧每个已绑定账号对应一个持久化 profile（`BROWSER_PROFILE_DIR`），采集任务触发时启动、完成后关闭，不常驻。
- 采集频率内置限频（两次采集间隔下限），封号风险租户自担 + 界面免责声明 + 建议小号先试。
- **理由**：落地清单标注「每租户几百 MB 内存」，常驻会打爆服务器内存；Beta 用「按需 + 单账号」先把链路跑通，再谈多账号池化。

### 决策 7：手机号/微信号靠正则提，画像字段标「推断 + 置信度」

- 手机号 `1[3-9]\d{9}`、微信号从私信正文正则提取（平台不暴露，只能提用户自留的）。
- 性别/年龄/城市/品类/喜好等走 LLM 推断，`profile_json` 里带 `confidence`，前端展示「AI 生成仅供参考，可人工纠偏」。
- **理由**：对齐 16.test 技术方案「可直接获取 vs 需推断 vs 基本拿不到」的三类字段判断，不夸大画像准确率。

### 决策 8：采集异步化 + 配置预埋，上线后无需再改代码

- **采集异步**：`POST /accounts/{id}/crawl` 走 Spring `@Async`（复用现有 20 线程池），接口立即返回，账号进入 `CRAWLING` 状态，前端每 3s 轮询 `GET /accounts` + `GET /profiles` 感知进度，结束归位 `LOGGED_IN/EXPIRED/FAILED`。
- **配置预埋**：`ai.dm.baseUrl`（采集服务地址）与 `ai.dm.internalToken`（服务间鉴权 token）由迁移脚本 `V1.35.0_3__dm_profile_params.sql` 以空值预埋进 `sys_parameter`，部署时只填值、不改代码；未配置时调用会返回明确缺失提示。
- **多租户并发（本次已落地，非增量）**：Python 16.test 采用 **N 浏览器槽位池 + asyncio 任务队列**。`BROWSER_POOL_SIZE`（默认 2，每槽位独占一个浏览器 profile + 一个 Chromium 进程，约 300~500MB/槽位）决定同时能跑几个独立账号的采集；`POST /api/crawl/collect` 提交任务（Cookie 随请求体传入，服务端入队后立即返回 `task_id`），Java 侧 `@Async` 提交 + 每 3s 轮询 `GET /api/crawl/result/{task_id}`（`queued/running/done/failed`）直到结束。采集任务结果保留 `CRAWL_TASK_TTL`（默认 3600s）防内存无限增长。注入与采集在单个槽位内原子完成，避免并发串号；`logged_in=false` 归位 `EXPIRED`（重绑），采集失败归位 `FAILED`。
- **理由**：把「同步 HTTP 阻塞」和「部署后补基础设施」两件事都消掉，符合「能现在做好的不等」；异步 + 轮询是项目内已有模式（对齐智能通话复盘）。私信获客上线即热门、必然并发，浏览器池 + 任务队列在 Beta 就位，上线后只调 `BROWSER_POOL_SIZE` 不改架构。

---

## 三、数据模型（改动）

### 3.1 新表（DDL，版本 1.35.0）

新版本迁移 `migration/1.35.0/ddl/V1.35.0_1__ai_dm_account_profile.sql`：

```sql
SET SESSION innodb_lock_wait_timeout = 7200;

-- 私信获客画像（功能 8）：租户绑定的平台账号 + 登录态 Cookie 密文
CREATE TABLE ai_dm_account
(
    id                 VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id    VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    platform           VARCHAR(32)  NOT NULL COMMENT '平台: douyin/kuaishou/xiaohongshu',
    account_name       VARCHAR(128) COMMENT '账号昵称(绑定后回填)',
    unique_id          VARCHAR(64)  COMMENT '账号ID(抖音号)',
    cookie_ciphertext  TEXT         COMMENT '登录态Cookie密文(EncryptUtils.aesEncrypt)',
    status             VARCHAR(32)  NOT NULL DEFAULT 'UNBOUND' COMMENT '绑定状态: UNBOUND/LOGGED_IN/EXPIRED/FAILED',
    last_crawl_time    BIGINT       COMMENT '最近采集时间(毫秒)',
    last_error         VARCHAR(512) COMMENT '最近失败原因',
    create_time        BIGINT       COMMENT '创建时间(毫秒)',
    update_time        BIGINT       COMMENT '更新时间(毫秒)',
    create_user        VARCHAR(32)  COMMENT '创建人',
    update_user        VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_platform (organization_id, platform)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

-- 私信联系人画像结果：原始消息 + 正则提取 + LLM 结构化画像 + 导入线索池关联
CREATE TABLE ai_dm_profile
(
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    organization_id VARCHAR(50)  NOT NULL COMMENT '租户组织ID',
    account_id      VARCHAR(32)  COMMENT '来源账号(ai_dm_account.id)',
    platform        VARCHAR(32)  COMMENT '平台',
    sec_uid         VARCHAR(64)  COMMENT '平台用户 sec_uid',
    nickname        VARCHAR(128) COMMENT '昵称',
    unique_id       VARCHAR(64)  COMMENT '账号ID(抖音号)',
    follower_count  VARCHAR(32)  COMMENT '粉丝数(群聊固定"群聊")',
    is_group        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否群聊',
    phone           VARCHAR(32)  COMMENT '正则提取手机号',
    wechat          VARCHAR(64)  COMMENT '正则提取微信号',
    messages_json   TEXT         COMMENT '私信原文(结构化 sender/content)',
    profile_json    TEXT         COMMENT '画像JSON(性别/年龄/城市/品类/喜好/话术汇总/置信度)',
    status          VARCHAR(32)  NOT NULL DEFAULT 'RAW' COMMENT '状态: RAW/ANALYZED/IMPORTED/FAILED',
    clue_id         VARCHAR(32)  COMMENT '导入线索池后的线索ID',
    create_time     BIGINT       COMMENT '创建时间(毫秒)',
    update_time     BIGINT       COMMENT '更新时间(毫秒)',
    create_user     VARCHAR(32)  COMMENT '创建人',
    update_user     VARCHAR(32)  COMMENT '修改人',
    PRIMARY KEY (id),
    KEY idx_org_account (organization_id, account_id, create_time),
    KEY idx_org_sec (organization_id, sec_uid)
) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_general_ci;

SET SESSION innodb_lock_wait_timeout = DEFAULT;
```

### 3.2 feature_code 注册（DML，版本 1.35.0）

新版本迁移 `migration/1.35.0/dml/V1.35.0_2__dm_profile_feature.sql`：

```sql
SET SESSION innodb_lock_wait_timeout = 7200;

-- 功能点：私信获客画像（AI 画像挂 G2 额度 + G3 权限开关）
INSERT INTO sys_feature (id, feature_code, name, category, enable, create_user, create_time, update_user, update_time)
VALUES (UUID_SHORT(), 'dm_profile', '私信获客画像', 'ai', 1, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000);

-- 专业版+：专业版、企业版均可使用私信获客画像
INSERT INTO sys_edition_feature (id, edition_id, feature_id, create_user, create_time, update_user, update_time)
SELECT UUID_SHORT(), e.id, f.id, 'admin', UNIX_TIMESTAMP() * 1000, 'admin', UNIX_TIMESTAMP() * 1000
FROM sys_edition e
         JOIN sys_feature f ON f.feature_code = 'dm_profile'
WHERE e.code IN ('PRO', 'ENTERPRISE');

SET SESSION innodb_lock_wait_timeout = DEFAULT;
```

### 3.3 代码改动（非 SQL）

- `AiQuotaConstant`：新增 `public static final String AI_DM_PROFILE = "dm_profile";`（该 code 已在 `AI_FEATURE_CODES` 中预留）。
- 新增 `cn.cordys.crm.ai.dmprofile` 包（controller / service / dto / domain / client）。
- 新增 `DmProfileClient`：HTTP 调用 Python 16.test 微服务，baseUrl + 内部鉴权 token 走 `sys_parameter`（`ai.dm.baseUrl` / `ai.dm.internalToken`，已由迁移脚本预埋空值，部署时仅填值即可），失败可读提示、不吞异常。

---

## 四、接口清单

### 4.1 Java 侧 REST（前端调用，前缀 `/agent/dm-profile`）

| 接口 | 说明 |
|---|---|
| `GET /agent/dm-profile/available` | 是否开通（G3：`editionService.hasFeature(orgId, AiQuotaConstant.AI_DM_PROFILE)`） |
| `GET /agent/dm-profile/accounts` | 已绑定账号列表（不回显 Cookie 明文；含状态 `CRAWLING` 与 `lastError`） |
| `POST /agent/dm-profile/accounts` | 绑定账号（body：`platform` + `cookieText`，走 Cookie 迁移主路径） |
| `DELETE /agent/dm-profile/accounts/{id}` | 解绑（清 Cookie） |
| `POST /agent/dm-profile/accounts/{id}/crawl` | 触发采集（**异步**：立即返回「采集中」，采集走 `@Async` 后台线程） |
| `GET /agent/dm-profile/profiles` | 画像结果列表（按状态筛选） |
| `POST /agent/dm-profile/profiles/import` | 勾选画像导入线索池（复用 `ClueService.add`，标记来源「私信获客」） |

> 采集为异步执行：接口立即返回，账号状态变为 `CRAWLING`；前端轮询 `GET /accounts` 与 `GET /profiles` 感知进度，
> 结束后账号归位 `LOGGED_IN`（成功）/ `EXPIRED`（登录态过期）/ `FAILED`（失败，`lastError` 带原因）。不再单设 `crawl-status` 轮询接口。

绑定账号请求/响应要点：

```
POST /agent/dm-profile/accounts
{ "platform": "douyin", "cookieText": "sessionid_ss=...; uid_tt=...; ..." }
→ 校验关键 Cookie（sessionid_ss + uid_tt）→ EncryptUtils 加密落库 → 调 Python 注入并校验登录态
响应 { "id": "...", "status": "LOGGED_IN", "accountName": "...", "uniqueId": "..." }
```

导入画像响应要点（对齐线索爬取导入）：

```
POST /agent/dm-profile/profiles/import
{ "profileIds": ["...", "..."] }
→ 逐条导入，单条失败不阻断其余
响应 { "successCount": 2, "failedNames": ["张三"] }
```

### 4.2 Python 采集适配器契约（16.test）

| Python 接口 | 用途 | 状态 |
|---|---|---|
| `POST /api/login/cookie` | 注入 Cookie 并校验登录态（`sessionid_ss`+`uid_tt`），需带 `X-INTERNAL-TOKEN`；绑定账号时校验用 | 已具备（已加内部鉴权） |
| `POST /api/crawl/collect` | 提交采集任务：请求体 `{cookies}`，入队后立即返回 `{task_id, status:"queued"}`，采集在浏览器池 worker 内原子「注入+采集」 | 本次新增（异步任务队列） |
| `GET /api/crawl/result/{task_id}` | 轮询任务结果：`{status: queued/running/done/failed, ok, logged_in, convs, count, message}`，`done/failed` 结束 | 本次新增 |

- 采集契约：Cookie 随 `POST /api/crawl/collect` 请求体传入（不再依赖先注入全局单浏览器），每个任务独占一个浏览器槽位，避免多租户串号。
- 内部鉴权：Java 调用带 `X-INTERNAL-TOKEN`，Python 侧 `Config.INTERNAL_TOKEN` 校验，为空则跳过（仅本地开发）。
- 画像不落在 Python：Python 只返回原始会话（camelCase 结构），画像字段由 Java G1 层补。
- `logged_in=false` 表示 Cookie 已失效，Java 侧把账号归位 `EXPIRED`（需重新绑定）；采集失败（风控/超时）归位 `FAILED`。

### 4.3 复用（既有能力）

- G3 门控：`EditionService.hasFeature`（同 `LeadCrawlController` 用法）。
- G2 计量：画像调用走 `AiQuotaService`（同会话军师/竞品洞察的计量入口），画像前校验额度、画像后记 `ai_usage_record`。
- G1 模型路由：画像用任务模型（`task_models`，洞察评估类走会话军师同款路由）。
- 导入线索池：`ClueService.add`，线索来源自定义字段标「私信获客」（同 `LeadCrawlService.importToPool` 的 `clueSource` 写法）。

---

## 五、前端

- 「AI 中心」新增「私信获客画像」入口（独立型），入口可见性由 `/available` 门控；未开通渲染「升级专业版解锁」空态，不触发全局 403 跳转。
- 新增页面 `views/workbench/dmProfile/index.vue`（平行于 `views/workbench/callReview`），两个区块：
  1. **账号绑定区**：Cookie 粘贴框（默认主路径）+「切换扫码登录」折叠入口（备路径，橙色说明「机房环境可能触发二次验证，仅建议小号测试」）；已绑定时显示账号名/抖音号 + 最近采集时间 + 「重新绑定」「解绑」。
  2. **画像结果区**：分页表格（昵称/抖音号/粉丝数/手机号/微信号/画像摘要/状态/置信度），支持筛选「待导入」；顶部「开始采集」按钮用 `AiActionButton`（大号），采集/画像中思考提示用 `ShimmerText` 流光扫字。
- 行操作：勾选 + 「导入线索池」（导入后状态变「已导入」，可跳转对应线索详情）。
- 画像卡片：性别/年龄/城市/品类/喜好/话术汇总 + 橙色置信度说明「AI 生成仅供参考，可人工纠偏」。
- i18n：新增 key 同步维护 `locale/zh-CN.ts` 与 `en-US.ts`。
- 合规与风险提示固定在页面底部橙色小字：封号风险自担、建议小号先试、采集限频、仅处理租户自己账号的私信。

---

## 六、权限与合规

1. **多租户隔离**：`ai_dm_account` / `ai_dm_profile` 全部按 `organization_id` 隔离，接口只取本租户数据。
2. **行级权限**：账号绑定/采集/导入沿用现有登录态 + 组织上下文校验；不额外引入跨租户能力。
3. **敏感凭据**：Cookie 密文落库（`EncryptUtils`），接口不回显明文，解绑即清空；日志不打印 Cookie、Token 或私信原文。
4. **数据合规（PIPL）**：只处理租户自己账号收到的私信（用户主动来私信留资，合法性基础已覆盖大部分场景）；画像字段为推断值，前端固定「AI 生成仅供参考」；手机号/微信号属个人信息，仅存本租户 CRM、不外发。
5. **免责声明**：绑定页 + 画像页固定展示「浏览器自动化采集存在账号风控/封号风险，风险由租户自担；建议小号先试、控制采集频率」。
6. **限频**：两次采集间隔下限由 `sys_parameter` 配置，超频返回可读提示。

---

## 七、验收要点（端到端）

- 专业版租户进「AI 中心 → 私信获客画像」：`/available=true`，页面可打开；基础版租户看到「升级解锁」空态且接口不抛 403。
- 租户在自己电脑浏览器登录抖音 → 复制 Cookie 粘贴进绑定框 → 绑定成功，回显账号名/抖音号，Cookie 密文落库（查库不可读明文）。
- 点「开始采集」→ 私信自动拉回并逐条画像（性别/年龄/城市/品类/话术汇总带置信度）→ 表格实时刷新；手机号/微信号从正文正则提出来。
- 勾选若干条 → 导入线索池 → 线索列表出现对应记录且来源标「私信获客」，画像状态变「已导入」。
- 采集走 G2 额度：画像调用被计量，租户「次数」看板 + admin「成本」看板能看到消耗。
- Cookie 过期后（`sessionid_ss` 失效）→ 账号状态变「已过期」，前端提示重新绑定；解绑后 Cookie 清空。
- 备用扫码路径：小号测试时二维码能扫、状态能流转到「已登录」（界面已注明可能二次验证）。

---

## 八、明确不在本 PRD 范围

- 快手、小红书接入（Phase 2，复用同一套采集适配器接口与数据模型，加 platform 值即可）。
- 图片/语音私信留存与解密（涉及 AES-GCM 解密，Phase 2）。
- 秒级实时（企业号官方开放平台 Webhook）——第一版是轮询分钟级准实时，不对租户承诺秒级。
- 多账号池化 / 常驻浏览器进程（Beta 先单账号按需启动，后续再谈）。
- 官方开放平台 API 路线（企业号资质 + 私信/评论管理接口），有资质后再评估切换。
