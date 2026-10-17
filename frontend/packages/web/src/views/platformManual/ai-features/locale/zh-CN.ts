export default {
  'platformManualAiFeatures.intro':
    'AI 功能多、放行规则也多。下面是「怎么放行、额度怎么扣、哪些暂时封存」的口径，需要时回来翻一眼。',

  'platformManualAiFeatures.flowTitle': '额度扣减顺序',
  'platformManualAiFeatures.section.release.title': 'AI 功能怎么放行',
  'platformManualAiFeatures.item.trial.name': '试用期放开全部 AI 功能',
  'platformManualAiFeatures.item.trial.desc':
    '目前所有 AI 功能都对试用租户放开，只受额度限制；额度用完会提示升级企业版，不单独关闭某个 AI 功能。举例：试用租户能用会话军师，也能用 AI 方案/PPT，只是共用额度。',
  'platformManualAiFeatures.item.featureCode.name': '放行按「功能 + 额度」控制',
  'platformManualAiFeatures.item.featureCode.desc':
    '每个 AI 功能对应一个功能标识（比如会话军师 = ai_advisor），是否放行由「套餐版本 + AI 额度」共同决定，租户不能自己开。举例：企业版能用，免费试用也能用，区别只在额度多少。',
  'platformManualAiFeatures.item.model.name': 'AI 模型由 admin 统一配',
  'platformManualAiFeatures.item.model.desc':
    'AI 用哪个模型、对话模型和任务模型分开配，只有平台 admin 能在「系统设置」里改，并按租户隔离。举例：想换更强的模型，只有 admin 去改，租户看不到也改不了。',

  'platformManualAiFeatures.section.quota.title': 'AI 额度怎么扣',
  'platformManualAiFeatures.item.monthFirst.name': '先扣月配额，再扣额度包',
  'platformManualAiFeatures.item.monthFirst.desc':
    '计费时优先扣「月配额」，月配额用完再扣购买的「额度包」，且具体额度优先于通用额度。',
  'platformManualAiFeatures.item.exhaust.name': '额度用尽会提醒',
  'platformManualAiFeatures.item.exhaust.desc': '租户 AI 额度用尽时，会发站内信通知租户管理员，引导升级企业版。',

  'platformManualAiFeatures.section.sealed.title': '哪些功能暂时只对 admin 可见',
  'platformManualAiFeatures.item.pending.name': '四个待完善功能已封存',
  'platformManualAiFeatures.item.pending.desc':
    '爆款复刻、短视频、PPT、漫剧目前收进「待完善功能」菜单，只对 admin 可见；将来要放开时只需改几处开关。',
  'platformManualAiFeatures.item.agentAdd.name': '智能体添加入口暂只 admin 可见',
  'platformManualAiFeatures.item.agentAdd.desc': '「添加智能体」入口目前封存，等你想清楚怎么给用户开放再放开。',
};
