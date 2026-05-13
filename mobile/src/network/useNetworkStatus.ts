import NetInfo from '@react-native-community/netinfo';
import { useEffect, useState } from 'react';

export interface NetworkStatus {
  isOnline: boolean;
  isInternetReachable: boolean | null;
}

export function useNetworkStatus(): NetworkStatus {
  const [status, setStatus] = useState<NetworkStatus>({
    isOnline: true,
    isInternetReachable: null,
  });

  useEffect(() => {
    const unsub = NetInfo.addEventListener((s) => {
      setStatus({
        isOnline: s.isConnected ?? false,
        isInternetReachable: s.isInternetReachable,
      });
    });
    return () => unsub();
  }, []);

  return status;
}
