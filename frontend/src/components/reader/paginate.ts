/** Split tale text into reader pages. Paragraphs (blank-line separated) are
    grouped greedily up to ~targetChars per page; a paragraph is never split. */
export function paginate(text: string, targetChars = 700): string[][] {
  const paragraphs = text
    .split(/\n\s*\n+/)
    .map(p => p.trim())
    .filter(Boolean)
  if (paragraphs.length === 0) return []

  const pages: string[][] = []
  let current: string[] = []
  let size = 0
  for (const p of paragraphs) {
    if (current.length > 0 && size + p.length > targetChars) {
      pages.push(current)
      current = []
      size = 0
    }
    current.push(p)
    size += p.length
  }
  if (current.length > 0) pages.push(current)
  return pages
}
