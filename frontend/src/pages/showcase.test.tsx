import { render, screen } from '@testing-library/react'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { en } from '../locales/en'

// Task B6: the public sample-tale showcase. A logged-out visitor can browse the
// 5 curated tales and read one in full, with a "sign up to create your own" CTA,
// but cannot create/edit/delete anything. These tests mock the public showcase
// API (which is unauthenticated) and assert the gallery + read-only reader.

vi.mock('../lib/LocaleContext', () => ({
  useLocale: () => ({ lang: 'en', t: en, toggleLang: vi.fn() }),
}))

const openAuth = vi.fn()
vi.mock('../lib/AuthModalContext', () => ({
  useAuthModal: () => ({ openAuth, closeAuth: vi.fn(), open: false, tab: 'signIn', setTab: vi.fn() }),
}))

vi.mock('../lib/AuthContext', () => ({
  useAuth: () => ({ user: null, loading: false, refresh: vi.fn(), signOut: vi.fn(), resendVerification: vi.fn() }),
}))

const list = vi.fn()
const get = vi.fn()
vi.mock('../lib/apiClient', () => ({
  showcase: {
    list: (...a: unknown[]) => list(...a),
    get: (...a: unknown[]) => get(...a),
  },
}))

import { ShowcasePage } from './ShowcasePage'
import { ShowcaseDetailPage } from './ShowcaseDetailPage'

const tale = (id: string, title: string) => ({
  id,
  title,
  theme: 'forest adventure',
  characters: ['Fox'],
  ageGroup: '3-5',
  length: 'short',
  language: 'uk',
  content: 'Жили собі дід та баба.\n\nІ була в них онучка.',
  illustrationStatus: 'READY',
  extractionStatus: 'SKIPPED',
  isBranching: false,
  branchingState: 'complete',
  panels: [{ panelIndex: 0, imageUrl: `/api/public/showcase/${id}/image/cover.png`, narration: '', dialog: [], aspect: 'PAGE' }],
  createdAt: '2026-06-01T00:00:00Z',
  updatedAt: '2026-06-01T00:00:00Z',
})

afterEach(() => {
  vi.clearAllMocks()
})

describe('ShowcasePage (gallery, logged out)', () => {
  it('renders the returned sample tales and a sign-up CTA', async () => {
    list.mockResolvedValue([tale('s1', 'The Three Acorns'), tale('s2', 'The Wise Rooster')])

    render(
      <MemoryRouter>
        <ShowcasePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('The Three Acorns')).toBeInTheDocument()
    expect(screen.getByText('The Wise Rooster')).toBeInTheDocument()
    const link = screen.getByText('The Wise Rooster').closest('a')
    expect(link).toHaveAttribute('href', '/showcase/s2')

    // The prominent sign-up CTA is present and wired to open the sign-up modal.
    const cta = screen.getByRole('button', { name: en.showcase.cta.button })
    expect(cta).toBeInTheDocument()
    cta.click()
    expect(openAuth).toHaveBeenCalledWith('signUp')
  })

  it('shows an empty state (not an error) when there are no tales', async () => {
    list.mockResolvedValue([])
    render(
      <MemoryRouter>
        <ShowcasePage />
      </MemoryRouter>,
    )
    expect(await screen.findByText(en.showcase.empty)).toBeInTheDocument()
  })

  it('shows an error state when the API rejects', async () => {
    list.mockRejectedValue(new Error('boom'))
    render(
      <MemoryRouter>
        <ShowcasePage />
      </MemoryRouter>,
    )
    expect(await screen.findByText(en.showcase.error)).toBeInTheDocument()
  })
})

describe('ShowcaseDetailPage (read-only reader)', () => {
  it('renders the tale read-only with no edit/create/delete controls', async () => {
    get.mockResolvedValue(tale('s1', 'The Three Acorns'))

    render(
      <MemoryRouter initialEntries={['/showcase/s1']}>
        <Routes>
          <Route path="/showcase/:id" element={<ShowcaseDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('The Three Acorns')).toBeInTheDocument()
    // Content paragraphs render.
    expect(screen.getByText(/Жили собі дід та баба/)).toBeInTheDocument()
    // The panel image renders (reusing ComicsReader).
    expect(screen.getByRole('img', { name: 'The Three Acorns' })).toBeInTheDocument()

    // No read/write controls from the authenticated reader.
    expect(screen.queryByRole('button', { name: en.story.edit })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: en.story.delete })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: en.story.save })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: en.story.illustrate })).not.toBeInTheDocument()
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument()

    // But the sign-up CTA is offered here too.
    expect(screen.getByRole('button', { name: en.showcase.cta.button })).toBeInTheDocument()
  })

  it('shows a friendly not-found message when the tale is missing (404)', async () => {
    get.mockRejectedValue(new Error('404'))

    render(
      <MemoryRouter initialEntries={['/showcase/missing']}>
        <Routes>
          <Route path="/showcase/:id" element={<ShowcaseDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(en.showcase.notFound)).toBeInTheDocument()
  })
})
