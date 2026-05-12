import type { Translations } from './en';

export const uk: Translations = {
  welcome: {
    title: 'Казкар',
    subtitle: 'Магічні українські казки',
    signIn: 'Увійти',
    signUp: 'Створити акаунт',
  },
  login: {
    title: 'З поверненням',
    email: 'Електронна пошта',
    password: 'Пароль',
    submit: 'Увійти',
    forgot: 'Забули пароль?',
    withGoogle: 'Продовжити з Google',
    withApple: 'Продовжити з Apple',
  },
  signup: {
    title: 'Створити акаунт',
    displayName: 'Ваше ім\'я',
    email: 'Електронна пошта',
    password: 'Пароль',
    submit: 'Створити',
    alreadyHave: 'Вже маєте акаунт? Увійти',
  },
  verifyEmail: {
    title: 'Перевірте пошту',
    body: 'Ми надіслали посилання для підтвердження на {{email}}. Відкрийте його на цьому пристрої, щоб завершити налаштування.',
    resend: 'Надіслати лист знову',
  },
  forgot: {
    title: 'Скинути пароль',
    body: 'Введіть свою електронну пошту — ми надішлемо посилання для скидання.',
    email: 'Електронна пошта',
    submit: 'Надіслати посилання',
    sent: 'Перевірте пошту, щоб отримати посилання для скидання.',
  },
  errors: {
    INVALID_CREDENTIALS: 'Неправильна електронна пошта або пароль.',
    EMAIL_TAKEN: 'Ця електронна пошта вже зареєстрована.',
    EMAIL_NOT_VERIFIED: 'Спершу підтвердіть свою електронну пошту.',
    INVALID_REFRESH_TOKEN: 'Ваша сесія закінчилася. Будь ласка, увійдіть знову.',
    INVALID_APPLE_TOKEN: 'Не вдалося увійти через Apple. Спробуйте ще раз.',
    ACCOUNT_SUSPENDED: 'Ваш акаунт призупинено.',
    VALIDATION: 'Перевірте форму на помилки.',
    UNAUTHENTICATED: 'Будь ласка, увійдіть.',
    NETWORK: 'Помилка мережі. Спробуйте ще раз.',
    ERROR: 'Щось пішло не так.',
  },
  tabs: {
    home: 'Головна',
    library: 'Бібліотека',
    create: 'Створити',
    profile: 'Профіль',
  },
  profile: {
    signOut: 'Вийти',
  },
};
