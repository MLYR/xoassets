import { Controller } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { Text } from '@/components/ui';
import { useRegisterForm } from '@/features/auth';
import { AuthScaffold, AuthTextField } from '../components/AuthScaffold';
import { useTheme } from '@/core/design/theme';

export function RegisterScreen() {
  const theme = useTheme();
  const {
    control,
    onSubmit,
    formState: { errors, isSubmitting }
  } = useRegisterForm();

  return (
    <AuthScaffold
      mode="register"
      footerText="已有账号？"
      footerAction="返回登录"
      footerHref="/(auth)/login"
      actionLabel="注册并进入"
      actionLoading={isSubmitting}
      onActionPress={onSubmit}
    >
      <View style={styles.formHeader}>
        <Text style={styles.formTitle}>创建账户</Text>
        <Text variant="muted" style={styles.formDesc}>注册成功后会自动登录，立即进入资产驾驶舱。</Text>
      </View>
      <Controller
        control={control}
        name="username"
        render={({ field: { onChange, onBlur, value } }) => (
          <AuthTextField
            autoCapitalize="none"
            label="账户名"
            placeholder="设置一个账户名"
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
          <AuthTextField
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
          <AuthTextField
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
      {errors.root?.message ? (
        <View style={[styles.errorBox, { backgroundColor: `${theme.destructive}12` }]}>
          <Text variant="error">{errors.root.message}</Text>
        </View>
      ) : null}
    </AuthScaffold>
  );
}

const styles = StyleSheet.create({
  formHeader: {
    gap: 4
  },
  formTitle: {
    fontSize: 20,
    fontWeight: '800'
  },
  formDesc: {
    fontSize: 14,
    lineHeight: 19
  },
  errorBox: {
    borderRadius: 12,
    padding: 12
  }
});
