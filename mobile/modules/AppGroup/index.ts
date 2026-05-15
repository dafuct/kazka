import { requireOptionalNativeModule } from 'expo';

interface AppGroupModule {
  writeJSON(filename: string, payload: Record<string, unknown>): Promise<void>;
  reloadAllTimelines(): Promise<void>;
}

// Optional: the native module ships only inside iOS binaries that included the
// Pods install adding AppGroupModule.swift. Builds that predate that linkage
// (or Android, where this module is unavailable) get a no-op stub. Widget
// updates are best-effort and the app never depends on them.
const Native = requireOptionalNativeModule<AppGroupModule>('AppGroup');

const Stub: AppGroupModule = {
  writeJSON: async () => {},
  reloadAllTimelines: async () => {},
};

export default Native ?? Stub;
