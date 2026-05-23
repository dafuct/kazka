declare global {
  interface Window {
    Paddle?: {
      Environment: { set: (env: 'sandbox' | 'production') => void }
      Setup: (config: { token: string; eventCallback?: (event: PaddleEvent) => void }) => void
      Checkout: {
        open: (params: { transactionId: string; settings?: { successUrl?: string; theme?: 'light' | 'dark' } }) => void
        close: () => void
      }
    }
  }
}

export interface PaddleEvent {
  name: string
  data?: unknown
}

const PADDLE_JS_URL = 'https://cdn.paddle.com/paddle/v2/paddle.js'

let loadPromise: Promise<void> | null = null
let setupDone = false

function loadScript(): Promise<void> {
  if (window.Paddle) return Promise.resolve()
  if (loadPromise) return loadPromise
  loadPromise = new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = PADDLE_JS_URL
    s.async = true
    // Paddle does not publish stable SRI hashes — they rotate the v2 bundle frequently,
    // so pinning would cause production outages. Defense in depth is provided by:
    //   1) CSP `script-src` in nginx.conf restricts loads to https://cdn.paddle.com
    //   2) `crossOrigin=anonymous` enforces a CORS-validated load (no credentials)
    s.crossOrigin = 'anonymous'
    s.referrerPolicy = 'no-referrer'
    s.onload = () => resolve()
    s.onerror = () => {
      loadPromise = null
      reject(new Error('Failed to load Paddle.js'))
    }
    document.head.appendChild(s)
  })
  return loadPromise
}

function ensureSetup() {
  if (setupDone) return
  const Paddle = window.Paddle
  if (!Paddle) throw new Error('Paddle.js not loaded')
  const env = (import.meta.env.VITE_PADDLE_ENVIRONMENT ?? 'sandbox') as 'sandbox' | 'production'
  const token = import.meta.env.VITE_PADDLE_CLIENT_TOKEN as string | undefined
  if (!token) throw new Error('VITE_PADDLE_CLIENT_TOKEN not set — generate a client-side token in Paddle Dashboard → Developer tools → Authentication.')
  if (env === 'sandbox') Paddle.Environment.set('sandbox')
  Paddle.Setup({ token })
  setupDone = true
}

export async function openPaddleCheckout(params: {
  transactionId: string
  successUrl?: string
  theme?: 'light' | 'dark'
}): Promise<void> {
  await loadScript()
  ensureSetup()
  const Paddle = window.Paddle!
  Paddle.Checkout.open({
    transactionId: params.transactionId,
    settings: {
      successUrl: params.successUrl,
      theme: params.theme,
    },
  })
}
