import { useEffect } from 'react';
import { Dimensions, View } from 'react-native';
import Animated, { useSharedValue, useAnimatedStyle, withRepeat, withTiming, withDelay, Easing } from 'react-native-reanimated';
import { StyleSheet, useUnistyles } from 'react-native-unistyles';
import { useThemeStore } from '@/src/stores/theme.store';

interface ParticleProps {
  delay: number;
  duration: number;
  startX: number;
  size: number;
}

const { width, height } = Dimensions.get('window');

function Particle({ delay, duration, startX, size }: ParticleProps) {
  const { theme } = useUnistyles();
  const translateY = useSharedValue(height);
  const opacity = useSharedValue(0);

  useEffect(() => {
    translateY.value = withDelay(delay, withRepeat(
      withTiming(-50, { duration, easing: Easing.linear }),
      -1, false));
    opacity.value = withDelay(delay, withRepeat(
      withTiming(0.6, { duration: duration / 3, easing: Easing.inOut(Easing.ease) }),
      -1, true));
  }, []);

  const style = useAnimatedStyle(() => ({
    transform: [{ translateY: translateY.value }, { translateX: startX }],
    opacity: opacity.value,
  }));

  return (
    <Animated.View
      style={[
        {
          position: 'absolute',
          width: size, height: size, borderRadius: size / 2,
          backgroundColor: theme.colors.magicGlow,
        },
        style,
      ]}
    />
  );
}

export function Particles() {
  const visualStyle = useThemeStore((s) => s.visualStyle);
  // Particle budget per style.
  const count = visualStyle === 'immersive' ? 20 : visualStyle === 'playful' ? 12 : 8;

  const particles = Array.from({ length: count }, (_, i) => ({
    delay: (i * 700) % 5000,
    duration: 6000 + (i * 137) % 4000,
    startX: ((i * 73) % width),
    size: 3 + (i % 4),
  }));

  return (
    <View pointerEvents="none" style={StyleSheet.absoluteFillObject}>
      {particles.map((p, i) => <Particle key={i} {...p} />)}
    </View>
  );
}
