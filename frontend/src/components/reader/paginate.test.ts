import { describe, it, expect } from 'vitest'
import { paginate } from './paginate'

describe('paginate', () => {
  it('returns no pages for empty/whitespace text', () => {
    expect(paginate('')).toEqual([])
    expect(paginate('  \n\n  ')).toEqual([])
  })

  it('puts a single short paragraph on one page', () => {
    expect(paginate('Жила собі лисичка.')).toEqual([['Жила собі лисичка.']])
  })

  it('groups paragraphs up to the target size without splitting any paragraph', () => {
    const p1 = 'а'.repeat(400)
    const p2 = 'б'.repeat(400)
    const p3 = 'в'.repeat(400)
    const pages = paginate(`${p1}\n\n${p2}\n\n${p3}`, 700)
    // 400 fits; 400+400 > 700 → p2 starts page 2; p2+p3 > 700 → p3 starts page 3
    expect(pages).toEqual([[p1], [p2], [p3]])
  })

  it('keeps multiple small paragraphs on one page', () => {
    const pages = paginate('Один.\n\nДва.\n\nТри.', 700)
    expect(pages).toEqual([['Один.', 'Два.', 'Три.']])
  })

  it('never drops content — joined pages equal the source paragraphs', () => {
    const text = 'Перший абзац казки.\n\nДругий, трохи довший абзац казки.\n\n\nТретій.'
    const flat = paginate(text, 30).flat()
    expect(flat).toEqual(['Перший абзац казки.', 'Другий, трохи довший абзац казки.', 'Третій.'])
  })
})
