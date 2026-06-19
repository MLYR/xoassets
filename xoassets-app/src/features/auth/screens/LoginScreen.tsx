import { Controller } from 'react-hook-form';
import { StyleSheet, View } from 'react-native';

import { Text } from '@/components/ui';
import { useLoginForm } from '@/features/auth';
import { AuthScaffold, AuthTextField } from '../components/AuthScaffold';
import { useTheme } from '@/core/design/theme';

export function LoginScreen() {
  const theme = useTheme();
  const {
    control,
    onSubmit,
    formState: { errors, isSubmitting }
  } = useLoginForm();

  return (
    <AuthScaffold
      mode="login"
      footerText="还没有账号？"
      footerAction="创建账号"
      footerHref="/(auth)/register"
      actionLabel="登录"
      actionLoading={isSubmitting}
      onActionPress={onSubmit}
    >
      <View style={styles.formHeader}>
        <Text style={styles.formTitle}>欢迎回来</Text>
        <Text variant="muted" style={styles.formDesc}>仅使用账户密码登录，数据通过后端账号隔离。</Text>
      </View>
      <Controller
        control={control}
        name="username"
        render={({ field: { onChange, onBlur, value } }) => (
          <AuthTextField
            autoCapitalize="none"
            label="账户名"
            placeholder="请输入账户名"
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
            placeholder="请输入密码"
            secureTextEntry
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
