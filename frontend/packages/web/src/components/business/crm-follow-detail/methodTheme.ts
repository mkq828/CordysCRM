/** 跟进方式 -> 配色（常见方式给语义色，其余按名称哈希稳定取色，保证同名同色） */
export interface FollowMethodTheme {
  solid: string;
  bg: string;
  border: string;
  text: string;
}

const METHOD_THEMES: { match: RegExp; theme: FollowMethodTheme }[] = [
  { match: /电话|来电|手机/, theme: { solid: '#1677ff', bg: '#eaf2ff', border: '#1677ff', text: '#1677ff' } },
  { match: /拜访|上门|面谈|见面/, theme: { solid: '#f97316', bg: '#fff1e8', border: '#f97316', text: '#e05a00' } },
  { match: /微信|企微|企业微信/, theme: { solid: '#16a34a', bg: '#e7f6ec', border: '#16a34a', text: '#12803a' } },
  { match: /邮件|邮箱|email/i, theme: { solid: '#7c3aed', bg: '#f1eafe', border: '#7c3aed', text: '#6b21c2' } },
  { match: /短信/, theme: { solid: '#0891b2', bg: '#e0f4f8', border: '#0891b2', text: '#0b7185' } },
  {
    match: /会议|线上|视频|语音|远程/,
    theme: { solid: '#4f46e5', bg: '#eae9fd', border: '#4f46e5', text: '#4238c7' },
  },
];

const METHOD_FALLBACKS: FollowMethodTheme[] = [
  { solid: '#64748b', bg: '#f1f5f9', border: '#64748b', text: '#475569' },
  { solid: '#db2777', bg: '#fdf0f5', border: '#db2777', text: '#be1d6d' },
  { solid: '#d97706', bg: '#fef4e6', border: '#d97706', text: '#b45309' },
  { solid: '#0d9488', bg: '#e6f6f5', border: '#0d9488', text: '#0f766e' },
];

export function methodThemeOf(method: string): FollowMethodTheme {
  const hit = METHOD_THEMES.find((c) => c.match.test(method));
  if (hit) {
    return hit.theme;
  }
  let hash = 0;
  for (let i = 0; i < method.length; i += 1) {
    hash = hash * 31 + method.charCodeAt(i);
  }
  return METHOD_FALLBACKS[Math.abs(hash) % METHOD_FALLBACKS.length];
}
