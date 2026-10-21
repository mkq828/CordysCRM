import { SysErrorLogListUrl } from '@lib/shared/api/requrls/system/sysErrorLog';
import type { CommonList } from '@lib/shared/models/common';
import type { SysErrorLogItem, SysErrorLogListParams } from '@lib/shared/models/system/sysErrorLog';

import CDR from '@/api/http/index';

// 系统错误日志-列表
// eslint-disable-next-line import/prefer-default-export -- 与其余 api 模块保持一致用命名导出
export function sysErrorLogList(data: SysErrorLogListParams) {
  return CDR.post<CommonList<SysErrorLogItem>>({ url: SysErrorLogListUrl, data });
}
