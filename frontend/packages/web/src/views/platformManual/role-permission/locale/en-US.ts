export default {
  'platformManualRolePermission.intro':
    'Who sees what is decided by two layers: identity and permissions. Here are the four identities, how permissions take effect, and which menus are visible to whom.',

  'platformManualRolePermission.section.roles.title': 'Four identities at a glance',
  'platformManualRolePermission.item.admin.name': 'admin = platform super-admin',
  'platformManualRolePermission.item.admin.desc':
    'A hard-coded super-admin, not tied to any role, always has full access (platform operations, platform billing, AI quota management). Example: only admin can configure AI models and audit enterprise signups.',
  'platformManualRolePermission.item.orgAdmin.name': 'org_admin = tenant owner (legal person)',
  'platformManualRolePermission.item.orgAdmin.desc':
    'Auto-created when an enterprise signup is approved, for the tenant owner. Platform-side permissions have been removed — it only sees its own company. Example: the owner manages their own staff and customers but cannot see the "Platform Operations" menu.',
  'platformManualRolePermission.item.cityManager.name': 'city_manager = city manager (partner)',
  'platformManualRolePermission.item.cityManager.desc':
    'For city partners/managers, with city-scoped permissions (city org, city dashboard, city contract, city payment). Example: a city manager sees operating data of sub-organizations in their city.',
  'platformManualRolePermission.item.salesStaff.name': 'sales_staff = tenant salesperson',
  'platformManualRolePermission.item.salesStaff.desc':
    "An ordinary tenant role with only business permissions over the tenant's own customers, leads and follow-ups. Example: a salesperson sees no platform menus and no other tenant's data.",

  'platformManualRolePermission.section.cache.title': 'How permissions take effect',
  'platformManualRolePermission.item.reload.name': 'After changing roles, re-login to see it',
  'platformManualRolePermission.item.reload.desc':
    'Permissions are cached per user + tenant in Redis. After changing roles or permissions, the affected user may need to re-login (or have admin clear the permission cache) to see the change. Example: grant someone "Platform Billing" and they won\'t see it until they re-login.',

  'platformManualRolePermission.section.visible.title': 'Which menus are visible to whom',
  'platformManualRolePermission.item.platformMenus.name': 'Platform menus: admin + city manager',
  'platformManualRolePermission.item.platformMenus.desc':
    'Platform top-level menus like "Platform Operations", "Platform Billing" and "AI Quota Management" are visible only to platform admin and city managers; regular tenants (including org_admin) cannot see them.',
  'platformManualRolePermission.item.manual.name': 'This manual: admin + city manager',
  'platformManualRolePermission.item.manual.desc':
    'The "Admin Manual" is likewise visible only to platform admin and city managers, so you can come back to these rules any time.',
};
