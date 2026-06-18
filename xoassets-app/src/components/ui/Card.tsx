import type { PropsWithChildren } from 'react';
import { Platform, StyleSheet, View, type ViewProps } from 'react-native';

import { useTheme } from '@/styles/theme';
import { Text } from './Text';

export function Card({ children, style, ...props }: PropsWithChildren<ViewProps>) {
  const theme = useTheme();
  const styles = createStyles(theme);

  return (
    <View style={[styles.card, style]} {...props}>
      {children}
    </View>
  );
}

export function CardHeader({ children, style, ...props }: PropsWithChildren<ViewProps>) {
  return (
    <View style={[styles.header, style]} {...props}>
      {children}
    </View>
  );
}

export function CardTitle({ children }: PropsWithChildren) {
  return <Text variant="subtitle">{children}</Text>;
}

export function CardDescription({ children }: PropsWithChildren) {
  return <Text variant="muted">{children}</Text>;
}

export function CardContent({ children, style, ...props }: PropsWithChildren<ViewProps>) {
  return (
    <View style={[styles.content, style]} {...props}>
      {children}
    </View>
  );
}

export function CardFooter({ children, style, ...props }: PropsWithChildren<ViewProps>) {
  return (
    <View style={[styles.footer, style]} {...props}>
      {children}
    </View>
  );
}

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  StyleSheet.create({
    card: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderRadius: theme.tokens.radius.lg,
      borderWidth: 1,
      ...(Platform.OS === 'web'
        ? { boxShadow: `0 8px 18px ${theme.shadow}10` }
        : {
            shadowColor: theme.shadow,
            shadowOffset: { width: 0, height: 8 },
            shadowOpacity: 0.06,
            shadowRadius: 18
          })
    }
  });

const styles = StyleSheet.create({
  header: {
    gap: 4,
    padding: 16,
    paddingBottom: 8
  },
  content: {
    padding: 16
  },
  footer: {
    padding: 16,
    paddingTop: 8
  }
});
