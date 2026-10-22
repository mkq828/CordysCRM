export default {
  'platformManualNoticeRules.intro':
    '平台的每一类通知都集中在「消息设置」里统一管理，可以开关、改发送通道、改文案。下表列出全部平台通知，说明「什么时候触发、发给谁」；具体开关和通道以「消息设置」里的实际配置为准。',
  'platformManualNoticeRules.eventsTitle': '9 类平台通知',
  'platformManualNoticeRules.col.event': '通知',
  'platformManualNoticeRules.col.trigger': '什么时候触发',
  'platformManualNoticeRules.col.receiver': '发给谁',

  'platformManualNoticeRules.event.registerApply.name': '企业注册申请',
  'platformManualNoticeRules.event.registerApply.trigger': '企业提交注册后',
  'platformManualNoticeRules.event.registerApply.receiver': '平台管理员',
  'platformManualNoticeRules.event.registerApproved.name': '企业注册通过',
  'platformManualNoticeRules.event.registerApproved.trigger': '审核通过后',
  'platformManualNoticeRules.event.registerApproved.receiver': '申请人',
  'platformManualNoticeRules.event.registerRejected.name': '企业注册驳回',
  'platformManualNoticeRules.event.registerRejected.trigger': '审核驳回后',
  'platformManualNoticeRules.event.registerRejected.receiver': '申请人',
  'platformManualNoticeRules.event.planExpire.name': '套餐到期提醒',
  'platformManualNoticeRules.event.planExpire.trigger': '套餐快到期时',
  'platformManualNoticeRules.event.planExpire.receiver': '租户管理员',
  'platformManualNoticeRules.event.planApply.name': '续费 / 升级申请',
  'platformManualNoticeRules.event.planApply.trigger': '租户提交续费申请后',
  'platformManualNoticeRules.event.planApply.receiver': '平台管理员',
  'platformManualNoticeRules.event.planApproved.name': '续费通过',
  'platformManualNoticeRules.event.planApproved.trigger': '续费核销通过后',
  'platformManualNoticeRules.event.planApproved.receiver': '租户管理员',
  'platformManualNoticeRules.event.planRejected.name': '续费驳回',
  'platformManualNoticeRules.event.planRejected.trigger': '续费驳回后',
  'platformManualNoticeRules.event.planRejected.receiver': '租户管理员',
  'platformManualNoticeRules.event.quotaBreak.name': 'AI 额度用尽',
  'platformManualNoticeRules.event.quotaBreak.trigger': '租户 AI 额度用完时',
  'platformManualNoticeRules.event.quotaBreak.receiver': '租户管理员',
  'platformManualNoticeRules.event.paymentVerified.name': '回款核销到账',
  'platformManualNoticeRules.event.paymentVerified.trigger': '回款核销完成后',
  'platformManualNoticeRules.event.paymentVerified.receiver': '租户管理员',

  'platformManualNoticeRules.specialTitle': '特别说明',
  'platformManualNoticeRules.special.paymentNoSms':
    '回款核销只发站内信、不发短信：租户「个人中心 → 套餐」页会随核销自动刷新，不需要短信再催。',
  'platformManualNoticeRules.special.renewalNoDup':
    '续费核销通过走「续费通过」这条通知，不会再重复发「回款核销」通知，避免租户收到两条。',
  'platformManualNoticeRules.special.personalNoAudit': '个人注册默认免审、不发通知，提交后直接开通免费试用。',
  'platformManualNoticeRules.special.registerApplyWecomGroup':
    '企业注册申请现在会推送到企业微信群：在「组织设置 → 第三方集成/企业微信」里填「群机器人 Webhook」后，企业一提交注册就会推一条消息到管理员群（含公司名、联系人、电话、申请时间、审批链接），个人注册不推。群机器人模式不用填企业 ID / 应用 ID / 密钥，也不用配企业可信 IP。',
  'platformManualNoticeRules.special.wecomLoginClosed':
    '企微扫码登录已关闭：登录页只保留账号密码和短信验证码两种方式。注册审批通知里会附一句「暂不支持企微一键登录」的提示，引导用户用账号密码登录。',
};
