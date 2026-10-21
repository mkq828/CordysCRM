import { JobExecLogListUrl } from '@lib/shared/api/requrls/system/jobExecLog';
import type { CommonList } from '@lib/shared/models/common';
import type { JobExecLogItem, JobExecLogListParams } from '@lib/shared/models/system/jobExecLog';

import CDR from '@/api/http/index';

// 定时任务执行日志-列表
// eslint-disable-next-line import/prefer-default-export -- 与其余 api 模块保持一致用命名导出
export function jobExecLogList(data: JobExecLogListParams) {
  return CDR.post<CommonList<JobExecLogItem>>({ url: JobExecLogListUrl, data });
}
