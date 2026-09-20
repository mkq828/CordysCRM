import type { CordysAxios } from '@lib/shared/api/http/Axios';
import {
  addSuggestionCommentUrl,
  addSuggestionUrl,
  deleteSuggestionUrl,
  getSuggestionCommentListUrl,
  getSuggestionDetailUrl,
  getSuggestionPageUrl,
  updateSuggestionStatusUrl,
  voteSuggestionUrl,
} from '@lib/shared/api/requrls/system/suggestion';
import type { CommonList } from '@lib/shared/models/common';
import type {
  SuggestionAddParams,
  SuggestionCommentAddParams,
  SuggestionCommentItem,
  SuggestionItem,
  SuggestionPageParams,
  SuggestionStatus,
  SuggestionVoteResult,
} from '@lib/shared/models/system/suggestion';

export default function useSuggestionApi(CDR: CordysAxios) {
  // 建议分页列表
  function getSuggestionPage(data: SuggestionPageParams) {
    return CDR.post<CommonList<SuggestionItem>>({ url: getSuggestionPageUrl, data });
  }

  // 建议详情
  function getSuggestionDetail(id: string) {
    return CDR.get<SuggestionItem>({ url: `${getSuggestionDetailUrl}/${id}` });
  }

  // 提交建议
  function addSuggestion(data: SuggestionAddParams) {
    return CDR.post<{ id: string }>({ url: addSuggestionUrl, data });
  }

  // 点赞/取消点赞
  function voteSuggestion(id: string) {
    return CDR.post<SuggestionVoteResult>({ url: `${voteSuggestionUrl}/${id}` });
  }

  // 评论列表
  function getSuggestionCommentList(suggestionId: string) {
    return CDR.get<SuggestionCommentItem[]>({ url: `${getSuggestionCommentListUrl}/${suggestionId}` });
  }

  // 发表评论
  function addSuggestionComment(data: SuggestionCommentAddParams) {
    return CDR.post({ url: addSuggestionCommentUrl, data });
  }

  // 更新状态（仅 admin）
  function updateSuggestionStatus(data: { id: string; status: SuggestionStatus }) {
    return CDR.post({ url: updateSuggestionStatusUrl, data });
  }

  // 删除建议（作者或 admin）
  function deleteSuggestion(id: string) {
    return CDR.post({ url: `${deleteSuggestionUrl}/${id}` });
  }

  return {
    getSuggestionPage,
    getSuggestionDetail,
    addSuggestion,
    voteSuggestion,
    getSuggestionCommentList,
    addSuggestionComment,
    updateSuggestionStatus,
    deleteSuggestion,
  };
}
