import { Link } from 'expo-router';
import { useRef, type PropsWithChildren } from 'react';
import { ActivityIndicator, Animated, KeyboardAvoidingView, Platform, Pressable, ScrollView, StyleSheet, TextInput, View } from 'react-native';

import { Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { AuthLogo } from './AuthLogo';

type AuthMode = 'login' | 'register';

interface AuthScaffoldProps {
  mode: AuthMode;
  footerText: string;
  footerAction: string;
  footerHref: '/(auth)/login' | '/(auth)/register';
  actionLabel: string;
  actionLoading?: boolean;
  onActionPress: () => void;
}

export function AuthScaffold({
  mode,
  footerText,
  footerAction,
  footerHref,
  actionLabel,
  actionLoading,
  onActionPress,
  children
}: PropsWithChildren<AuthScaffoldProps>) {
  const theme = useTheme();
  const isDark = theme.background === '#09090b';
  const colors = getAuthColors(isDark);

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[styles.page, { backgroundColor: colors.background }]}
    >
      <AuthMarketBackdrop isDark={isDark} />
      <ScrollView
        contentContainerStyle={[styles.content, mode === 'register' ? styles.registerContent : null]}
        keyboardShouldPersistTaps="handled"
        showsVerticalScrollIndicator={false}
      >
        <View style={styles.hero}>
          <View style={styles.logoShadow}>
            <AuthLogo size={58} />
          </View>
          <Text style={[styles.brand, { color: colors.foreground }]}>小〇财迹</Text>
        </View>

        <View
          style={[
            styles.card,
            {
              backgroundColor: colors.card,
              borderColor: colors.cardBorder,
              shadowColor: colors.shadow
            }
          ]}
        >
          {children}
        </View>

        <AuthPrimaryButton loading={actionLoading} onPress={onActionPress}>
          {actionLabel}
        </AuthPrimaryButton>

        <View style={styles.footerRow}>
          <Text style={[styles.footerText, { color: colors.muted }]}>{footerText}</Text>
          <Link href={footerHref} asChild>
            <Pressable hitSlop={10}>
              <Text style={[styles.footerAction, { color: colors.foreground }]}>{footerAction}</Text>
            </Pressable>
          </Link>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

interface AuthTextFieldProps {
  label: string;
  placeholder: string;
  value?: string;
  error?: string;
  secureTextEntry?: boolean;
  keyboardType?: 'default' | 'email-address';
  autoCapitalize?: 'none' | 'sentences' | 'words' | 'characters';
  onBlur?: () => void;
  onChangeText?: (value: string) => void;
}

export function AuthTextField({ label, error, secureTextEntry, ...props }: AuthTextFieldProps) {
  const theme = useTheme();
  const isDark = theme.background === '#09090b';
  const colors = getAuthColors(isDark);

  return (
    <View style={styles.fieldGroup}>
      <Text style={[styles.label, { color: colors.foreground }]}>{label}</Text>
      <View style={[styles.inputShell, { backgroundColor: colors.input, borderColor: error ? colors.error : colors.inputBorder }]}>
        <View style={styles.inputInner}>
          <AuthNativeInput colors={colors} secureTextEntry={secureTextEntry} {...props} />
          {secureTextEntry ? <Text style={[styles.eyeIcon, { color: colors.foreground }]}>⊙</Text> : null}
        </View>
      </View>
      {error ? <Text style={[styles.errorText, { color: colors.error }]}>{error}</Text> : null}
    </View>
  );
}

function AuthNativeInput({ colors, ...props }: Omit<AuthTextFieldProps, 'label' | 'error'> & { colors: ReturnType<typeof getAuthColors> }) {
  return (
    <TextInput
      placeholderTextColor={colors.placeholder}
      selectionColor={colors.foreground}
      style={[styles.input, { color: colors.foreground }]}
      {...props}
    />
  );
}

export function AuthCheckbox({ checked, label, rightText, onPress }: { checked: boolean; label: string; rightText?: string; onPress: () => void }) {
  const theme = useTheme();
  const isDark = theme.background === '#09090b';
  const colors = getAuthColors(isDark);

  return (
    <View style={styles.optionRow}>
      <Pressable accessibilityRole="checkbox" accessibilityState={{ checked }} onPress={onPress} style={styles.checkWrap} hitSlop={8}>
        <View style={[styles.checkbox, { borderColor: colors.checkboxBorder, backgroundColor: checked ? colors.foreground : 'transparent' }]}>
          {checked ? <Text style={[styles.checkMark, { color: colors.background }]}>✓</Text> : null}
        </View>
        <Text style={[styles.optionText, { color: colors.muted }]}>{label}</Text>
      </Pressable>
      {rightText ? <Text style={[styles.optionText, { color: colors.muted }]}>{rightText}</Text> : null}
    </View>
  );
}

export function AuthPrimaryButton({ children, loading, onPress }: PropsWithChildren<{ loading?: boolean; onPress: () => void }>) {
  const theme = useTheme();
  const isDark = theme.background === '#09090b';
  const colors = getAuthColors(isDark);
  const pressProgress = useRef(new Animated.Value(1)).current;

  const animatePress = (toValue: number) => {
    // Android 上 Pressable 的 pressed style 不够稳定，用 Animated 固定提供可感知的点击反馈。
    Animated.spring(pressProgress, {
      toValue,
      friction: 7,
      tension: 130,
      useNativeDriver: true
    }).start();
  };

  return (
    <Animated.View
      style={[
        styles.primaryButtonShell,
        {
          backgroundColor: colors.primaryButton,
          borderColor: colors.primaryButton,
          shadowColor: colors.shadow,
          opacity: pressProgress.interpolate({ inputRange: [0.97, 1], outputRange: [0.86, 1] }),
          transform: [{ scale: pressProgress }]
        },
        loading ? styles.disabled : null
      ]}
    >
      <Pressable
        accessibilityRole="button"
        disabled={loading}
        onPress={onPress}
        onPressIn={() => animatePress(0.97)}
        onPressOut={() => animatePress(1)}
        style={[StyleSheet.absoluteFill, styles.primaryButtonHitArea]}
      >
        {loading ? <ActivityIndicator color={colors.primaryButtonText} /> : null}
        <Text style={[styles.primaryButtonText, { color: colors.primaryButtonText }]}>{children}</Text>
      </Pressable>
    </Animated.View>
  );
}

export function AuthDivider() {
  const theme = useTheme();
  const isDark = theme.background === '#09090b';
  const colors = getAuthColors(isDark);

  return (
    <View style={styles.dividerRow}>
      <View style={[styles.dividerLine, { backgroundColor: colors.divider }]} />
      <Text style={[styles.dividerText, { color: colors.muted }]}>或</Text>
      <View style={[styles.dividerLine, { backgroundColor: colors.divider }]} />
    </View>
  );
}

export function AuthGoogleButton({ children }: PropsWithChildren) {
  const theme = useTheme();
  const isDark = theme.background === '#09090b';
  const colors = getAuthColors(isDark);

  return (
    <Pressable
      accessibilityRole="button"
      style={({ pressed }) => [
        styles.googleButton,
        { borderColor: colors.googleBorder, backgroundColor: colors.googleBackground },
        pressed ? styles.pressed : null
      ]}
    >
      <Text style={[styles.googleIcon, { color: colors.foreground }]}>G</Text>
      <Text style={[styles.googleText, { color: colors.foreground }]}>{children}</Text>
    </Pressable>
  );
}

export function AuthInlineTerms({ checked, onPress }: { checked: boolean; onPress: () => void }) {
  const theme = useTheme();
  const isDark = theme.background === '#09090b';
  const colors = getAuthColors(isDark);

  return (
    <Pressable accessibilityRole="checkbox" accessibilityState={{ checked }} onPress={onPress} style={styles.termsRow} hitSlop={8}>
      <View style={[styles.checkbox, { borderColor: colors.checkboxBorder, backgroundColor: checked ? colors.foreground : 'transparent' }]}>
        {checked ? <Text style={[styles.checkMark, { color: colors.background }]}>✓</Text> : null}
      </View>
      <Text style={[styles.optionText, { color: colors.muted }]}>
        我已阅读并同意 <Text style={[styles.linkText, { color: colors.foreground }]}>服务条款</Text> 与{' '}
        <Text style={[styles.linkText, { color: colors.foreground }]}>隐私政策</Text>
      </Text>
    </Pressable>
  );
}

function AuthMarketBackdrop({ isDark }: { isDark: boolean }) {
  const colors = getAuthColors(isDark);
  const gridLines = Array.from({ length: 12 });
  const verticalLines = Array.from({ length: 9 });

  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      {gridLines.map((_, index) => (
        <View key={`h-${index}`} style={[styles.gridH, { top: 90 + index * 48, borderColor: colors.grid }]} />
      ))}
      {verticalLines.map((_, index) => (
        <View key={`v-${index}`} style={[styles.gridV, { left: 34 + index * 52, borderColor: colors.grid }]} />
      ))}
    </View>
  );
}

