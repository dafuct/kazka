import { render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { en } from '../../locales/en'

// These tests lock in Task A9: features that were previously Pro-gated on the
// frontend (saved characters, including characters in tales) are now usable by
// everyone. The components no longer read billing state at all, so these tests
// assert the feature simply renders/works — no paywall copy, no Upgrade button.

vi.mock('../../lib/LocaleContext', () => ({
  useLocale: () => ({ lang: 'en', t: en, toggleLang: vi.fn() }),
}))

const listCharacters = vi.fn()
const confirm = vi.fn()
const candidates = vi.fn()

vi.mock('../../lib/apiClient', () => ({
  children: { listCharacters: (...a: unknown[]) => listCharacters(...a) },
  charactersApi: { confirm: (...a: unknown[]) => confirm(...a) },
  extraction: { candidates: (...a: unknown[]) => candidates(...a) },
}))

const activeChild = { id: 'child-1', name: 'Mia' }
vi.mock('../../lib/ChildrenContext', () => ({
  useChildren: () => ({ active: activeChild, children: [activeChild] }),
}))

import { CharacterPicker } from './CharacterPicker'
import { ExtractedCharactersPanel } from './ExtractedCharactersPanel'

afterEach(() => {
  vi.clearAllMocks()
})

describe('CharacterPicker (free user)', () => {
  it('renders saved characters instead of a paywall for a non-Pro user', async () => {
    listCharacters.mockResolvedValue([
      { id: 'c1', name: 'Foxy' },
      { id: 'c2', name: 'Owl' },
    ])

    render(<CharacterPicker selected={[]} onChange={() => {}} />)

    // The picker chips appear — feature is reachable.
    expect(await screen.findByText('Foxy')).toBeInTheDocument()
    expect(screen.getByText('Owl')).toBeInTheDocument()

    // The picker hint is shown (feature reachable) and there's no Upgrade button.
    expect(screen.getByText(en.children.pickUpTo3)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: en.common.upgrade })).not.toBeInTheDocument()
    expect(screen.queryByText(/paid plan/i)).not.toBeInTheDocument()
  })

  it('shows the empty state (not a paywall) when a free user has no saved characters', async () => {
    listCharacters.mockResolvedValue([])

    render(<CharacterPicker selected={[]} onChange={() => {}} />)

    expect(await screen.findByText(en.children.noCharactersYet)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: en.common.upgrade })).not.toBeInTheDocument()
  })
})

describe('ExtractedCharactersPanel (free user)', () => {
  it('lets a non-Pro user save extracted characters (no disabled inputs, no upgrade note)', async () => {
    candidates.mockResolvedValue([
      { name: 'Dragon', kind: 'creature', description: 'a friendly dragon' },
    ])

    render(
      <ExtractedCharactersPanel
        storyId="story-1"
        childProfileId="child-1"
        extractionStatus="DONE"
        onConfirmed={() => {}}
      />,
    )

    // The candidate checkbox is rendered AND enabled.
    const checkbox = await screen.findByRole('checkbox')
    expect(checkbox).toBeEnabled()

    // The save button is enabled (a candidate is pre-selected by default).
    const saveBtn = screen.getByRole('button', { name: en.children.saveSelected })
    await waitFor(() => expect(saveBtn).toBeEnabled())

    // No "upgrade to save" note.
    expect(screen.queryByText(/upgrade to save/i)).not.toBeInTheDocument()
  })
})
