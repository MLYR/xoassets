import { useMutation } from '@tanstack/react-query';
import { Redirect, router } from 'expo-router';
import { ChevronRight, Settings, Target, UserRound, WalletCards, X } from 'lucide-react-native';
import { useEffect, useMemo, useState } from 'react';
import { ActivityIndicator, KeyboardAvoidingView, Modal, Platform, Pressable, ScrollView, StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Button, Card, CardContent, Input, Separator, Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';
import { authApi } from '@/features/auth';
import { useAuthStore } from '@/stores/authStore';

const profileEntries = [
  { label: '账户管理', description: '账户列表、详情、流水和余额修正', route: '/account', icon: WalletCards },
  { label: '预算管理', description: '本月预算、分类预算和超支提醒', route: '/budget', icon: Target },
  { label: '设置', description: '主题、登录状态和退出登录', route: '/settings', icon: Settings }
] as const;

export function ProfileScreen() {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);
  const { userInfo, isHydrated, isLoggedIn, restoreToken, setUserInfo } = useAuthStore();
  const [profileOpen, setProfileOpen] = useState(false);
  const [nickname, setNickname] = useState(userInfo?.nickname || userInfo?.username || '');
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [successTip, setSuccessTip] = useState<string | null>(null);

  useEffect(() => {
    restoreToken();
  }, [restoreToken]);

  useEffect(() => {
    setNickname(userInfo?.nickname || userInfo?.username || '');
  }, [userInfo?.nickname, userInfo?.username]);

  const updateProfileMutation = useMutation({
    mutationFn: (nextNickname: string) => authApi.updateProfile({ nickname: nextNickname }),
    onSuccess: (updated) => {
      setUserInfo(updated);
      setSuccessTip('资料已更新');
    }
  });

  const changePasswordMutation = useMutation({
    mutationFn: () => authApi.changePassword({ oldPassword, newPassword }),
    onSuccess: () => {
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSuccessTip('密码已更新');
    }
  });

  if (!isHydrated) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator color={theme.primary} />
      </View>
    );
  }

  if (!isLoggedIn) {
    return <Redirect href="/login" />;
  }

  async function submitProfile() {
    const value = nickname.trim();
    if (!value) {
      setFormError('显示名称不能为空');
      return;
    }
    setFormError(null);
    setSuccessTip(null);
    try {
      await updateProfileMutation.mutateAsync(value);
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '资料保存失败');
    }
  }

  async function submitPassword() {
    if (!oldPassword || !newPassword) {
      setFormError('请输入原密码和新密码');
      return;
    }
    if (newPassword.length < 6) {
      setFormError('新密码至少 6 位');
      return;
    }
    if (newPassword !== confirmPassword) {
      setFormError('两次输入的新密码不一致');
      return;
    }
    setFormError(null);
    setSuccessTip(null);
    try {
      await changePasswordMutation.mutateAsync();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : '密码修改失败');
    }
  }

  return (
    <SafeAreaView style={styles.page}>
      <GridBackdrop color={theme.border} />
      <ScrollView contentContainerStyle={styles.content} showsVerticalScrollIndicator={false}>
        <View style={styles.header}>
          <Text style={styles.title}>我的</Text>
          <Text variant="muted">账户、预算、投资和设置入口</Text>
        </View>

        <Card>
          <CardContent style={styles.profileCard}>
            <Pressable style={styles.profileRow} onPress={() => setProfileOpen(true)}>
              <View style={styles.avatar}>
                <UserRound color={theme.primaryForeground} size={24} strokeWidth={2.3} />
              </View>
              <View style={styles.profileCopy}>
                <Text style={styles.profileName}>{userInfo?.nickname || userInfo?.username || 'XOAssets 用户'}</Text>
                <Text variant="muted">{userInfo?.username || '已登录'}</Text>
              </View>
              <ChevronRight color={theme.mutedForeground} size={18} />
            </Pressable>
          </CardContent>
        </Card>

        <Card>
          <CardContent style={styles.entryCard}>
            {profileEntries.map((entry, index) => {
              const Icon = entry.icon;
              return (
                <View key={entry.route}>
                  <Pressable style={styles.entryRow} onPress={() => router.push(entry.route)}>
                    <View style={styles.entryIcon}>
                      <Icon color={theme.foreground} size={20} strokeWidth={2.3} />
                    </View>
                    <View style={styles.entryCopy}>
                      <Text style={styles.entryTitle}>{entry.label}</Text>
                      <Text variant="caption">{entry.description}</Text>
                    </View>
                    <ChevronRight color={theme.mutedForeground} size={18} />
                  </Pressable>
                  {index < profileEntries.length - 1 ? <Separator /> : null}
                </View>
              );
            })}
          </CardContent>
        </Card>
      </ScrollView>

      <ProfileEditSheet
        visible={profileOpen}
        username={userInfo?.username || '--'}
        nickname={nickname}
        oldPassword={oldPassword}
        newPassword={newPassword}
        confirmPassword={confirmPassword}
        formError={formError}
        successTip={successTip}
        savingProfile={updateProfileMutation.isPending}
        savingPassword={changePasswordMutation.isPending}
        onClose={() => {
          setProfileOpen(false);
          setFormError(null);
          setSuccessTip(null);
        }}
        onNicknameChange={(value) => {
          setNickname(value);
          setFormError(null);
          setSuccessTip(null);
        }}
        onOldPasswordChange={(value) => {
          setOldPassword(value);
          setFormError(null);
          setSuccessTip(null);
        }}
        onNewPasswordChange={(value) => {
          setNewPassword(value);
          setFormError(null);
          setSuccessTip(null);
        }}
        onConfirmPasswordChange={(value) => {
          setConfirmPassword(value);
          setFormError(null);
          setSuccessTip(null);
        }}
        onSubmitProfile={submitProfile}
        onSubmitPassword={submitPassword}
      />
    </SafeAreaView>
  );
}