function getAuthColors(isDark: boolean) {
  return isDark
    ? {
        background: '#050506',
        foreground: '#f7f7f7',
        muted: '#a7a7ad',
        placeholder: '#77777f',
        card: 'rgba(22, 22, 24, 0.86)',
        cardBorder: '#45454c',
        input: 'rgba(9, 9, 11, 0.52)',
        inputBorder: '#4a4a52',
        primaryButton: '#f7f7f7',
        primaryButtonText: '#050506',
        googleBackground: 'transparent',
        googleBorder: '#5d5d66',
        divider: '#3b3b42',
        checkboxBorder: '#707078',
        grid: 'rgba(255,255,255,0.055)',
        shadow: '#000000',
        error: '#ff6b6b'
      }
    : {
        background: '#fbfbfb',
        foreground: '#050505',
        muted: '#606068',
        placeholder: '#b0b0b7',
        card: 'rgba(255, 255, 255, 0.92)',
        cardBorder: '#dedee2',
        input: 'rgba(255, 255, 255, 0.92)',
        inputBorder: '#d9d9de',
        primaryButton: '#050505',
        primaryButtonText: '#ffffff',
        googleBackground: 'transparent',
        googleBorder: '#9f9fa6',
        divider: '#dfdfe3',
        checkboxBorder: '#9a9aa2',
        grid: 'rgba(0,0,0,0.055)',
        shadow: '#000000',
        error: '#dc2626'
      };
}

