// 读取当前 CSS 主题变量，供 ECharts 这类 JS 配置同步明暗色。
export function readThemeVar(name: string, fallback: string) {
  if (typeof window === 'undefined') {
    return fallback;
  }
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || fallback;
}
