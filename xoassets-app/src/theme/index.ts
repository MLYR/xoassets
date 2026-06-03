/* 主题入口：集中注册所有可用主题。 */
import type { ThemeConfig, ThemeName } from './types'
import { classicBlueTheme } from './themes/classic-blue'
import { techDarkTheme } from './themes/tech-dark'
import { cartoonSoftTheme } from './themes/cartoon-soft'

export type { ThemeConfig, ThemeIcon, ThemeIconPair, ThemeName } from './types'
export { themeIconMap } from './icon-map'

export const DEFAULT_THEME_NAME: ThemeName = 'classic-blue'

export const themes: Record<ThemeName, ThemeConfig> = {
  'classic-blue': classicBlueTheme,
  'tech-dark': techDarkTheme,
  'cartoon-soft': cartoonSoftTheme
}

export const themeOptions = Object.values(themes).map((theme) => ({
  name: theme.name,
  label: theme.label
}))

export function getTheme(name?: string | null): ThemeConfig {
  return themes[(name as ThemeName) || DEFAULT_THEME_NAME] || themes[DEFAULT_THEME_NAME]
}