const styles = StyleSheet.create({
  page: {
    flex: 1
  },
  content: {
    flexGrow: 1,
    justifyContent: 'center',
    paddingBottom: 18,
    paddingHorizontal: 26,
    paddingTop: 18
  },
  registerContent: {
    paddingTop: 18
  },
  hero: {
    alignItems: 'center',
    marginBottom: 12
  },
  logoShadow: {
    borderRadius: 18,
    elevation: 10,
    marginBottom: 8,
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.22,
    shadowRadius: 18
  },
  brand: {
    fontSize: 22,
    fontWeight: '800',
    letterSpacing: 1.2,
    textShadowColor: 'rgba(0,0,0,0.18)',
    textShadowOffset: { width: 0, height: 3 },
    textShadowRadius: 6
  },
  card: {
    borderRadius: 22,
    borderWidth: 1,
    elevation: 12,
    gap: 12,
    paddingHorizontal: 18,
    paddingVertical: 18,
    shadowOffset: { width: 0, height: 14 },
    shadowOpacity: 0.12,
    shadowRadius: 24
  },
  fieldGroup: {
    gap: 6
  },
  label: {
    fontSize: 15,
    fontWeight: '800',
    letterSpacing: 0.3
  },
  inputShell: {
    borderRadius: 12,
    borderWidth: 1,
    minHeight: 50,
    paddingHorizontal: 14
  },
  inputInner: {
    alignItems: 'center',
    flexDirection: 'row',
    minHeight: 48
  },
  input: {
    flex: 1,
    fontSize: 15,
    fontWeight: '500',
    minHeight: 48
  },
  eyeIcon: {
    fontSize: 22,
    fontWeight: '700',
    marginLeft: 12
  },
  errorText: {
    fontSize: 12,
    fontWeight: '600'
  },
  optionRow: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  checkWrap: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 10
  },
  termsRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 10
  },
  checkbox: {
    alignItems: 'center',
    borderRadius: 6,
    borderWidth: 1.5,
    height: 24,
    justifyContent: 'center',
    width: 24
  },
  checkMark: {
    fontSize: 15,
    fontWeight: '900'
  },
  optionText: {
    flexShrink: 1,
    fontSize: 15,
    fontWeight: '500'
  },
  linkText: {
    fontWeight: '800',
    textDecorationLine: 'underline'
  },
  primaryButtonShell: {
    alignSelf: 'stretch',
    borderRadius: 14,
    borderWidth: 1,
    elevation: 8,
    height: 56,
    marginTop: 14,
    overflow: 'hidden',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.18,
    shadowRadius: 18,
    width: '100%'
  },
  primaryButtonHitArea: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 8,
    justifyContent: 'center',
    paddingTop: 1
  },
  primaryButtonText: {
    fontSize: 18,
    fontWeight: '900',
    includeFontPadding: false,
    letterSpacing: 1,
    lineHeight: 22,
    textAlign: 'center',
    textAlignVertical: 'center'
  },
  dividerRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 16
  },
  dividerLine: {
    flex: 1,
    height: StyleSheet.hairlineWidth
  },
  dividerText: {
    fontSize: 16,
    fontWeight: '600'
  },
  googleButton: {
    alignItems: 'center',
    borderRadius: 12,
    borderWidth: 1,
    flexDirection: 'row',
    gap: 16,
    justifyContent: 'center',
    minHeight: 58
  },
  googleIcon: {
    fontSize: 24,
    fontWeight: '900'
  },
  googleText: {
    fontSize: 17,
    fontWeight: '700',
    letterSpacing: 0.4
  },
  footerRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 10,
    justifyContent: 'center',
    marginTop: 16
  },
  footerText: {
    fontSize: 17,
    fontWeight: '500'
  },
  footerAction: {
    fontSize: 17,
    fontWeight: '900'
  },
  pressed: {
    opacity: 0.76
  },
  disabled: {
    opacity: 0.5
  },
  gridH: {
    borderStyle: 'dashed',
    borderTopWidth: 1,
    left: 0,
    position: 'absolute',
    right: 0
  },
  gridV: {
    borderLeftWidth: 1,
    borderStyle: 'dashed',
    bottom: 0,
    position: 'absolute',
    top: 70
  }
});
