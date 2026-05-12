import { forwardRef } from 'react';
import { TextInput, type TextInputProps } from 'react-native';
import { StyleSheet, useUnistyles } from 'react-native-unistyles';

export const Input = forwardRef<TextInput, TextInputProps>((props, ref) => {
  const { theme } = useUnistyles();
  return <TextInput ref={ref} placeholderTextColor={theme.colors.textFaint} style={styles.input} {...props} />;
});
Input.displayName = 'Input';

const styles = StyleSheet.create((theme) => ({
  input: {
    backgroundColor: theme.colors.surface,
    color: theme.colors.text,
    borderRadius: theme.scalars.radius / 2,
    paddingHorizontal: 16,
    paddingVertical: 14,
    fontSize: theme.scalars.bodySize + 1,
  },
}));
