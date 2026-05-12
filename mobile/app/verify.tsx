import { Redirect, useLocalSearchParams } from 'expo-router';

export default function VerifyDeepLinkRedirect() {
  const { token } = useLocalSearchParams<{ token?: string }>();
  return <Redirect href={{ pathname: '/(auth)/verify-email', params: { token } }} />;
}
