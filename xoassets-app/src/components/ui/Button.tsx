import type { PropsWithChildren } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, type PressableProps } from 'react-native';

import { useTheme } from '@/styles/theme';
import { Text } from './Text';

type ButtonVariant = 'default' | 'secondary' | 'outline' | 'ghost' | 'destructive' | 'link';
type ButtonSize = 'sm' | 'md' | 'lg';

interface ButtonProps extends PressableProps {
  variant?: ButtonVariant;
  size?: ButtonSize;
  loading?: boolean;
}

export function Button({
  children,
  variant = 'default',
  size = 'md',
  loading = false,
  disabled,
  style,
  ...props
}: PropsWithChildren<ButtonProps>) {
  const theme = useTheme();
  const variantStyle = getVariantStyle(theme, variant);
  const sizeStyle = getSizeStyle(theme, size);
  const isDisabled = disabled || loading;

  return (
    <Pressable
      accessibilityRole="button"
      disabled={isDisabled}
      style={({ pressed }) => [
        styles.base,
        sizeStyle.container,
        variantStyle.container,
        pressed && !isDisabled ? styles.pressed : null,
        isDisabled ? styles.disabled : null,
        typeof style === 'function' ? style({ pressed }) : style
      ]}
      {...props}
    >
      {loading ? <ActivityIndicator color={variantStyle.text.color} /> : null}
      <Text variant="body" style={[styles.label, sizeStyle.text, variantStyle.text]}>
        {children}
      </Text>
    </Pressable>
  );
}

function getVariantStyle(theme: ReturnType<typeof useTheme>, variant: ButtonVariant) {
  const variants = {
    default: {
      container: { backgroundColor: theme.primary, borderColor: theme.primary },
      text: { color: theme.primaryForeground }
    },
    secondary: {
      container: { backgroundColor: theme.secondary, borderColor: theme.secondary },
      text: { color: theme.secondaryForeground }
    },
    outline: {
      container: { backgroundColor: 'transparent', borderColor: theme.border },
      text: { color: theme.foreground }
    },
    ghost: {
      container: { backgroundColor: 'transparent', borderColor: 'transparent' },
      text: { color: theme.foreground }
    },
    destructive: {
      container: { backgroundColor: theme.destructive, borderColor: theme.destructive },
      text: { color: theme.destructiveForeground }
    },
    link: {
      container: { backgroundColor: 'transparent', borderColor: 'transparent' },
      text: { color: theme.info }
    }
  };
  return variants[variant];
}

function getSizeStyle(theme: ReturnType<typeof useTheme>, size: ButtonSize) {
  const sizes = {
    sm: {
      container: { minHeight: 36, paddingHorizontal: theme.tokens.spacing.md },
      text: { fontSize: theme.tokens.fontSize.caption }
    },
    md: {
      container: { minHeight: 44, paddingHorizontal: theme.tokens.spacing.lg },
      text: { fontSize: theme.tokens.fontSize.body }
    },
    lg: {
      container: { minHeight: 52, paddingHorizontal: theme.tokens.spacing.xl },
      text: { fontSize: theme.tokens.fontSize.subtitle }
    }
  };
  return sizes[size];
}

const styles = StyleSheet.create({
  base: {
    alignItems: 'center',
    borderRadius: 8,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 8,
    justifyContent: 'center'
  },
  label: {
    fontWeight: '600'
  },
  pressed: {
    opacity: 0.78,
    transform: [{ scale: 0.99 }]
  },
  disabled: {
    opacity: 0.5
  }
});
