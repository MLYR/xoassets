import { useMemo } from 'react';
import { ActivityIndicator, Pressable, StyleSheet, Text as RNText, View } from 'react-native';

import { useTheme } from '@/core/design/theme';

export function SubmitActionButton({
  label,
  loading,
  pressed,
  onPressedChange,
  onPress
}: {
  label: string;
  loading: boolean;
  pressed: boolean;
  onPressedChange: (value: boolean) => void;
  onPress: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <View
      style={[
        styles.button,
        loading ? styles.buttonDisabled : null,
        pressed && !loading ? styles.buttonPressed : null
      ]}
    >
      <Pressable
        accessibilityRole="button"
        disabled={loading}
        style={StyleSheet.absoluteFill}
        onPressIn={() => onPressedChange(true)}
        onPressOut={() => onPressedChange(false)}
        onPress={onPress}
      />
      <View style={styles.content}>
        {loading ? <ActivityIndicator color="#ffffff" /> : null}
        <RNText style={styles.text}>{label}</RNText>
      </View>
    </View>
  );
}

const createStyles = (theme: ReturnType<typeof useTheme>) => {
  const isDark = theme.background === '#09090b';
  const submitButtonBackground = isDark ? '#0f0f10' : '#ffffff';
  const submitButtonText = isDark ? '#ffffff' : '#111111';
  const submitButtonBorder = isDark ? '#2a2a2d' : '#e4e4e7';
  const submitButtonGlow = isDark
    ? '0 0 0 1px rgba(255,255,255,0.08), 0 0 24px rgba(96,165,250,0.16), 0 10px 24px rgba(0,0,0,0.38)'
    : '0 0 0 1px rgba(0,0,0,0.06), 0 0 24px rgba(37,99,235,0.12), 0 10px 24px rgba(37,99,235,0.08)';

  return StyleSheet.create({
    button: {
      alignItems: 'center',
      backgroundColor: submitButtonBackground,
      borderColor: submitButtonBorder,
      borderRadius: 14,
      borderCurve: 'continuous',
      borderWidth: 1,
      flexDirection: 'row',
      gap: 8,
      justifyContent: 'center',
      minHeight: 54,
      paddingHorizontal: 16,
      paddingVertical: 14,
      boxShadow: submitButtonGlow,
      shadowColor: isDark ? '#ffffff' : '#2563eb',
      shadowOffset: { width: 0, height: 6 },
      shadowOpacity: isDark ? 0.18 : 0.14,
      shadowRadius: 12,
      width: '100%',
      elevation: 6
    },
    content: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 8,
      justifyContent: 'center'
    },
    buttonPressed: {
      opacity: 0.9,
      transform: [{ scale: 0.985 }]
    },
    buttonDisabled: {
      opacity: 0.55
    },
    text: {
      color: submitButtonText,
      fontSize: 16,
      fontWeight: '700',
      includeFontPadding: false,
      lineHeight: 18,
      textAlign: 'center'
    }
  });
};
