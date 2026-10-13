# AI 漫剧（功能 7 扩展）— printfilm drama 移植

范围：完整脚本工作流（列表/新建/大纲/资产库/分镜编辑/生成/合成），跳过 React Flow 画布与 Agent Skill。方案见 `/Users/mac/.claude/plans/witty-gathering-newell.md`。

## Phase 1 后端地基
- [x] 常量 AI_DRAMA="ai_drama" + AI_FEATURE_CODES
- [x] 迁移 DDL 1.47.0（ai_drama_project/script/episode/fragment/asset 5 表）
- [x] 迁移 DML 1.47.0（sys_feature + sys_edition_feature 门控）
- [x] domain：AiDramaProject/Script/Episode/Fragment/Asset
- [x] DramaController + DramaService（CRUD + available/models/voices）
- [x] 编译通过

## Phase 2 前端骨架 + 列表/新建
- [x] 路由 modules/drama.ts + routeEnum + locale menu.aiDrama
- [x] lib-shared api + requrls（drama 方法）
- [x] views/drama/index.vue（列表 + 内联新建）
- [x] printfilm.css（pf 组件类 + pf-drama 列表样式移植）
- [x] 页面可进、可建项目（type:check + eslint 通过）

## Phase 3 剧情大纲
- [x] script_summary + episode_script LLM（DramaGenerateService 异步编排 + 轮询）
- [x] workspace.vue 大纲视图 + 分集目录（自动流水线 + 加集 + 创意/摘要/剧本三卡 + 全局设定弹窗）

## Phase 4 资产库 ✅（2026-10-10 完成）
- [x] 后端 seed_from_script 抽取（seed 接口 + paramsJson assets_seed_status 状态）
- [x] 后端 GET /agent/drama/assets（全局资产库列表，按 org 隔离）
- [x] 文生图（generate 接口 + 前端 setInterval 轮询 3 分钟超时）
- [x] 上传/更换图片（upload 接口 + 前端 file 上传）
- [x] 提示词编辑保存（update 接口 + buildPromptParams）
- [x] assetsStep.vue 工作台资产视图（角色/场景/道具三 Tab + 自动抽取 + 详情弹窗 + 放大）
- [x] assets.vue 全局资产库页（分类 Tab + 项目筛选 + 搜索 + 打开项目）
- [ ] 试听 —— 延后到 Phase 6（音色绑定跳过，试听随合成配音一起做）
- [x] 验证：后端编译 + 前端 type:check/eslint/stylelint + 生产构建全通过

## Phase 5 分镜生成 ✅（2026-10-10 完成）
- [x] 后端 plan_fragments（LLM 逐镜规划：content 画面 + narration 口播 + durationSec + 引用资产）
- [x] 后端 save_fragments（按 id 增量 upsert，未提交分镜删除）
- [x] 后端 generate + generate_status（Seedream 文生图逐镜异步 + 轮询）
- [x] 后端 ensureEpisodeRows（分集剧本完成后物化 ai_drama_episode 行，供分镜挂靠）
- [x] episode.vue 四栏：左分集目录 + 中分镜编辑器 + 右预览 + 底部分镜条
- [x] 工作台大纲视图「🎬 分镜编辑」入口跳转
- [x] 验证：后端编译 + 前端 type:check/eslint/stylelint + 生产构建全通过（E2E 归 Phase 8）

## Phase 6 合成成片 ✅（2026-10-10 完成）
- [x] 后端 DramaVideoComposer（逐镜 ffmpeg 出 segment → concat 合成 1080×1920 30fps mp4，ffprobe 取时长）
- [x] 后端口播配音 POST /episodes/{id}/voice（@Async 逐镜 TTS → fragment.narrationAttachmentId，状态入 paramsJson narration_status）
- [x] 后端试听 POST /fragments/{id}/voice-preview（返回 base64 WAV）
- [x] 后端合成 POST /episodes/{id}/compose（@Async 封面+口播 → episode.videoAttachmentId，compose_status 状态）
- [x] 前端 episode.vue 顶栏：配音 / 合成 / 下载（下载 disabled 到 hasVideo）+ 每镜试听按钮 + 轮询
- [x] lib-shared models/api/requrls + web api re-export 打通
- [x] 验证：后端编译 + 前端 type:check + 生产构建全通过（E2E 归 Phase 8）

## Phase 7 Seedance 图生视频 ✅（2026-10-10 完成）
- [x] 后端 POST /episodes/{id}/generate-video（@Async 逐镜 Seedance 图生视频 → fragment.videoAttachmentId，3 并发）
- [x] 后端 resolveSeedanceModel（豆包 seedance 系列）+ buildFragmentVideoPrompt（画面描述+风格+角色锚点+运镜）
- [x] 后端 DramaVideoComposer 支持视频素材（-stream_loop 循环补齐，静图/视频二选一）
- [x] composeAsync 优先用视频片段合成（有视频用视频，否则回退静图 slideshow）
- [x] 分集 videoStatus/videoError 落 paramsJson 并扁平化到 DramaEpisodeResponse
- [x] 前端 episode.vue 顶栏「生成视频」按钮 + videoStatus 轮询 + 分镜「已出视频」徽标
- [x] 验证：后端编译 + 前端 type:check/eslint + 生产构建全通过（E2E 归 Phase 8）

