# 军师联网查客户背景（DeepSeek Search 试点）

## 目标
军师分析对话时，可选开启「联网查客户背景」：按客户名/关键词调 DeepSeek Search，把带出处的公开信息注入分析上下文，结果里回传可点击出处。

## 方案
- 走 DeepSeek `/v1/tools/search`，key 读 sys_parameter `ai.deepseek.searchApiKey`（平台级，用户自填）。
- 搜索失败 / 未配 key 静默降级，不阻断军师主流程。

## 待办
- [ ] 迁移 SQL 种子 `ai.deepseek.searchApiKey`
- [ ] `AdvisorWebSource` 结果 DTO
- [ ] `SalesAdvisorAnalyzeRequest` 加 `enableWebSearch`
- [ ] `SalesAdvisorAnalyzeResponse` 加 `webSources`
- [ ] `DeepSeekSearchClient` 搜索客户端
- [ ] `SalesAdvisorService` 注入检索 + 拼上下文 + 回传出处
- [ ] 前端共享模型 + api 参数
- [ ] `salesAdvisor.vue` 开关
- [ ] `AdvisorResult.vue` 渲染出处链接
- [ ] locale zh-CN / en-US

## 验证
- [ ] 后端 `./mvnw -f backend/pom.xml -pl crm -am test`（或 compile）
- [ ] 前端 `pnpm --dir frontend --filter @cordys/web type:check` + lint
- [ ] 未配 key 时军师分析不受影响
