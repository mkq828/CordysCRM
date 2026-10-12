/** 资产库共用工具：解析资产 paramsJson、读取形象图与生图提示词（1:1 移植 printfilm，字段改驼峰）。 */
import type { DramaAsset } from '@lib/shared/models/ai';

/** 资产 paramsJson 解析结果（后端以字符串存 JSON） */
export interface DramaAssetParams {
  visualPrompt?: string;
  visualImage?: string;
  kind?: string;
  aspectRatio?: string;
  roleType?: string;
  title?: string;
  coreTags?: string;
  personality?: string;
  [key: string]: unknown;
}

/** 安全解析资产 paramsJson */
export function parseAssetParams(asset: DramaAsset | null | undefined): DramaAssetParams {
  if (!asset?.paramsJson) return {};
  try {
    return JSON.parse(asset.paramsJson) as DramaAssetParams;
  } catch {
    return {};
  }
}

/** 生图提示词：visualPrompt 优先，其次 visualImage（生成时后端写这两个字段） */
export function readVisualPrompt(asset: DramaAsset | null | undefined): string {
  const params = parseAssetParams(asset);
  return String(params.visualPrompt || params.visualImage || '').trim();
}

/** 资产是否已有形象图（AI 生成或本地上传均写入 coverAttachmentId） */
export function assetHasImage(asset: DramaAsset | null | undefined): boolean {
  return Boolean(asset?.coverAttachmentId);
}

const NON_IMAGE_KINDS = ['voice', 'video', 'audio', 'text'];

/** 是否仍需生图（无图且非纯音频/视频类资产） */
export function assetNeedsImageGeneration(asset: DramaAsset): boolean {
  const kind = (asset.type || '').toLowerCase();
  if (NON_IMAGE_KINDS.includes(kind)) return false;
  return !assetHasImage(asset);
}

export type DramaAssetKind = 'character' | 'scene' | 'prop' | 'other';

/** 资产归入角色 / 场景 / 道具 / 其它 */
export function assetKind(asset: DramaAsset): DramaAssetKind {
  const t = (asset.type || '').toLowerCase();
  if (t === 'character' || t === 'scene' || t === 'prop') return t;
  return 'other';
}

/** 读取项目 paramsJson 里的资产抽取状态（generating/done/failed/空） */
export function readProjectAssetsSeedStatus(project: { paramsJson?: string | null } | null | undefined): string {
  if (!project?.paramsJson) return '';
  try {
    const obj = JSON.parse(project.paramsJson) as { assets_seed_status?: string };
    return obj.assets_seed_status || '';
  } catch {
    return '';
  }
}

/** 读取项目 paramsJson 里的本次新建资产数 */
export function readProjectAssetsSeedCreated(project: { paramsJson?: string | null } | null | undefined): number {
  if (!project?.paramsJson) return 0;
  try {
    const obj = JSON.parse(project.paramsJson) as { assets_seed_created?: number };
    return Number(obj.assets_seed_created || 0);
  } catch {
    return 0;
  }
}
