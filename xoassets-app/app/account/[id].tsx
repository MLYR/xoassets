import { useLocalSearchParams } from 'expo-router';

import { AccountDetailScreen } from '@/features/account/screens/AccountDetailScreen';

export default function AccountDetailPage() {
  const params = useLocalSearchParams<{ id?: string }>();
  return <AccountDetailScreen accountId={String(params.id ?? '')} />;
}
