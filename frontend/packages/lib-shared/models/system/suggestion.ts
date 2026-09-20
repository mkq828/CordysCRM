export type SuggestionStatus = 'PENDING' | 'ADOPTED' | 'DEVELOPING' | 'RELEASED' | 'REJECTED';

export interface SuggestionImage {
  id: string;
  name: string;
  type?: string;
  size?: number;
}

export interface SuggestionItem {
  id: string;
  title: string;
  content: string;
  organizationId?: string;
  userId?: string;
  status: SuggestionStatus;
  imageIds?: string;
  voteCount: number;
  createTime: number;
  updateTime?: number;
  createUser?: string;
  updateUser?: string;
  userName?: string;
  organizationName?: string;
  commentCount?: number;
  voted?: boolean;
  imageList?: SuggestionImage[];
}

export interface SuggestionPageParams {
  current: number;
  pageSize: number;
  keyword?: string;
  status?: SuggestionStatus | '';
}

export interface SuggestionAddParams {
  title: string;
  content: string;
  imageIds?: string[];
}

export interface SuggestionCommentItem {
  id: string;
  suggestionId: string;
  userId?: string;
  content: string;
  replyCommentId?: string;
  createTime: number;
  userName?: string;
  replyUserName?: string;
}

export interface SuggestionCommentAddParams {
  suggestionId: string;
  content: string;
  replyCommentId?: string;
}

export interface SuggestionVoteResult {
  voteCount: number;
  voted: boolean;
}
