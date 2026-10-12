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
