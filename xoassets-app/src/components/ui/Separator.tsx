import { StyleSheet, View } from 'react-native';

import { useTheme } from '@/styles/theme';

export function Separator() {
  const theme = useTheme();

  return <View style={[styles.separator, { backgroundColor: theme.border }]} />;
}

const styles = StyleSheet.create({
  separator: {
    height: StyleSheet.hairlineWidth,
    width: '100%'
  }
});