function ProfileEditSheet({
  visible,
  username,
  nickname,
  oldPassword,
  newPassword,
  confirmPassword,
  formError,
  successTip,
  savingProfile,
  savingPassword,
  onClose,
  onNicknameChange,
  onOldPasswordChange,
  onNewPasswordChange,
  onConfirmPasswordChange,
  onSubmitProfile,
  onSubmitPassword
}: {
  visible: boolean;
  username: string;
  nickname: string;
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
  formError: string | null;
  successTip: string | null;
  savingProfile: boolean;
  savingPassword: boolean;
  onClose: () => void;
  onNicknameChange: (value: string) => void;
  onOldPasswordChange: (value: string) => void;
  onNewPasswordChange: (value: string) => void;
  onConfirmPasswordChange: (value: string) => void;
  onSubmitProfile: () => void;
  onSubmitPassword: () => void;
}) {
  const theme = useTheme();
  const styles = useMemo(() => createStyles(theme), [theme]);

  return (
    <Modal visible={visible} animationType="slide" transparent onRequestClose={onClose}>
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={styles.modalRoot}>
        <Pressable style={styles.modalBackdrop} onPress={onClose} />
        <View style={styles.sheet}>
          <ScrollView contentContainerStyle={styles.sheetContent} showsVerticalScrollIndicator={false} keyboardShouldPersistTaps="handled">
            <View style={styles.sheetHeader}>
              <Text style={styles.sheetTitle}>用户信息</Text>
              <Pressable onPress={onClose}>
                <X color={theme.foreground} size={24} />
              </Pressable>
            </View>
            <View style={styles.avatarEditRow}>
              <View style={styles.avatarLarge}>
                <UserRound color={theme.primaryForeground} size={30} strokeWidth={2.3} />
              </View>
              <View style={styles.avatarCopy}>
                <Text style={styles.entryTitle}>头像</Text>
                <Text variant="caption">上传图片功能暂未接入</Text>
              </View>
            </View>
            <View style={styles.readonlyField}>
              <Text style={styles.sheetFieldLabel}>用户名</Text>
              <Text variant="muted">{username}</Text>
            </View>
            <Input label="显示名称" value={nickname} onChangeText={onNicknameChange} />
            <Button loading={savingProfile} onPress={onSubmitProfile}>保存资料</Button>
            <Separator />
            <Text style={styles.sheetFieldLabel}>修改密码</Text>
            <Input label="原密码" secureTextEntry value={oldPassword} onChangeText={onOldPasswordChange} />
            <Input label="新密码" secureTextEntry value={newPassword} onChangeText={onNewPasswordChange} />
            <Input label="确认新密码" secureTextEntry value={confirmPassword} onChangeText={onConfirmPasswordChange} />
            <Button variant="secondary" loading={savingPassword} onPress={onSubmitPassword}>修改密码</Button>
            {formError ? <Text variant="error">{formError}</Text> : null}
            {successTip ? <Text style={styles.successText}>{successTip}</Text> : null}
          </ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function GridBackdrop({ color }: { color: string }) {
  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFill}>
      {Array.from({ length: 18 }).map((_, index) => (
        <View key={`v-${index}`} style={[stylesStatic.gridLineVertical, { left: index * 28, backgroundColor: color }]} />
      ))}
      {Array.from({ length: 36 }).map((_, index) => (
        <View key={`h-${index}`} style={[stylesStatic.gridLineHorizontal, { top: index * 28, backgroundColor: color }]} />
      ))}
    </View>
  );
}

