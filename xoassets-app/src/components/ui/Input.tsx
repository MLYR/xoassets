import { forwardRef, useState } from 'react';
import {
  StyleSheet,
  TextInput,
  View,
  type TextInputProps,
  type TextStyle,
  type ViewStyle
} from 'react-native';

import { useTheme } from '@/styles/theme';
import { Label } from './Label';
import { Text } from './Text';

interface InputProps extends TextInputProps {
  label?: string;
  error?: string;
  containerStyle?: ViewStyle;
  inputStyle?: TextStyle;
}

export const Input = forwardRef<TextInput, InputProps>(
  ({ label, error, containerStyle, inputStyle, editable = true, onFocus, onBlur, ...props }, ref) => {
    const theme = useTheme();
    const [focused, setFocused] = useState(false);
    const styles = createStyles(theme);

    return (
      <View style={[styles.wrapper, containerStyle]}>
        {label ? <Label>{label}</Label> : null}
        <TextInput
          ref={ref}
          editable={editable}
          placeholderTextColor={theme.mutedForeground}
          selectionColor={theme.ring}
          style={[
            styles.input,
            focused ? styles.focused : null,
            error ? styles.error : null,
            !editable ? styles.disabled : null,
            inputStyle
          ]}
          onFocus={(event) => {
            setFocused(true);
            onFocus?.(event);
          }}
          onBlur={(event) => {
            setFocused(false);
            onBlur?.(event);
          }}
          {...props}
        />
        {error ? <Text variant="error">{error}</Text> : null}
      </View>
    );
  }
);

Input.displayName = 'Input';

const createStyles = (theme: ReturnType<typeof useTheme>) =>
  StyleSheet.create({
    wrapper: {
      gap: 6
    },
    input: {
      backgroundColor: theme.background,
      borderColor: theme.input,
      borderRadius: theme.tokens.radius.md,
      borderWidth: 1,
      color: theme.foreground,
      fontSize: theme.tokens.fontSize.body,
      minHeight: 46,
      paddingHorizontal: theme.tokens.spacing.md
    },
    focused: {
      borderColor: theme.ring
    },
    error: {
      borderColor: theme.destructive
    },
    disabled: {
      opacity: 0.5
    }
  });
