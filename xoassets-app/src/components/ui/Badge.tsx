import type { PropsWithChildren } from 'react';
import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/styles/theme';
import { Text } from './Text';

type BadgeVariant = 'default' | 'secondary' | 'outline' | 'destructive' | 'success' | 'warning';

interface BadgeProps {
  variant?: BadgeVariant;
}

export function Badge({ children, variant = 'default' }: PropsWithChildren<BadgeProps>) {
  const theme = useTheme();
  const variantStyle = getVariantStyle(theme, variant);

  return (
    <View style={[styles.badge, variantStyle.container]}>
      <Text variant="caption" style={variantStyle.text}>
        {children}
      </Text>
    </View>
  );
}

function getVariantStyle(theme: ReturnType<typeof useTheme>, variant: BadgeVariant) {
  const base = {
    container: { backgroundColor: theme.primary, borderColor: theme.primary },
    text: { color: theme.primaryForeground }
  };
  const variants = {
    default: base,
    secondary: {
      container: { backgroundColor: theme.secondary, borderColor: theme.secondary },
      text: { color: theme.secondaryForeground }
    },
    outline: {
      container: { backgroundColor: 'transparent', borderColor: theme.border },
      text: { color: theme.foreground }
    },
    destructive: {
      container: { backgroundColor: theme.destructive, borderColor: theme.destructive },
      text: { color: theme.destructiveForeground }
    },
    success: {
      container: { backgroundColor: theme.success, borderColor: theme.success },
      text: { color: theme.primaryForeground }
    },
    warning: {
      container: { backgroundColor: theme.warning, borderColor: theme.warning },
      text: { color: theme.primaryForeground }
    }
  };
  return variants[variant];
}

const styles = StyleSheet.create({
  badge: {
    alignSelf: 'flex-start',
    borderRadius: 999,
    borderWidth: 1,
    paddingHorizontal: 10,
    paddingVertical: 4
  }
});
