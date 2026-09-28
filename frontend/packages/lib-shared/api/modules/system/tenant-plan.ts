import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  tenantPlanConfigUrl,
  tenantPlanDemoUrl,
  tenantPlanListUrl,
  tenantPlanOpenUrl,
  tenantPlanToggleUrl,
} from '@lib/shared/api/requrls/system/tenant-plan';
import type { CommonList } from '@lib/shared/models/common';
import type {
  TenantPlanConfig,
  TenantPlanDemoParams,
  TenantPlanItem,
  TenantPlanOpenParams,
  TenantPlanQueryParams,
  TenantPlanToggleParams,
} from '@lib/shared/models/system/tenant-plan';

export default function useTenantPlanApi(CDR: CordysAxios) {
  // 付费用户-列表查询
  function pageList(data: TenantPlanQueryParams) {
    return CDR.post<CommonList<TenantPlanItem>>({ url: tenantPlanListUrl, data });
  }

  // 付费用户-开通/续费
  function open(data: TenantPlanOpenParams) {
    return CDR.post({ url: tenantPlanOpenUrl, data });
  }

  // 付费用户-启用/禁用
  function toggle(data: TenantPlanToggleParams) {
    return CDR.post({ url: tenantPlanToggleUrl, data });
  }

  // 付费用户-演示标记
  function toggleDemo(data: TenantPlanDemoParams) {
    return CDR.post({ url: tenantPlanDemoUrl, data });
  }

  // 付费用户-全局配置查询
  function getConfig() {
    return CDR.get<TenantPlanConfig>({ url: tenantPlanConfigUrl });
  }

  // 付费用户-全局配置更新
  function updateConfig(data: TenantPlanConfig) {
    return CDR.post({ url: tenantPlanConfigUrl, data });
  }

  return {
    pageList,
    open,
    toggle,
    toggleDemo,
    getConfig,
    updateConfig,
  };
}
