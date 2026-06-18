import type { PropsWithChildren } from 'react';
import { Text as NativeText, StyleSheet, type TextProps as NativeTextProps } from 'react-native';

import { useTheme } from '@/styles/theme';

type TextVariant = 'title' | 'subtitle' | 'body' | 'muted' | 'caption' | 'error';

interface TextProps extends NativeTextProps {
  variant?: TextVariant;
}

export function Text({ children, variant = 'body', style, ...props }: PropsWithChildren<TextProps>) {
  const theme = useTheme();
  const styles = createStyles(theme);

  return (
    <NativeText style={[styles.base, styles[variant], style]} {...props}>
      {children}
    </NativeText>
  );
}

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  StyleSheet.create({
    base: {
      color: theme.foreground,
      letterSpacing: 0
    },
    title: {
      fontSize: theme.tokens.fontSize.title,
      fontWeight: theme.tokens.fontWeight.bold
    },
    subtitle: {
      fontSize: theme.tokens.fontSize.subtitle,
      fontWeight: theme.tokens.fontWeight.semibold
    },
    body: {
      fontSize: theme.tokens.fontSize.body,
      fontWeight: theme.tokens.fontWeight.regular
    },
    muted: {
      color: theme.mutedForeground,
      fontSize: theme.tokens.fontSize.body
    },
    caption: {
      color: theme.mutedForeground,
      fontSize: theme.tokens.fontSize.caption
    },
    error: {
      color: theme.destructive,
      fontSize: theme.tokens.fontSize.caption
    }
  });
