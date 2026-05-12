// Theme tokens ported from the design bundle's kazkar-theme.jsx.
// 3 visual styles (cozy/playful/immersive) × 2 modes (light/dark) = 6 themes.

interface Colors {
  bg: string;
  surface: string;
  surface2: string;
  surfaceDeep: string;
  text: string;
  textMuted: string;
  textFaint: string;
  accent: string;
  magic: string;
  magicSoft: string;
  magicGlow: string;
  gold: string;
  forest: string;
  cardBg: string;
  cardBorder: string;
  tabBg: string;
  tabBorder: string;
}

interface ThemeScalars {
  radius: number;
  animDurationMs: number;
  animEasing: string; // cubic-bezier(...) — RN's Easing.bezier(a,b,c,d) form
  titleWeight: '600' | '700';
  bodySize: number;
  headlineSize: number;
}

interface Theme {
  colors: Colors;
  scalars: ThemeScalars;
}

const cozyScalars: ThemeScalars = {
  radius: 16,
  animDurationMs: 300,
  animEasing: 'cubic-bezier(0.16,1,0.3,1)',
  titleWeight: '600',
  bodySize: 15,
  headlineSize: 28,
};

const playfulScalars: ThemeScalars = {
  radius: 20,
  animDurationMs: 400,
  animEasing: 'cubic-bezier(0.34,1.56,0.64,1)',
  titleWeight: '700',
  bodySize: 15,
  headlineSize: 30,
};

const immersiveScalars: ThemeScalars = {
  radius: 12,
  animDurationMs: 500,
  animEasing: 'cubic-bezier(0.4,0,0.2,1)',
  titleWeight: '600',
  bodySize: 16,
  headlineSize: 26,
};

export const themes = {
  cozyLight: {
    colors: {
      bg: '#FDF6EC', surface: '#FAF0DC', surface2: '#F5E6C8', surfaceDeep: '#EDD9A3',
      text: '#2C1810', textMuted: '#6B4C3B', textFaint: '#A07860',
      accent: '#C2410C', magic: '#7C3AED', magicSoft: '#EDE9FE', magicGlow: '#C4B5FD',
      gold: '#D97706', forest: '#166534', cardBg: '#FFFFFF', cardBorder: 'rgba(160,120,96,0.15)',
      tabBg: 'rgba(253,246,236,0.92)', tabBorder: 'rgba(160,120,96,0.12)',
    },
    scalars: cozyScalars,
  },
  cozyDark: {
    colors: {
      bg: '#0F0A1E', surface: '#1A1035', surface2: '#231548', surfaceDeep: '#2D2055',
      text: '#E8DCC8', textMuted: '#A090C0', textFaint: '#6B5A8C',
      accent: '#EA580C', magic: '#8B5CF6', magicSoft: '#1E1040', magicGlow: '#C4B5FD',
      gold: '#F59E0B', forest: '#22C55E', cardBg: '#1E1338', cardBorder: 'rgba(139,92,246,0.15)',
      tabBg: 'rgba(15,10,30,0.92)', tabBorder: 'rgba(139,92,246,0.1)',
    },
    scalars: cozyScalars,
  },
  playfulLight: {
    colors: {
      bg: '#FFF8F0', surface: '#FFF0E0', surface2: '#FFE4CC', surfaceDeep: '#FFD4A8',
      text: '#1A0A00', textMuted: '#7A5030', textFaint: '#B08060',
      accent: '#E85D04', magic: '#8B5CF6', magicSoft: '#F3E8FF', magicGlow: '#D8B4FE',
      gold: '#F59E0B', forest: '#16A34A', cardBg: '#FFFFFF', cardBorder: 'rgba(232,93,4,0.12)',
      tabBg: 'rgba(255,248,240,0.92)', tabBorder: 'rgba(232,93,4,0.1)',
    },
    scalars: playfulScalars,
  },
  playfulDark: {
    colors: {
      bg: '#120828', surface: '#1E0E40', surface2: '#2A1458', surfaceDeep: '#361A70',
      text: '#F0E4D4', textMuted: '#C0A0E0', textFaint: '#8060B0',
      accent: '#FB923C', magic: '#A78BFA', magicSoft: '#1E0E40', magicGlow: '#D8B4FE',
      gold: '#FBBF24', forest: '#34D399', cardBg: '#221040', cardBorder: 'rgba(167,139,250,0.15)',
      tabBg: 'rgba(18,8,40,0.92)', tabBorder: 'rgba(167,139,250,0.1)',
    },
    scalars: playfulScalars,
  },
  immersiveLight: {
    colors: {
      bg: '#F5ECD8', surface: '#EDE0C4', surface2: '#E0D0AC', surfaceDeep: '#D4C090',
      text: '#1A1008', textMuted: '#5A4830', textFaint: '#8A7860',
      accent: '#9333EA', magic: '#7C3AED', magicSoft: '#EDE9FE', magicGlow: '#C4B5FD',
      gold: '#D97706', forest: '#166534', cardBg: '#F0E6D0', cardBorder: 'rgba(147,51,234,0.12)',
      tabBg: 'rgba(245,236,216,0.92)', tabBorder: 'rgba(147,51,234,0.1)',
    },
    scalars: immersiveScalars,
  },
  immersiveDark: {
    colors: {
      bg: '#080510', surface: '#140D28', surface2: '#1C1238', surfaceDeep: '#241848',
      text: '#E0D4C0', textMuted: '#9080B0', textFaint: '#604880',
      accent: '#A855F7', magic: '#7C3AED', magicSoft: '#140D28', magicGlow: '#C4B5FD',
      gold: '#F59E0B', forest: '#22C55E', cardBg: '#180E30', cardBorder: 'rgba(124,58,237,0.2)',
      tabBg: 'rgba(8,5,16,0.92)', tabBorder: 'rgba(124,58,237,0.15)',
    },
    scalars: immersiveScalars,
  },
} as const;

export type ThemeName = keyof typeof themes;
export type VisualStyle = 'cozy' | 'playful' | 'immersive';
export type Mode = 'light' | 'dark';

export function pickTheme(style: VisualStyle, mode: Mode): ThemeName {
  return `${style}${mode === 'light' ? 'Light' : 'Dark'}` as ThemeName;
}

export type { Colors, ThemeScalars, Theme };
