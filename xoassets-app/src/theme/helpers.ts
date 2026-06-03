/* 主题读取工具：页面只读语义字段，避免直接散落颜色和图标路径。 */
import { getTheme } from './index'
import type { ThemeConfig, ThemeIcon, ThemeName } from './types'

export function getThemeColor(name: ThemeName | undefined, key: keyof ThemeConfig['colors']): string {
  return getTheme(name).colors[key]
}

export function getThemeGradient(name: ThemeName | undefined, key: keyof ThemeConfig['gradients']): string {
  return getTheme(name).gradients[key]
}

export function getThemeBackground(name: ThemeName | undefined, key: keyof ThemeConfig['backgrounds']): string {
  return getTheme(name).backgrounds[key]
}

export function getThemeIconText(icon: ThemeIcon | undefined, fallback = ''): string {
  if (!icon) return fallback
  if (icon.type === 'text') return icon.value
  return fallback
}

export function getMenuIcon(name: ThemeName | undefined, key: string): ThemeIcon | undefined {
  return getTheme(name).icons.menu[key]
}

export function getCategoryFallbackIcon(
  name: ThemeName | undefined,
  key: keyof ThemeConfig['icons']['categoryFallback']
): ThemeIcon {
  return getTheme(name).icons.categoryFallback[key]
}
