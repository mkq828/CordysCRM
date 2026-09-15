import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  registerApplicationApproveUrl,
  registerApplicationDetailUrl,
  registerApplicationListUrl,
  registerApplicationRejectUrl,
  registerApplyUrl,
  registerStatusUrl,
} from '@lib/shared/api/requrls/system/register';
import type { CommonList } from '@lib/shared/models/common';
import type {
  RegisterApproveParams,
  RegisterApplyParams,
  RegisterAuditItem,
  RegisterAuditQueryParams,
  RegisterRejectParams,
  RegisterStatusResult,
} from '@lib/shared/models/system/register';

export default function useRegisterApi(CDR: CordysAxios) {
  // 提交注册申请
  function apply(data: RegisterApplyParams) {
    return CDR.post({ url: registerApplyUrl, data });
  }

  // 查询注册审核状态
  function status(data: { phone: string }) {
    return CDR.post<RegisterStatusResult>({ url: registerStatusUrl, data });
  }

  // 注册申请-列表查询
  function pageList(data: RegisterAuditQueryParams) {
    return CDR.post<CommonList<RegisterAuditItem>>({ url: registerApplicationListUrl, data });
  }

  // 注册申请-详情
  function detail(id: string) {
    return CDR.get<RegisterAuditItem>({ url: `${registerApplicationDetailUrl}/${id}` });
  }

  // 注册申请-审核通过
  function approve(data: RegisterApproveParams) {
    return CDR.post({ url: registerApplicationApproveUrl, data });
  }

  // 注册申请-驳回
  function reject(data: RegisterRejectParams) {
    return CDR.post({ url: registerApplicationRejectUrl, data });
  }

  return {
    apply,
    status,
    pageList,
    detail,
    approve,
    reject,
  };
}
