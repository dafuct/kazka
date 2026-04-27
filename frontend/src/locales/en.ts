import type { Locale } from './uk'

export const en: Locale = {
  nav: {
    home: 'Home',
    archive: 'Story Archive',
    toggleTheme: 'Toggle theme',
    toggleLang: 'UA',
  },
  home: {
    hero: 'Kazkar',
    tagline: 'Magical fairy tales for your child — created by AI',
    cta: 'Create a story',
  },
  form: {
    title: 'New Story',
    theme: 'Story theme',
    themePlaceholder: 'e.g. forest adventure, friendship, a dream',
    characters: 'Main characters',
    charactersPlaceholder: 'Add a character and press Enter',
    ageGroup: "Child's age",
    ageGroups: {
      '3-5': '3–5 years',
      '6-8': '6–8 years',
      '9-12': '9–12 years',
    },
    length: 'Story length',
    lengths: {
      short: 'Short',
      medium: 'Medium',
      long: 'Long',
    },
    language: 'Language',
    languages: {
      uk: 'Ukrainian',
      en: 'English',
    },
    submit: 'Create story',
    generating: 'Generating...',
  },
  story: {
    illustrate: 'Generate illustration',
    illustrating: 'Drawing...',
    edit: 'Edit',
    delete: 'Delete',
    save: 'Save',
    cancel: 'Cancel',
    back: 'Back',
    readTime: (min: number) => `~${min} min read`,
  },
  archive: {
    title: 'Story Archive',
    empty: 'No stories yet. Create your first one!',
    deleteConfirm: 'Delete story?',
    deleteConfirmText: 'This action cannot be undone.',
    confirmDelete: 'Delete',
    cancelDelete: 'Cancel',
  },
  howItWorks: {
    title: 'How it works',
    steps: [
      { title: 'Choose a theme', desc: 'Specify the theme, characters, and child\'s age' },
      { title: 'AI creates the story', desc: 'Ollama generates a unique story on your device' },
      { title: 'Get an illustration', desc: 'Click "Draw" and the story comes to life' },
    ],
  },
  features: {
    title: 'Why Kazkar',
    items: [
      { title: 'Full privacy', desc: 'Everything runs locally on your device' },
      { title: 'Ukrainian language', desc: 'Stories in your native language for your children' },
      { title: 'Unique plots', desc: 'Every story is created just for you' },
    ],
  },
  nightCta: {
    title: 'Time for a story',
    text: 'Start right now — it\'s free and works offline.',
    button: 'Create a story',
  },
  errors: {
    generateFailed: 'Failed to generate story. Please try again.',
    loadFailed: 'Failed to load story.',
    saveFailed: 'Failed to save changes.',
  },
}