const stylesStatic = StyleSheet.create({
  gridLineVertical: {
    opacity: 0.14,
    position: 'absolute',
    top: 0,
    bottom: 0,
    width: 1
  },
  gridLineHorizontal: {
    left: 0,
    opacity: 0.14,
    position: 'absolute',
    right: 0,
    height: 1
  }
});

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  StyleSheet.create({
    page: {
      backgroundColor: theme.background,
      flex: 1
    },
    loading: {
      alignItems: 'center',
      backgroundColor: theme.background,
      flex: 1,
      justifyContent: 'center'
    },
    content: {
      gap: 14,
      padding: 18,
      paddingBottom: 112
    },
    header: {
      gap: 4,
      marginBottom: 4
    },
    title: {
      fontSize: 28,
      fontWeight: '900'
    },
    profileCard: {
      gap: 12
    },
    profileRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    avatar: {
      alignItems: 'center',
      backgroundColor: theme.primary,
      borderRadius: 24,
      height: 48,
      justifyContent: 'center',
      width: 48
    },
    profileCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    profileName: {
      fontSize: 18,
      fontWeight: '900'
    },
    entryCard: {
      gap: 2
    },
    entryRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12,
      paddingVertical: 13
    },
    entryIcon: {
      alignItems: 'center',
      backgroundColor: theme.secondary,
      borderRadius: 18,
      height: 36,
      justifyContent: 'center',
      width: 36
    },
    entryCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    entryTitle: {
      fontSize: 16,
      fontWeight: '800'
    },
    modalRoot: {
      flex: 1,
      justifyContent: 'flex-end'
    },
    modalBackdrop: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: 'rgba(0,0,0,0.2)'
    },
    sheet: {
      backgroundColor: theme.card,
      borderColor: theme.border,
      borderTopLeftRadius: 22,
      borderTopRightRadius: 22,
      borderWidth: 1,
      maxHeight: '88%'
    },
    sheetContent: {
      gap: 14,
      padding: 18,
      paddingBottom: 30
    },
    sheetHeader: {
      alignItems: 'center',
      flexDirection: 'row',
      justifyContent: 'space-between'
    },
    sheetTitle: {
      fontSize: 20,
      fontWeight: '900'
    },
    avatarEditRow: {
      alignItems: 'center',
      flexDirection: 'row',
      gap: 12
    },
    avatarLarge: {
      alignItems: 'center',
      backgroundColor: theme.primary,
      borderRadius: 28,
      height: 56,
      justifyContent: 'center',
      width: 56
    },
    avatarCopy: {
      flex: 1,
      gap: 3,
      minWidth: 0
    },
    readonlyField: {
      borderColor: theme.border,
      borderRadius: 12,
      borderWidth: 1,
      gap: 6,
      padding: 12
    },
    sheetFieldLabel: {
      fontSize: 14,
      fontWeight: '800'
    },
    successText: {
      color: theme.success,
      fontSize: 13,
      fontWeight: '800'
    }
  });
