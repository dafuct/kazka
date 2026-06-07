import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { en } from '../../locales/en'

// Task C3: the create flow lets a user add MULTIPLE children before one submit,
// which fires a single POST /api/children/batch. This test locks in that
// behaviour at the component level: start with one row, add a second, fill both
// names, submit once → onSubmit is called with exactly two child requests.

vi.mock('../../lib/LocaleContext', () => ({
  useLocale: () => ({ lang: 'en', t: en, toggleLang: vi.fn() }),
}))

import { ChildProfileBatchForm } from './ChildProfileBatchForm'

afterEach(() => {
  vi.clearAllMocks()
})

describe('ChildProfileBatchForm (multi-child create)', () => {
  it('adds a second row and submits a batch with two children', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)

    render(<ChildProfileBatchForm onSubmit={onSubmit} submitLabel={en.children.createCta} />)

    // Starts with a single row → one Name input.
    let nameInputs = screen.getAllByRole('textbox', { name: en.children.fieldName })
    expect(nameInputs).toHaveLength(1)

    // Add another child row.
    await user.click(screen.getByRole('button', { name: en.children.addAnotherChild }))

    nameInputs = screen.getAllByRole('textbox', { name: en.children.fieldName })
    expect(nameInputs).toHaveLength(2)

    // Fill both names (name is the only client-required field per row).
    await user.type(nameInputs[0], 'Mia')
    await user.type(nameInputs[1], 'Leo')

    // One submit fires a single batch call with both children.
    await user.click(screen.getByRole('button', { name: en.children.createCta }))

    expect(onSubmit).toHaveBeenCalledTimes(1)
    const arg = onSubmit.mock.calls[0][0]
    expect(arg).toHaveLength(2)
    expect(arg[0].name).toBe('Mia')
    expect(arg[1].name).toBe('Leo')
  })

  it('keeps submit disabled until every row has a name', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn().mockResolvedValue(undefined)

    render(<ChildProfileBatchForm onSubmit={onSubmit} submitLabel={en.children.createCta} />)

    const submit = screen.getByRole('button', { name: en.children.createCta })
    expect(submit).toBeDisabled()

    await user.type(screen.getByRole('textbox', { name: en.children.fieldName }), 'Mia')
    expect(submit).toBeEnabled()

    // A new, empty row should re-disable submit.
    await user.click(screen.getByRole('button', { name: en.children.addAnotherChild }))
    expect(submit).toBeDisabled()
  })

  it('removes a row', async () => {
    const user = userEvent.setup()
    render(<ChildProfileBatchForm onSubmit={vi.fn()} submitLabel={en.children.createCta} />)

    await user.click(screen.getByRole('button', { name: en.children.addAnotherChild }))
    expect(screen.getAllByRole('textbox', { name: en.children.fieldName })).toHaveLength(2)

    await user.click(screen.getAllByRole('button', { name: en.children.removeChild })[0])
    expect(screen.getAllByRole('textbox', { name: en.children.fieldName })).toHaveLength(1)
  })
})
