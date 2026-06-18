import { Link } from 'expo-router';
import { Controller } from 'react-hook-form';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet } from 'react-native';

import { useRegisterForm } from '@/features/auth';
import { Button, Card, CardContent, CardHeader, CardTitle, Input, Text } from '@/components/ui';
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
        <Card>
          <CardHeader>
            <CardTitle>创建账号</CardTitle>
            <Text variant="muted">用一套干净的账本开始记录。</Text>
          </CardHeader>
          <CardContent style={styles.form}>
            <Controller
              control={control}
              name="nickname"
              render={({ field: { onChange, onBlur, value } }) => (
                <Input
                  label="昵称"
                  placeholder="请输入昵称"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.nickname?.message}
                />
              )}
            />
            <Controller
              control={control}
              name="identifier"
              render={({ field: { onChange, onBlur, value } }) => (
                <Input
                  autoCapitalize="none"
                  keyboardType="email-address"
                  label="邮箱 / 手机号"
                  placeholder="请输入邮箱或手机号"
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.identifier?.message}
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
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.confirmPassword?.message}
                />
              )}
            />
            {errors.root?.message ? <Text variant="error">{errors.root.message}</Text> : null}
            <Button size="lg" loading={isSubmitting} onPress={onSubmit}>
              注册
            </Button>
            <Link href="/login" asChild>
              <Button variant="link">已有账号？返回登录</Button>
            </Link>
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
    justifyContent: 'center',
    padding: 20
  },
  form: {
    gap: 14
  }
});
