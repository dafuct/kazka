import { useThemeStore } from './theme.store';

describe('useThemeStore', () => {
  beforeEach(() => {
    useThemeStore.setState({ visualStyle: 'cozy', darkMode: false });
  });

  it('starts at cozy + light', () => {
    expect(useThemeStore.getState().visualStyle).toBe('cozy');
    expect(useThemeStore.getState().darkMode).toBe(false);
  });

  it('setVisualStyle changes only style', () => {
    useThemeStore.getState().setVisualStyle('playful');
    expect(useThemeStore.getState().visualStyle).toBe('playful');
    expect(useThemeStore.getState().darkMode).toBe(false);
  });

  it('toggleDarkMode flips and back', () => {
    useThemeStore.getState().toggleDarkMode();
    expect(useThemeStore.getState().darkMode).toBe(true);
    useThemeStore.getState().toggleDarkMode();
    expect(useThemeStore.getState().darkMode).toBe(false);
  });

  it('themeName concatenates style + mode', () => {
    useThemeStore.getState().setVisualStyle('immersive');
    useThemeStore.getState().toggleDarkMode();
    expect(useThemeStore.getState().themeName()).toBe('immersiveDark');
  });
});
