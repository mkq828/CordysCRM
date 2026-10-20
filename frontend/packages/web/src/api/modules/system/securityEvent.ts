import { SecurityEventExportUrl, SecurityEventListUrl } from '@lib/shared/api/requrls/system/securityEvent';
import type { CommonList } from '@lib/shared/models/common';
import type { SecurityEventItem, SecurityEventListParams } from '@lib/shared/models/system/securityEvent';

import CDR from '@/api/http/index';

// 安全事件-列表
export function securityEventList(data: SecurityEventListParams) {
  return CDR.post<CommonList<SecurityEventItem>>({ url: SecurityEventListUrl, data });
}

// 安全事件-导出CSV
export function securityEventExport(data: SecurityEventListParams) {
  return CDR.get({ url: SecurityEventExportUrl, params: data, responseType: 'blob' }, { isTransformResponse: false });
}
