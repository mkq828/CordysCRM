import type { CordysAxios } from '@lib/shared/api/http/Axios';
import { getPlatformDashboardUrl } from '@lib/shared/api/requrls/system/platformDashboard';
import type { PlatformDashboardResponse } from '@lib/shared/models/system/platformDashboard';

export default function usePlatformDashboardApi(CDR: CordysAxios) {
  // 平台大屏总览（仅admin）
  function getPlatformDashboard() {
    return CDR.get<PlatformDashboardResponse>({ url: getPlatformDashboardUrl });
  }

  return {
    getPlatformDashboard,
  };
}
