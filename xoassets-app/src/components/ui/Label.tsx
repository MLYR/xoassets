import type { PropsWithChildren } from 'react';

import { Text } from './Text';

export function Label({ children }: PropsWithChildren) {
  return <Text variant="caption">{children}</Text>;
}
