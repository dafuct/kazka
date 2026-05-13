import type { ExpoConfig } from 'expo/config';

const config: ExpoConfig = {
  name: 'Kazkar',
  slug: 'kazkar',
  version: '0.1.0',
  orientation: 'portrait',
  icon: './assets/images/icon.png',
  // expo prebuild also adds 'app.kazka.ios' as a second URL scheme automatically
  // (used by Google sign-in's OAuth callback). Don't list it here — it's derived
  // from ios.bundleIdentifier.
  scheme: 'kazka',
  userInterfaceStyle: 'automatic',
  newArchEnabled: true,
  ios: {
    supportsTablet: false,
    bundleIdentifier: 'app.kazka.ios',
    associatedDomains: ['applinks:kazka.app'],
    usesAppleSignIn: true,
    buildNumber: '2',
    entitlements: {
      'aps-environment': 'development',
    },
  },
  android: {
    package: 'app.kazka.android',
    adaptiveIcon: {
      backgroundColor: '#E6F4FE',
      foregroundImage: './assets/images/android-icon-foreground.png',
      backgroundImage: './assets/images/android-icon-background.png',
      monochromeImage: './assets/images/android-icon-monochrome.png',
    },
    edgeToEdgeEnabled: true,
    predictiveBackGestureEnabled: false,
  },
  web: {
    output: 'static',
    favicon: './assets/images/favicon.png',
  },
  plugins: [
    'expo-router',
    [
      'expo-splash-screen',
      {
        image: './assets/images/splash-icon.png',
        imageWidth: 200,
        resizeMode: 'contain',
        backgroundColor: '#ffffff',
        dark: {
          backgroundColor: '#000000',
        },
      },
    ],
    'expo-secure-store',
    'expo-localization',
    'expo-apple-authentication',
    'expo-notifications',
  ],
  experiments: {
    typedRoutes: true,
    reactCompiler: true,
  },
};

export default config;
