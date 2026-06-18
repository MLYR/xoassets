export const tokens = {
  colorKeys: [
    'background',
    'foreground',
    'card',
    'cardForeground',
    'popover',
    'popoverForeground',
    'primary',
    'primaryForeground',
    'secondary',
    'secondaryForeground',
    'muted',
    'mutedForeground',
    'accent',
    'accentForeground',
    'destructive',
    'destructiveForeground',
    'border',
    'input',
    'ring'
  ],
  radius: {
    sm: 6,
    md: 8,
    lg: 12,
    xl: 16,
    full: 999
  },
  spacing: {
    xs: 4,
    sm: 8,
    md: 12,
    lg: 16,
    xl: 24,
    xxl: 32
  },
  fontSize: {
    caption: 12,
    body: 14,
    subtitle: 16,
    title: 24,
    display: 32
  },
  fontWeight: {
    regular: '400',
    medium: '500',
    semibold: '600',
    bold: '700'
  }
} as const;

export type XoTokens = typeof tokens;
export type XoThemeColorKey = (typeof tokens.colorKeys)[number];
