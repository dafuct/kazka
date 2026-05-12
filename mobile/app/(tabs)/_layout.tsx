import { Tabs } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';
import { useUnistyles } from 'react-native-unistyles';
import { TabBarCenterButton } from '@/src/components/TabBarCenterButton';

export default function TabsLayout() {
  const { t } = useTranslation();
  const { theme } = useUnistyles();

  return (
    <Tabs
      screenOptions={{
        headerShown: false,
        tabBarStyle: { backgroundColor: theme.colors.tabBg, borderTopColor: theme.colors.tabBorder },
        tabBarActiveTintColor: theme.colors.accent,
        tabBarInactiveTintColor: theme.colors.textFaint,
      }}
    >
      <Tabs.Screen
        name="index"
        options={{
          title: t('tabs.home'),
          tabBarIcon: ({ color }) => <View style={{ width: 22, height: 22, borderRadius: 4, backgroundColor: color }} />,
        }}
      />
      <Tabs.Screen
        name="library"
        options={{
          title: t('tabs.library'),
          tabBarIcon: ({ color }) => <View style={{ width: 22, height: 22, borderRadius: 4, backgroundColor: color }} />,
        }}
      />
      <Tabs.Screen
        name="create-pseudo"
        options={{
          title: '',
          tabBarButton: () => <TabBarCenterButton />,
        }}
        listeners={{
          tabPress: (e) => { e.preventDefault(); },
        }}
      />
      <Tabs.Screen
        name="profile"
        options={{
          title: t('tabs.profile'),
          tabBarIcon: ({ color }) => <View style={{ width: 22, height: 22, borderRadius: 11, backgroundColor: color }} />,
        }}
      />
    </Tabs>
  );
}
