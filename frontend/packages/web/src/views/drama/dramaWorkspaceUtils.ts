/** 剧情大纲工作台共享工具：解析后端剧本 JSON 字符串、分集目录与状态（1:1 移植 printfilm，字段改驼峰）。 */
import type { DramaEpisodeBody, DramaProject, DramaScript } from '@lib/shared/models/ai';

/** 与后端 MIN_EPISODE_CONTENT_CHARS 对齐：正文去空白后不足该长度视为未完成 */
export const MIN_EPISODE_BODY_CHARS = 450;
export const MIN_EPISODE_CREATIVE_CHARS = 20;

/** 剧本 paramsJson 解析后的状态字段 */
export interface DramaScriptParams {
  episode_content_status?: string;
  episode_content_progress?: { done?: number; total?: number };
  episode_count?: number;
  image_style_id?: string;
  summary_error?: string;
  episode_content_error?: string;
  summary_running_at?: number;
}

export type OutlineDirectoryEpisode = DramaEpisodeBody;

/** 安全解析 JSON 字符串（后端以字符串存 JSON） */
export function parseJson<T = unknown>(raw: string | null | undefined): T | null {
  if (!raw) return null;
  try {
    return JSON.parse(raw) as T;
  } catch {
    return null;
  }
}

/** 解析剧本 paramsJson */
export function parseScriptParams(script: DramaScript | null | undefined): DramaScriptParams {
  return parseJson<DramaScriptParams>(script?.paramsJson) ?? {};
}

/** 解析剧情摘要 JSON */
export function parseSummary(script: DramaScript | null | undefined): Record<string, unknown> | null {
  return parseJson<Record<string, unknown>>(script?.summaryJson);
}

/** 解析 episode_content 为分集数组（后端存 {episodes:[...]}） */
export function parseEpisodeBodies(script: DramaScript | null | undefined): DramaEpisodeBody[] {
  const root = parseJson<{ episodes?: DramaEpisodeBody[] } | DramaEpisodeBody[]>(script?.episodeContentJson);
  if (!root) return [];
  if (Array.isArray(root)) return root as DramaEpisodeBody[];
  return root.episodes ?? [];
}

/** 去空白后的正文字数 */
export function episodeBodyCharLen(text: string | undefined | null): number {
  return (text || '').replace(/\s/g, '').length;
}

/** 手动加集（等待用户贴剧本）不会被自动流水线填满 */
export function isManualEpisode(ep?: DramaEpisodeBody): boolean {
  return ep?.origin === 'manual';
}

/** 正文是否达到可用长度 */
export function isSubstantialEpisodeBody(body: string | undefined | null): boolean {
  return episodeBodyCharLen(body) >= MIN_EPISODE_BODY_CHARS;
}

/** 本集创意是否达到可用长度 */
export function isSubstantialEpisodeCreative(creative: string | undefined | null): boolean {
  return episodeBodyCharLen(creative) >= MIN_EPISODE_CREATIVE_CHARS;
}

/** 是否已有至少一集可用正文 */
export function hasSubstantialEpisode(bodies: DramaEpisodeBody[]): boolean {
  return bodies.some((ep) => isSubstantialEpisodeBody(ep.body));
}

/** 自动流水线仍缺的集数（跳过手动空集） */
export function autoMissingEpisodeCount(bodies: DramaEpisodeBody[], target: number): number {
  const byNumber = new Map<number, DramaEpisodeBody>();
  bodies.forEach((ep) => {
    const num = ep.episodeNumber || 0;
    if (num >= 1) byNumber.set(num, ep);
  });
  const total = Math.max(target, 0);
  return Array.from({ length: total }, (_, i) => i + 1).reduce((missing, num) => {
    const ep = byNumber.get(num);
    if (!ep) return missing + 1;
    if (isSubstantialEpisodeBody(ep.body)) return missing;
    if (isManualEpisode(ep)) return missing;
    return missing + 1;
  }, 0);
}

/** 按目标集数铺满目录 */
export function buildOutlineDirectory(bodies: DramaEpisodeBody[], episodeCount: number): OutlineDirectoryEpisode[] {
  const byNumber = new Map<number, DramaEpisodeBody>();
  bodies.forEach((ep) => {
    byNumber.set(ep.episodeNumber || 0, ep);
  });
  const total = Math.max(episodeCount, bodies.length, 0);
  if (total <= 0) return [];
  return Array.from({ length: total }, (_, i) => {
    const episodeNumber = i + 1;
    const ep = byNumber.get(episodeNumber);
    return {
      episodeNumber,
      title: ep?.title || `第 ${episodeNumber} 集`,
      creative: ep?.creative,
      summary: ep?.summary,
      body: ep?.body,
      origin: ep?.origin,
    };
  });
}

/** 目录项与正文合并（保留 creative / summary / body） */
export function mergeDirectoryEpisodeBodies(
  directory: OutlineDirectoryEpisode[],
  bodies: DramaEpisodeBody[]
): DramaEpisodeBody[] {
  const byNumber = new Map(bodies.map((ep) => [ep.episodeNumber || 0, ep] as const));
  return directory.map((item) => {
    const found = byNumber.get(item.episodeNumber);
    return {
      episodeNumber: item.episodeNumber,
      title: found?.title || item.title || `第 ${item.episodeNumber} 集`,
      creative: found?.creative || item.creative || '',
      summary: found?.summary || item.summary || '',
      body: found?.body || item.body || '',
      origin: found?.origin || (item.origin as 'auto' | 'manual' | undefined),
    };
  });
}

/** 读取摘要状态（summaryStatus 为后端直接字段） */
export function getSummaryStatus(script: DramaScript | null | undefined): string {
  return script?.summaryStatus || (script?.summaryJson ? 'completed' : 'pending');
}

/** 读取分集剧本状态 */
export function getEpisodeContentStatus(script: DramaScript | null | undefined): string {
  return parseScriptParams(script).episode_content_status || 'pending';
}

/** 读取画面风格 ID */
export function getImageStyleId(script: DramaScript | null | undefined): string {
  return parseScriptParams(script).image_style_id || '';
}

/** 目标集数：项目 params → 摘要 episodeCount → 剧本 params episode_count → 默认 12（对齐后端） */
export function resolveEpisodeTarget(
  summary: Record<string, unknown> | null,
  script: DramaScript | null | undefined,
  project: DramaProject | null
): number {
  const candidates = [
    Number((parseJson<{ episode_count?: number }>(project?.paramsJson) || {}).episode_count || 0),
    Number((summary || {}).episodeCount || 0),
    Number(parseScriptParams(script).episode_count || 0),
  ];
  return candidates.find((value) => value >= 1) ?? 12;
}

/** 写回分集正文：后端统一存 {episodes:[...]} JSON 字符串 */
export function buildEpisodeContentUpdate(bodies: DramaEpisodeBody[]): string {
  return JSON.stringify({ episodes: bodies });
}
