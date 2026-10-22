export default {
  'platformManualConfigItems.intro':
    'A few global settings can only be changed by admin, and they must actually take effect. Here are the common categories.',

  'platformManualConfigItems.section.aiModel.title': 'AI model config (admin only)',
  'platformManualConfigItems.item.dualModel.name': 'Chat and task models are configured separately',
  'platformManualConfigItems.item.dualModel.desc':
    'AI uses a "chat model" for daily conversation and general tasks, and a "task model" for insight/evaluation tasks — two separate columns, configured centrally by admin in System Settings and platform-shared across all tenants.',
  'platformManualConfigItems.item.tenantNoSelf.name': 'Tenants cannot configure models themselves',
  'platformManualConfigItems.item.tenantNoSelf.desc':
    'Which model a tenant uses is decided by admin; tenants are only allowed to use it per "plan version + AI quota" and cannot see or change the model.',

  'platformManualConfigItems.section.aiParam.title': 'AI-related parameters',
  'platformManualConfigItems.item.snippet.name': 'Knowledge base snippet length',
  'platformManualConfigItems.item.snippet.desc':
    'How long the quoted snippet from the knowledge base is is controlled by a parameter, default 200 characters; change it in the system parameters and it takes effect without code changes.',

  'platformManualConfigItems.section.internal.title': 'Self-hosted service allowlist',
  'platformManualConfigItems.item.allowlist.name': 'Internal services must be allowlisted first',
  'platformManualConfigItems.item.allowlist.desc':
    'For safety, the system blocks requests to localhost and internal addresses by default. If you self-host an internal service that AI needs to call, add its address to the allowlist first — otherwise the request is blocked and the feature fails.',

  'platformManualConfigItems.section.verify.title': 'Verify config actually takes effect',
  'platformManualConfigItems.item.wired.name': 'A config change must actually take effect',
  'platformManualConfigItems.item.wired.desc':
    'Every setting must go through the full "write → read → feature use → UI echo" chain and be tested, to avoid "configured but not effective". If nothing changes after editing, check that chain first instead of changing a second place.',
};
