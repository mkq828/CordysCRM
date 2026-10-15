export interface AiModelParams {
  temperature?: number | null;
  max_tokens?: number | null;
  top_p?: number | null;
}

export interface AiModelSaveParams {
  id?: string;
  displayName: string;
  modelName?: string;
  provider: string;
  apiUrl?: string;
  apiKey?: string;
  /** 火山语音 AppID（豆包语音三件套鉴权） */
  appId?: string;
  /** 火山语音 AccessToken（豆包语音三件套鉴权） */
  accessToken?: string;
  /** 火山语音 SecretKey（豆包语音三件套鉴权） */
  secretKey?: string;
  enable: boolean;
  globalDailyLimit?: number;
  userDailyLimit?: number;
  modelParams?: string;
  /** 声音克隆模型名（阿里云声音复刻 target_model，如 qwen-audio-3.1-tts-flash） */
  voiceCloneModel?: string;
  /** 模型描述（短视频风格页展示） */
  description?: string;
  /** 是否推荐模型（短视频风格页「推荐」标识） */
  recommended?: boolean;
}

export interface AiModelItem extends AiModelSaveParams {
  id: string;
  dailyTotal: number;
  updateUserName: string;
  createUserName: string;
  createTime: number;
  updateTime: number;
}

export interface AiModelStatusParams {
  id: string;
}

export interface AiModelOption {
  id: string;
  name: string;
  idAsString: string;
}

export interface AiModelRouteStrategy {
  id?: string;
  chatModels: string[];
  taskModels: string[];
  ttsModels?: string[];
  fallback: boolean;
}

/** 模型应用场景矩阵：单个模型摘要（不暴露密钥） */
export interface AiModelFeatureUsageModel {
  displayName: string;
  modelName?: string;
  provider: string;
}

/** 模型应用场景矩阵：单个用途及解析到的模型候选链 */
export interface AiModelFeatureUsageRoute {
  route: string;
  routeName: string;
  models: AiModelFeatureUsageModel[];
}

/** 模型应用场景矩阵：单个 AI 功能 → 用途列表 */
export interface AiModelFeatureUsage {
  featureCode: string;
  featureName: string;
  remark?: string;
  usages: AiModelFeatureUsageRoute[];
}
