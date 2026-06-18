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
          <Badge variant="outline">小〇财迹</Badge>
          <Text variant="title">XOAssets</Text>
          <Text variant="muted">清晰记录资产、预算和投资变化。</Text>
        </View>

        <Card>
          <CardHeader>
            <CardTitle>登录</CardTitle>
            <Text variant="muted">继续查看你的财务复盘。</Text>
          </CardHeader>
          <CardContent style={styles.form}>
            <Controller
              control={control}
              name="identifier"
              render={({ field: { onChange, onBlur, value } }) => (
                <Input
                  autoCapitalize="none"
                  keyboardType="email-address"
                  label="手机号 / 邮箱"
                  placeholder="请输入手机号或邮箱"
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
                  placeholder="请输入密码"
                  secureTextEntry
                  value={value}
                  onBlur={onBlur}
                  onChangeText={onChange}
                  error={errors.password?.message}
                />
              )}
            />
            {errors.root?.message ? <Text variant="error">{errors.root.message}</Text> : null}
            <Button size="lg" loading={isSubmitting} onPress={onSubmit}>
              登录
            </Button>
            <Link href="/register" asChild>
              <Button variant="link">还没有账号？注册</Button>
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
    gap: 24,
    justifyContent: 'center',
    padding: 20
  },
  hero: {
    gap: 10
  },
  form: {
    gap: 14
  }
});
