export default {
  'platformManualAiFeatures.intro':
    'There are many AI features and many release rules. Here is the policy for how they are released, how quota is deducted, and which are temporarily sealed.',

  'platformManualAiFeatures.flowTitle': 'Quota deduction order',
  'platformManualAiFeatures.section.release.title': 'How AI features are released',
  'platformManualAiFeatures.item.trial.name': 'Trial opens all AI features',
  'platformManualAiFeatures.item.trial.desc':
    'Currently all AI features are open to trial tenants, limited only by quota; when quota runs out they are prompted to upgrade to Enterprise, and no single AI feature is turned off. Example: a trial tenant can use the Sales Advisor and AI Proposal/PPT, sharing the same quota.',
  'platformManualAiFeatures.item.featureCode.name': 'Release is gated by feature + quota',
  'platformManualAiFeatures.item.featureCode.desc':
    'Each AI feature maps to a feature code (e.g. Sales Advisor = ai_advisor). Whether it is available is decided by "plan version + AI quota" together; tenants cannot turn it on themselves.',
  'platformManualAiFeatures.item.model.name': 'AI model is configured centrally by admin',
  'platformManualAiFeatures.item.model.desc':
    'Which model AI uses — chat model and task model configured separately — can only be changed by platform admin in "System Settings", and this config is platform-shared: all tenants use the same set.',

  'platformManualAiFeatures.section.quota.title': 'How AI quota is deducted',
  'platformManualAiFeatures.item.monthFirst.name': 'Monthly quota first, then quota packs',
  'platformManualAiFeatures.item.monthFirst.desc':
    'Billing deducts the "monthly quota" first; once it runs out, purchased "quota packs" are deducted, with specific quota taking priority over general quota.',
  'platformManualAiFeatures.item.exhaust.name': 'Quota exhaustion triggers a reminder',
  'platformManualAiFeatures.item.exhaust.desc':
    "When a tenant's AI quota runs out, an in-app notice is sent to the tenant admin to guide them toward upgrading to Enterprise.",

  'platformManualAiFeatures.section.sealed.title': 'Which features are admin-only for now',
  'platformManualAiFeatures.item.pending.name': 'Four pending features are sealed',
  'platformManualAiFeatures.item.pending.desc':
    'Hit Replication, Short Video, PPT and Drama are currently tucked into the "Pending Features" menu, visible only to admin; to release them later you only change a few switches.',
  'platformManualAiFeatures.item.agentAdd.name': 'Agent add entry is admin-only for now',
  'platformManualAiFeatures.item.agentAdd.desc':
    'The "Add Agent" entry is currently sealed until you decide how to open it to users.',
};
