import { StyleSheet, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { Text } from '@/components/ui';
import { useTheme } from '@/core/design/theme';

export function PlaceholderTabScreen({ title }: { title: string }) {
  const theme = useTheme();

  return (
    <SafeAreaView style={[styles.page, { backgroundColor: theme.background }]}>
      <View style={styles.content}>
        <Text style={styles.title}>{title}</Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  page: {
    flex: 1
  },
  content: {
    padding: 20
  },
  title: {
    fontSize: 28,
    fontWeight: '800'
  }
});
