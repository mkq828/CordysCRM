/**
 * 话术预置分类：下拉框永远有得选，同时分类支持自由输入（输入新词即创建新分类）。
 * 预置分类与后端已有分类（selectCategories 汇总）去重合并后作为下拉选项。
 */
export const DEFAULT_SCRIPT_CATEGORIES = ['开场白', '产品介绍', '价格异议', '竞品对比', '促单逼单', '常见异议'];

export function buildScriptCategoryOptions(existing: string[] = []): Array<{ label: string; value: string }> {
  const seen = new Set<string>();
  const options: Array<{ label: string; value: string }> = [];
  [...DEFAULT_SCRIPT_CATEGORIES, ...existing].forEach((category) => {
    const value = category?.trim();
    if (value && !seen.has(value)) {
      seen.add(value);
      options.push({ label: value, value });
    }
  });
  return options;
}
