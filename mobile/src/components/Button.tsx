import { ActivityIndicator, Text, TouchableOpacity, type TouchableOpacityProps } from 'react-native';
import { StyleSheet } from 'react-native-unistyles';

interface ButtonProps extends TouchableOpacityProps {
  title: string;
  variant?: 'primary' | 'secondary';
  loading?: boolean;
}

export function Button({ title, variant = 'primary', loading = false, style, ...rest }: ButtonProps) {
  return (
    <TouchableOpacity
      style={[variant === 'primary' ? styles.primary : styles.secondary, loading && styles.disabled, style]}
      disabled={loading || rest.disabled}
      accessibilityRole="button"
      accessibilityLabel={title}
      accessibilityState={{ disabled: loading || rest.disabled }}
      {...rest}
    >
      {loading ? (
        <ActivityIndicator color="#fff" />
      ) : (
        <Text style={variant === 'primary' ? styles.primaryText : styles.secondaryText}>{title}</Text>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create((theme) => ({
  primary: {
    backgroundColor: theme.colors.accent,
    borderRadius: theme.scalars.radius,
    paddingVertical: 14,
    alignItems: 'center',
  },
  primaryText: { color: '#fff', fontSize: 17, fontWeight: '600' },
  secondary: {
    backgroundColor: theme.colors.surface,
    borderRadius: theme.scalars.radius,
    paddingVertical: 14,
    alignItems: 'center',
  },
  secondaryText: { color: theme.colors.text, fontSize: 17, fontWeight: '600' },
  disabled: { opacity: 0.6 },
}));
