export default {
  'platformManualVerifyRules.intro':
    'The rules around payment write-off, renewal write-off, and Enterprise activation/revoke are the easiest to mix up. Here are the ones you must know.',

  'platformManualVerifyRules.flowTitle': 'Write-off & activation at a glance',
  'platformManualVerifyRules.section.activation.title': 'Write-off & Enterprise activation',
  'platformManualVerifyRules.item.firstVerify.name': 'First write-off auto-activates Enterprise',
  'platformManualVerifyRules.item.firstVerify.desc':
    'On a tenant\'s first payment write-off, the system automatically upgrades the plan from "Free trial" to "Enterprise" and records the effective and expiry times — no manual activation needed.',
  'platformManualVerifyRules.item.revoke.name': 'Revoke only reverses the books, not Enterprise',
  'platformManualVerifyRules.item.revoke.desc':
    'Revoking a verified payment only moves the record from "Done" back to "Pending write-off". An already-activated Enterprise plan is not turned off or rolled back.',
  'platformManualVerifyRules.item.renewalExtend.name': 'Only a renewal write-off extends the expiry',
  'platformManualVerifyRules.item.renewalExtend.desc':
    'Only the "Renewal request → write-off" path extends the tenant\'s expiry time. Revoking and re-verifying the same payment does not extend it again.',

  'platformManualVerifyRules.section.amount.title': 'Payment amount & entry points',
  'platformManualVerifyRules.item.fixedAmount.name': 'Payment amount is fixed to the contract amount',
  'platformManualVerifyRules.item.fixedAmount.desc':
    "When adding a payment you don't enter an amount manually; the system books the full contract amount in one go (one-time payment — a contract can only have one payment record, and it must be deleted before re-entering).",
  'platformManualVerifyRules.item.twoEntries.name': "Don't mix up the two write-off entry points",
  'platformManualVerifyRules.item.twoEntries.desc':
    'Payment write-off is under "Tenant Lifecycle → Payments"; renewal write-off is under "Tenant Lifecycle → Renewal". Both mean "payment confirmed", but the former activates Enterprise and the latter extends the expiry.',
};
