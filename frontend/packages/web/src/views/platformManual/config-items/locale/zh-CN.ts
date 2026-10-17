export default {
  'platformManualConfigItems.intro':
    '一些影响全局的配置只有 admin 能改，且改了要能真正生效。下面把常见的几类配置说清楚。',

  'platformManualConfigItems.section.aiModel.title': 'AI 模型配置（只有 admin 能改）',
  'platformManualConfigItems.item.dualModel.name': '对话、任务两类模型分开配',
  'platformManualConfigItems.item.dualModel.desc':
    'AI 用「对话模型」做日常对话和通用任务，用「任务模型」做洞察评估类任务，两列分开，都在系统设置里由 admin 统一配，按租户隔离。',
  'platformManualConfigItems.item.tenantNoSelf.name': '租户不能自配模型',
  'platformManualConfigItems.item.tenantNoSelf.desc':
    '租户用哪个模型由 admin 定，租户只按「套餐版本 + AI 额度」被放行使用，看不到、也不能自己改模型。',

  'platformManualConfigItems.section.aiParam.title': 'AI 相关参数',
  'platformManualConfigItems.item.snippet.name': '知识库引用片段长度',
  'platformManualConfigItems.item.snippet.desc':
    'AI 引用知识库原文时，贴出来的片段有多长由参数控制，默认 200 字；在系统参数里改即生效，不需要改代码。',

  'platformManualConfigItems.section.internal.title': '自建服务白名单',
  'platformManualConfigItems.item.allowlist.name': '内网自建服务要先加白名单',
  'platformManualConfigItems.item.allowlist.desc':
    '为安全起见，系统默认拦住访问本机和内网地址的请求。如果你自建了内部服务要 AI 去调用，得先把它的地址加进白名单，否则请求会被拦截、功能会失败。',

  'platformManualConfigItems.section.verify.title': '改配置要验证生效',
  'platformManualConfigItems.item.wired.name': '配置改了要真正生效',
  'platformManualConfigItems.item.wired.desc':
    '每一项配置都要走「写入 → 读取 → 功能使用 → 界面回显」完整链路并实测，避免「配了但没生效」。如果改完没反应，先检查这条链路，别急着改第二处。',
};
