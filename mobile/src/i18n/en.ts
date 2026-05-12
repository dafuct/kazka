export const en = {
  welcome: {
    title: 'Kazkar',
    subtitle: 'Magical Ukrainian fairy tales',
    signIn: 'Sign in',
    signUp: 'Sign up',
  },
  login: {
    title: 'Welcome back',
    email: 'Email',
    password: 'Password',
    submit: 'Sign in',
    forgot: 'Forgot password?',
    withGoogle: 'Continue with Google',
    withApple: 'Continue with Apple',
  },
  signup: {
    title: 'Create an account',
    displayName: 'Your name',
    email: 'Email',
    password: 'Password',
    submit: 'Create account',
    alreadyHave: 'Already have an account? Sign in',
  },
  verifyEmail: {
    title: 'Check your email',
    body: 'We sent a verification link to {{email}}. Open it on this device to finish setting up your account.',
    resend: 'Resend verification email',
  },
  forgot: {
    title: 'Reset password',
    body: 'Enter your email — we will send a link to reset it.',
    email: 'Email',
    submit: 'Send reset link',
    sent: 'Check your email for the reset link.',
  },
  errors: {
    INVALID_CREDENTIALS: 'Incorrect email or password.',
    EMAIL_TAKEN: 'That email is already registered.',
    EMAIL_NOT_VERIFIED: 'Please verify your email first.',
    INVALID_REFRESH_TOKEN: 'Your session has expired. Please sign in again.',
    INVALID_APPLE_TOKEN: 'Apple sign-in failed. Please try again.',
    ACCOUNT_SUSPENDED: 'Your account is suspended.',
    VALIDATION: 'Please check the form for errors.',
    UNAUTHENTICATED: 'Please sign in.',
    NETWORK: 'Network error. Please try again.',
    ERROR: 'Something went wrong.',
  },
  tabs: {
    home: 'Home',
    library: 'Library',
    create: 'Create',
    profile: 'Profile',
  },
  profile: {
    signOut: 'Sign out',
  },
};

export type Translations = typeof en;
