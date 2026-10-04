import type { CommonList } from '../../models/common';
import type {
  CallReviewConfigResponse,
  CallReviewConfigSaveRequest,
  CallReviewDetailResponse,
  CallReviewPageRequest,
  CallReviewResponse,
  CallReviewUploadRequest,
} from '../../models/callReview';
import {
  CallReviewAvailableUrl,
  CallReviewConfigResetKeyUrl,
  CallReviewConfigUrl,
  CallReviewDetailUrl,
  CallReviewFollowedUrl,
  CallReviewPageUrl,
  CallReviewRetryUrl,
  CallReviewUploadUrl,
} from '../requrls/callReview';
import type { CordysAxios } from '../http/Axios';

export default function useCallReviewApi(CDR: CordysAxios) {
  function getCallReviewAvailable() {
    return CDR.get<boolean>({ url: CallReviewAvailableUrl });
  }

  function getCallReviewPage(data: CallReviewPageRequest) {
    return CDR.post<CommonList<CallReviewResponse>>({ url: CallReviewPageUrl, data });
  }

  function getCallReviewDetail(id: string) {
    return CDR.get<CallReviewDetailResponse>({ url: `${CallReviewDetailUrl}/${id}` });
  }

  function uploadCallReview(data: CallReviewUploadRequest) {
    return CDR.post<string>({ url: CallReviewUploadUrl, data });
  }

  function retryCallReview(id: string) {
    return CDR.post({ url: `${CallReviewRetryUrl}/${id}` });
  }

  function markCallReviewFollowed(id: string, followRecordId: string) {
    return CDR.post({ url: `${CallReviewFollowedUrl}/${id}`, data: { followRecordId } });
  }

  function getCallReviewConfig() {
    return CDR.get<CallReviewConfigResponse>({ url: CallReviewConfigUrl });
  }

  function saveCallReviewConfig(data: CallReviewConfigSaveRequest) {
    return CDR.post<CallReviewConfigResponse>({ url: CallReviewConfigUrl, data });
  }

  function resetCallReviewConfigKey() {
    return CDR.post<CallReviewConfigResponse>({ url: CallReviewConfigResetKeyUrl });
  }

  return {
    getCallReviewAvailable,
    getCallReviewPage,
    getCallReviewDetail,
    uploadCallReview,
    retryCallReview,
    markCallReviewFollowed,
    getCallReviewConfig,
    saveCallReviewConfig,
    resetCallReviewConfigKey,
  };
}