## Phase 8 打磨收尾 ✅（2026-10-10 完成）
- [x] 额度全链路 —— 已核：chatText 走 checkQuota + checkModelDailyLimit + record(AI_DRAMA)，Phase 1/3 已接，无需新增
- [x] 视觉对齐 printfilm —— printfilm.css（136KB）已移植，drama 全部视图引用
- [x] 落地清单勾选 —— docs/AI创作中心-融合方案.md Phase 2 漫剧骨架 + Phase 3 Seedance 图生视频 勾 [x]；docs/AI模块-落地清单.md 功能 7 增「进度（漫剧）」
- [x] E2E 环境就绪 —— 后端 8081 / 前端 5173 已起，/agent/drama 全部端点注册（未鉴权 401 通，含 Phase 6/7 voice/compose/generate-video）；完整生成链路需 admin 在「模型设置」配好 Seedream/Seedance/TTS 凭据后按验收步骤走

## 验证
- [x] 后端 clean package BUILD SUCCESS（Phase 1~7 各阶段编译通过）
- [x] 前端 type:check + eslint + build（Phase 2~7 各阶段通过）
- [ ] E2E：admin 新建「我的创业故事」→ 大纲→资产→分镜→合成→下载（环境已就绪、端点已注册；完整链路依赖 admin 模型凭据，见 Phase 8 说明）
============
完整生成链路验收步骤
当前环境：后端 http://localhost:8081、前端 http://localhost:5173 均已启动；MySQL 9.6 / Redis 正常。

测试账号：admin（账号 100001，试用态租户，已放开全部 AI 功能）。⚠️ 走通完整生成前，需先在「模型设置」里把 Seedream（文生图）、Seedance（图生视频）、TTS（配音）三类模型的凭据填好——这三类调用不走 LLM 额度，但需要真实 key 才能出图/出视频/出音。

步骤：

登录 → 左侧菜单「AI 漫剧」→ 点「新建项目」→ 填「我的创业故事」创意 + 选画面风格 → 立即生成
工作台大纲视图 → 等待 AI 自动跑出「分集剧本 + 分集目录」
切「资产库」→ 首次进入自动从剧本抽取角色/场景/道具 → 逐个「生成形象」
任选一集点「分镜编辑」→ 四栏布局里「分镜规划」→「整集生成」逐镜出画面
顶栏依次「生成配音」→（可选）「生成视频」→「合成成片」→「下载成片」出 1080×1920 mp4
可选深度：第 5 步的「生成视频」走 Seedance 逐镜图生视频，成片画面会真动起来；不点则回退为「静图 + 口播」slideshow 兜底档。

说明：三条 AI 生成链（LLM 剧本、Seedream 生图、TTS 配音、Seedance 生视频）目前只完成「代码 + 端点注册 + 编译构建」验证，端到端生成需要真实模型凭据，这一环我没法替你配 key，需你在「模型设置」填入后再按上面步骤验收。

需要我接着做的话，有两个可选项：① 把本次漫剧全部改动整理成一个 Conventional Commit 提交（目前 drama 代码仍是未跟踪状态）；② 补一篇漫剧的「模型设置」字段说明，明确 Seedream/Seedance/TTS 各要填哪几个 key。你定。

换参考底图：资产库 → 角色「邓总」→ 把原来的黑底真人照，换成你准备的半写实国漫三视图（原创手绘质感，别用照片转漫画滤镜）。
选非写实风格：项目工作台 →「剧情大纲」视图左上角「项目风格」→ 选一个非写实风格（建议「3D 动画」或「宫崎骏气质手绘」，别选带「写实」字样的）。
重生成角色资产：每个角色卡片点「生成/重新生成」，现在邓总会按国漫画风重绘。
重生成分镜画面：进单集编辑，逐镜点「重新生成画面」（或整集重跑）。
重新生成视频：点「生成视频」，此时分镜图是国漫人物，不会触发 Seedance 真人风控。

「真人镜头走自建 LivePortrait/MuseTalk、场景镜头走 Seedance」已写进永久记忆（[[ai-drama-real-person-video-liveportrait-musetalk]]），后续排期不会丢。

真正「零风控」的只有自建开源，本地跑没人拦你，真人脸随便用：
LivePortrait（商汤，MIT）——让静态真人照片「动起来」（转头/眨眼/表情），适合邓总说话/反应特写；
MuseTalk（腾讯，MIT）——音频驱动口型，真人开口说话；
AnimateDiff / DynamiCrafter / SVD——通用图生视频（镜头/场景运动）。
代价：要自建 GPU 服务 + 集成 + 调质量。这条我们之前已经记过（MuseTalk 首选、数字人排最后，见 [[digital-human-lipsync-musetalk]]），属于后面的独立里程碑。