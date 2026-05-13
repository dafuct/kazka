import { apiClient } from './client';

export interface DeviceRegisterArgs {
  deviceToken: string;
  platform: 'ios';
  locale?: string;
}

export const devicesApi = {
  register: (args: DeviceRegisterArgs): Promise<void> =>
    apiClient.post('/api/devices/register', args),

  unregister: (deviceToken: string): Promise<void> =>
    apiClient.delete(`/api/devices/${encodeURIComponent(deviceToken)}`),
};
