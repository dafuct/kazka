import { requireNativeModule } from 'expo';

interface AppGroupModule {
  writeJSON(filename: string, payload: Record<string, unknown>): Promise<void>;
  reloadAllTimelines(): Promise<void>;
}

const Native = requireNativeModule<AppGroupModule>('AppGroup');
export default Native;
