export default {
  'platformManualVerifyRules.intro':
    '回款核销、续费核销、企业版开通与撤回之间的口径最容易记混，下面是几条必须知道的规则。',

  'platformManualVerifyRules.flowTitle': '核销与开通，一张图看懂',
  'platformManualVerifyRules.section.activation.title': '核销与开通企业版',
  'platformManualVerifyRules.item.firstVerify.name': '首笔核销自动开通企业版',
  'platformManualVerifyRules.item.firstVerify.desc':
    '一个租户的第一笔回款核销时，系统自动把套餐从「免费试用」开通成「企业版」，并写入生效时间和到期时间，不需要再手工开通。',
  'platformManualVerifyRules.item.revoke.name': '撤回核销只退账、不关企业版',
  'platformManualVerifyRules.item.revoke.desc':
    '把已核销的回款撤回，只是把记账从「已完成」退回「待核销」，已经开通的企业版不会关掉、不会回滚。',
  'platformManualVerifyRules.item.renewalExtend.name': '只有续费核销才顺延有效期',
  'platformManualVerifyRules.item.renewalExtend.desc':
    '只有走「续费申请 → 核销」这条路径，租户的到期时间才会往后顺延；把同一笔回款「撤回再重新核销」不会重复顺延。',

  'platformManualVerifyRules.section.amount.title': '回款金额与入口',
  'platformManualVerifyRules.item.fixedAmount.name': '回款金额固定 = 合同金额',
  'platformManualVerifyRules.item.fixedAmount.desc':
    '新增回款不用手动填金额，系统按合同金额一次性全额记账（「一次性回款」口径，一个合同只能有一条回款，删除后才能重录）。',
  'platformManualVerifyRules.item.twoEntries.name': '两套核销入口别混',
  'platformManualVerifyRules.item.twoEntries.desc':
    '回款核销在「租户生命周期 → 回款」里；续费核销在「租户生命周期 → 续费申请」里。两者都表示「确认到账」，但作用不同：前者开企业版、后者顺延有效期。',
};
