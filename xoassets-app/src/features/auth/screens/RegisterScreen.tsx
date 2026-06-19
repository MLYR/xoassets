import { Link } from 'expo-router';
import { Controller } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, View } from 'react-native';

import { useRegisterForm } from '@/features/auth';
import { Badge, Button, Card, CardContent, CardHeader, CardTitle, Input, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';

export function RegisterScreen() {
  const theme = useTheme();
  const {
    control,
    onSubmit,
    formState: { errors, isSubmitting }
  } = useRegisterForm();

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
            <Badge variant="outline">创建账户</Badge>
          </View>
          <Text variant="title" style={styles.heroTitle}>
            开始记录你的资产
          </Text>
          <Text variant="muted">只需要账户名和密码，先建立一套干净的个人账本。</Text>
        </View>

        <Card>
          <CardHeader>
            <View style={styles.cardTitleRow}>
              <CardTitle>注册</CardTitle>
              <Badge variant="secondary">Account</Badge>
            </View>
            <Text variant="muted">暂不接入微信、短信或其他第三方方式。</Text>
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
                  placeholder="设置一个账户名"
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
                  placeholder="至少 6 位"
                  secureTextEntry
                  textContentType="newPassword"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.password?.message}
                />
              )}
            />
            <Controller
              control={control}
              name="confirmPassword"
              render={({ field: { onChange, onBlur, value } }) => (
                <Input
                  label="确认密码"
                  placeholder="再次输入密码"
                  secureTextEntry
                  textContentType="newPassword"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.confirmPassword?.message}
                />
              )}
            />
            {errors.root?.message ? (
              <View style={[styles.errorBox, { backgroundColor: `${theme.destructive}12` }]}> 
                <Text variant="error">{errors.root.message}</Text>
              </View>
            ) : null}
            <Button size="lg" loading={isSubmitting} onPress={onSubmit}>
              注册并进入
            </Button>
            <View style={styles.footerAction}>
              <Text variant="caption">已有账号？</Text>
              <Link href="/login" asChild>
                <Button variant="link">返回登录</Button>
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
