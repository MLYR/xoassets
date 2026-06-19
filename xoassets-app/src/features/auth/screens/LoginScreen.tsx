import { Link } from 'expo-router';
import { Controller } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';

import { useLoginForm } from '@/features/auth';
import { Badge, Button, Card, CardContent, CardHeader, CardTitle, Input, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';

export function LoginScreen() {
  const theme = useTheme();
  const {
    control,
    onSubmit,
    formState: { errors, isSubmitting }
  } = useLoginForm();

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={[styles.page, { backgroundColor: theme.background }]}
    >
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <View style={styles.hero}>
          <View style={styles.brandRow}>
            <View style={[styles.logoMark, { backgroundColor: theme.card, borderColor: theme.border }]}> 
              <Text variant="subtitle">小〇</Text>
            </View>
            <Badge variant="outline">账户登录</Badge>
          </View>
          <Text variant="title" style={styles.heroTitle}>
            登录小〇财迹
          </Text>
          <Text variant="muted">用账户名和密码进入你的资产驾驶舱。</Text>
        </View>

        <Card>
          <CardHeader>
            <View style={styles.cardTitleRow}>
              <CardTitle>欢迎回来</CardTitle>
              <Badge variant="secondary">Password</Badge>
            </View>
            <Text variant="muted">不接入微信或第三方登录，仅使用账户密码。</Text>
          </CardHeader>
          <CardContent style={styles.form}>
            <Controller
              control={control}
              name="username"
              render={({ field: { onChange, onBlur, value } }) => (
                <Input
                  autoCapitalize="none"
                  autoCorrect={false}
                  label="账户名"
                  placeholder="请输入账户名"
                  textContentType="username"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.username?.message}
                />
              )}
            />
            <Controller
              control={control}
              name="password"
              render={({ field: { onChange, onBlur, value } }) => (
                <Input
                  label="密码"
                  placeholder="请输入密码"
                  secureTextEntry
                  textContentType="password"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.password?.message}
                />
              )}
            />
            {errors.root?.message ? (
              <View style={[styles.errorBox, { backgroundColor: `${theme.destructive}12` }]}> 
                <Text variant="error">{errors.root.message}</Text>
              </View>
            ) : null}
            <Button size="lg" loading={isSubmitting} onPress={onSubmit}>
              登录
            </Button>
            <View style={styles.footerAction}>
              <Text variant="caption">还没有账号？</Text>
              <Link href="/register" asChild>
                <Button variant="link">创建账号</Button>
              </Link>
            </View>
          </CardContent>
        </Card>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  page: {
    flex: 1
  },
  content: {
    flexGrow: 1,
    gap: 24,
    justifyContent: 'center',
    padding: 20
  },
  hero: {
    gap: 10
  },
  brandRow: {
    alignItems: 'center',
    flexDirection: 'row',
    gap: 10
  },
  logoMark: {
    alignItems: 'center',
    borderRadius: 14,
    borderWidth: 1,
    height: 44,
    justifyContent: 'center',
    width: 44
  },
  heroTitle: {
    fontSize: 30
  },
  cardTitleRow: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'space-between'
  },
  form: {
    gap: 14
  },
  errorBox: {
    borderRadius: 10,
    padding: 12
  },
  footerAction: {
    alignItems: 'center',
    flexDirection: 'row',
    justifyContent: 'center'
  }
});
