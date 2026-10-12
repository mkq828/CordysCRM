// AI 漫剧画面风格库（1:1 移植 printfilm 的 21 款风格）。
// 前端以本静态目录为唯一真相源：id 与 printfilm imageStyles 对齐，生成时原样传给后端；
// 预览图走 public/image-styles 下的 jpg/png/svg 多级回退。
export const IMAGE_STYLE_IDS = [
  'retro-sci-fi-atompunk',
  'palace-intrigue-cold',
  'domestic-suspense-cold',
  'ancient-romance-soft',
  'ancient-chinese-mythology',
  'japanese-youth-film',
  'japanese-daily-natural',
  'korean-urban-soft',
  'chinese-urban-realistic',
  'wuxia-realistic-photo',
  '90s-realistic-film',
  'retro-narrative-film',
  'american-retro-hollywood',
  'neon-cyberpunk-film',
  '90s-rural-china-film',
  'cgi-3d-animation',
  'ghibli-handdrawn-anime',
  'tezuka-era-cartoon',
  'shanghai-animation',
  'pixel-art',
  'shadow-puppet-illustration',
] as const;

export type ImageStyleId = (typeof IMAGE_STYLE_IDS)[number];

export interface DramaImageStyle {
  id: ImageStyleId;
  label: string;
}

export const IMAGE_STYLE_OPTIONS: DramaImageStyle[] = [
  { id: 'retro-sci-fi-atompunk', label: '复古科幻原子朋克' },
  { id: 'palace-intrigue-cold', label: '宫斗权谋冷峻' },
  { id: 'domestic-suspense-cold', label: '国产悬疑冷调' },
  { id: 'ancient-romance-soft', label: '古偶唯美柔光' },
  { id: 'ancient-chinese-mythology', label: '中国古代神话史诗' },
  { id: 'japanese-youth-film', label: '日式青春胶片' },
  { id: 'japanese-daily-natural', label: '日式生活自然' },
  { id: 'korean-urban-soft', label: '韩剧都市柔光' },
  { id: 'chinese-urban-realistic', label: '国产都市写实' },
  { id: 'wuxia-realistic-photo', label: '武侠江湖写实摄影' },
  { id: '90s-realistic-film', label: '90年代写实电影' },
  { id: 'retro-narrative-film', label: '复古叙事电影' },
  { id: 'american-retro-hollywood', label: '美式复古好莱坞' },
  { id: 'neon-cyberpunk-film', label: '霓虹赛博电影' },
  { id: '90s-rural-china-film', label: '90年代中国农村电影' },
  { id: 'cgi-3d-animation', label: '3D 动画' },
  { id: 'ghibli-handdrawn-anime', label: '宫崎骏气质手绘' },
  { id: 'tezuka-era-cartoon', label: '手冢治虫时代卡通画风' },
  { id: 'shanghai-animation', label: '上美画风' },
  { id: 'pixel-art', label: '像素风' },
  { id: 'shadow-puppet-illustration', label: '皮影戏插画' },
];

export const EPISODE_COUNT_PRESETS = [1, 12, 24, 36, 48] as const;

export function getImageStyleLabel(styleId: string | undefined | null): string | null {
  if (!styleId) return null;
  return IMAGE_STYLE_OPTIONS.find((o) => o.id === styleId)?.label ?? null;
}

/** 风格预览图候选 URL（public jpg/png → svg 占位） */
export function getDramaImageStylePreviewCandidates(styleId: string): string[] {
  return [
    `${import.meta.env.BASE_URL}image-styles/${styleId}.jpg`,
    `${import.meta.env.BASE_URL}image-styles/${styleId}.png`,
    `${import.meta.env.BASE_URL}image-styles/${styleId}.svg`,
  ];
}

export function getDramaImageStylePreviewUrl(styleId: string): string {
  return getDramaImageStylePreviewCandidates(styleId)[0];
}
