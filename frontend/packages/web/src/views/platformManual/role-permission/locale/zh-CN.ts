export default {
  'platformManualRolePermission.intro':
    '平台的「谁看到什么」由身份和权限两层决定。下面把四类身份、权限怎么生效、菜单对谁可见说清楚。',

  'platformManualRolePermission.section.roles.title': '四类身份，一眼看懂',
  'platformManualRolePermission.item.admin.name': 'admin = 平台超管',
  'platformManualRolePermission.item.admin.desc':
    '系统写死的超级管理员，不挂在任何角色下，永远拥有全部权限（平台运营、平台收费、AI 额度管理这些平台菜单都能进）。举例：只有 admin 能配 AI 模型、能审核企业注册。',
  'platformManualRolePermission.item.orgAdmin.name': 'org_admin = 租户老板（法人）',
  'platformManualRolePermission.item.orgAdmin.desc':
    '企业注册审核通过时系统自动创建，给租户的老板用。它已经剥离了平台侧权限，只看得到自己公司的业务功能。举例：租户老板能管自己公司的员工和客户，但看不到「平台运营」菜单。',
  'platformManualRolePermission.item.cityManager.name': 'city_manager = 城市经理（城市合伙人）',
  'platformManualRolePermission.item.cityManager.desc':
    '给城市合伙人/城市经理用的角色，拥有城市运营相关的几块权限（城市组织、城市看板、城市合同、城市回款）。举例：城市经理能看到自己城市的下级组织经营数据。',
  'platformManualRolePermission.item.salesStaff.name': 'sales_staff = 租户一线销售',
  'platformManualRolePermission.item.salesStaff.desc':
    '租户内部普通销售角色，只有本公司的客户、线索、跟进等业务权限。举例：一线销售看不到任何平台菜单，也看不到别的公司的数据。',

  'platformManualRolePermission.section.cache.title': '权限怎么生效',
  'platformManualRolePermission.item.reload.name': '改了角色权限，重登才生效',
  'platformManualRolePermission.item.reload.desc':
    '权限按「用户 + 租户」缓存在 Redis。改完角色/权限后，相关用户可能要重新登录（或让管理员清权限缓存）才会立刻看到变化。举例：给某人加了「平台收费」权限，他当下看不到，重新登录后就出现了。',

  'platformManualRolePermission.section.visible.title': '菜单对谁可见',
  'platformManualRolePermission.item.platformMenus.name': '平台菜单：admin + 城市经理',
  'platformManualRolePermission.item.platformMenus.desc':
    '「平台运营」「平台收费」「AI 额度管理」等平台一级菜单，只对平台 admin 和城市经理可见；普通租户（含 org_admin）看不到。',
  'platformManualRolePermission.item.manual.name': '本手册：admin + 城市经理',
  'platformManualRolePermission.item.manual.desc':
    '「管理员手册」同样只对平台 admin 和城市经理可见，方便你随时回来翻这些规则。',
};
