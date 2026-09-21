import { BugReportListUrl, GetBugReportDetailUrl, SubmitBugReportUrl } from '@lib/shared/api/requrls/system/bugReport';
import type { CommonList } from '@lib/shared/models/common';
import type {
  BugReportDetail,
  BugReportItem,
  BugReportListParams,
  BugReportSubmitParams,
} from '@lib/shared/models/system/bugReport';

import CDR from '@/api/http/index';

// 提交问题反馈
export function submitBugReport(data: BugReportSubmitParams) {
  return CDR.post({ url: SubmitBugReportUrl, data });
}

// 问题反馈-列表
export function bugReportList(data: BugReportListParams) {
  return CDR.post<CommonList<BugReportItem>>({ url: BugReportListUrl, data });
}

// 问题反馈-详情
export function bugReportDetail(id: string) {
  return CDR.get<BugReportDetail>({ url: `${GetBugReportDetailUrl}/${id}` });
}
