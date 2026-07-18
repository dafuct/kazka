import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { TagInput } from '../components/form/TagInput'
import { CharacterPicker } from '../components/children/CharacterPicker'
import { HolidayChip } from '../components/holidays/HolidayChip'
import { useLocale } from '../lib/LocaleContext'
import { useAuth } from '../lib/AuthContext'
import { useChildren } from '../lib/ChildrenContext'
import { useStoryGeneration } from '../lib/useStoryGeneration'
import { branching } from '../lib/apiClient'
import { ApiError } from '../lib/types'
import type { GenerationRequest } from '../lib/types'
import styles from './CreatePage.module.css'

type OptionRow = 'age' | 'length' | 'lang' | 'chars'

const GEN_EMOJI = ['🌙', '⭐', '✨', '📖', '🦊']

export function CreatePage() {
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const { t, lang } = useLocale()
  const { user, resendVerification } = useAuth()
  const { active } = useChildren()
  const { generate, generating, error: genError, clearError } = useStoryGeneration()

  const isSuspended = !!user?.suspended
  const needsVerify = !!user && !user.emailVerified
  const [resendDone, setResendDone] = useState(false)

  const [idea, setIdea] = useState(() => {
    const fromQuery = params.get('idea')
    if (fromQuery) return fromQuery
    const suggested = localStorage.getItem('kazka.suggestedTheme')
    if (suggested) localStorage.removeItem('kazka.suggestedTheme')
    return suggested ?? ''
  })
  const [characters, setCharacters] = useState<string[]>([])
  const [includeCharacterIds, setIncludeCharacterIds] = useState<string[]>([])
  const [ageGroup, setAgeGroup] = useState<GenerationRequest['ageGroup']>('6-8')
  const [length, setLength] = useState<GenerationRequest['length']>('medium')
  const [language, setLanguage] = useState<'uk' | 'en'>(active?.preferredLanguage === 'en' ? 'en' : lang)
  const [isBranching, setIsBranching] = useState(false)
  const [more, setMore] = useState(false)
  const [openRow, setOpenRow] = useState<OptionRow | null>(null)
  const [branchBusy, setBranchBusy] = useState(false)
  const [branchError, setBranchError] = useState<string | null>(null)

  const openOption = (row: OptionRow) => {
    setMore(true)
    setOpenRow(row)
  }

  const canSubmit = !!idea.trim() && characters.length > 0 && !!active && !isSuspended

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (!canSubmit || !active) return
    clearError()
    setBranchError(null)

    const req: GenerationRequest = {
      theme: idea.trim(),
      characters,
      ageGroup,
      length,
      language,
      childProfileId: active.id,
      includeCharacterIds: includeCharacterIds.length > 0 ? includeCharacterIds : undefined,
    }

    if (isBranching) {
      setBranchBusy(true)
      try {
        const resp = await branching.start(req)
        navigate(`/stories/${resp.storyId}`)
      } catch (err: any) {
        if (err instanceof ApiError && err.status === 402) {
          setBranchError(t.story.monthlyLimit)
        } else {
          setBranchError(String(err?.message ?? err))
        }
        setBranchBusy(false)
      }
      return
    }

    generate(req)
  }

  if (generating || branchBusy) {
    return (
      <div className={styles.genWrap}>
        {GEN_EMOJI.map((e, i) => (
          <div
            key={i}
            className={styles.genEmoji}
            style={{
              fontSize: 24 + i * 5,
              left: `${14 + i * 17}%`,
              top: `${22 + (i % 3) * 20}%`,
              animation: `floaty ${2.5 + i * 0.5}s ease-in-out ${i * 0.3}s infinite`,
            }}
          >
            {e}
          </div>
        ))}
        <div className={styles.genCenter}>
          <div className={styles.genOrb} aria-hidden="true">
            <div className={styles.genStar}>✦</div>
          </div>
          <h2 className={styles.genTitle}>{t.create.genTitle}</h2>
          <p className={styles.genSub}>{t.create.genSub}</p>
        </div>
      </div>
    )
  }

  if (needsVerify) {
    return (
      <div className={styles.wrap}>
        <div className={`${styles.card} ${styles.verify}`}>
          <h2 className={styles.verifyTitle}>{t.auth.messages.verifyPanelTitle}</h2>
          <p className={styles.verifyBody}>{t.auth.messages.verifyPanelBody(user!.email)}</p>
          <button
            type="button"
            className="btn btn-primary"
            disabled={resendDone}
            onClick={async () => { await resendVerification(); setResendDone(true) }}
          >
            {resendDone ? '✓' : t.auth.actions.resend}
          </button>
        </div>
      </div>
    )
  }

  const error = genError ?? branchError

  return (
    <div className={styles.wrap}>
      {active && (
        <div className={styles.holiday}>
          <HolidayChip onApply={theme => setIdea(theme)} />
        </div>
      )}
      <form className={styles.card} onSubmit={handleSubmit}>
        <textarea
          className={styles.input}
          value={idea}
          onChange={e => setIdea(e.target.value)}
          placeholder={t.create.placeholder}
          required
        />

        <div className={styles.charsRow}>
          <div className={styles.charsLabel}>{t.form.characters}</div>
          <TagInput
            id="characters"
            value={characters}
            onChange={setCharacters}
            placeholder={t.form.charactersPlaceholder}
          />
        </div>

        <div className={styles.qset}>
          <div className={styles.qlbl}>{t.create.quick}</div>
          <div className={styles.qrow}>
            <button type="button" className={styles.qchip} onClick={() => openOption('age')}>
              {t.create.chAge}: <b>{t.form.ageGroups[ageGroup]}</b>
            </button>
            <button type="button" className={styles.qchip} onClick={() => openOption('length')}>
              {t.create.chLength}: <b>{t.form.lengths[length]}</b>
            </button>
            <button type="button" className={styles.qchip} onClick={() => openOption('lang')}>
              {t.create.chLang}: <b>{t.form.languages[language]}</b>
            </button>
            <button type="submit" className={`btn btn-primary ${styles.qsubmit}`} disabled={!canSubmit}>
              {t.create.submit}
            </button>
          </div>
        </div>

        <button type="button" className={styles.moreHead} onClick={() => setMore(m => !m)}>
          {t.create.more}
          <span className={more ? `${styles.chev} ${styles.chevOpen}` : styles.chev} aria-hidden="true">⌄</span>
        </button>

        {more && (
          <>
            <div className={styles.orow}>
              <div className={styles.orowHead} onClick={() => setOpenRow(o => (o === 'age' ? null : 'age'))}>
                <span className={styles.orowName}>{t.form.ageGroup}</span>
                <span className={styles.orowVal}>{t.form.ageGroups[ageGroup]}</span>
              </div>
              {openRow === 'age' && (
                <div className={styles.opts}>
                  {(Object.entries(t.form.ageGroups) as [GenerationRequest['ageGroup'], string][]).map(([k, v]) => (
                    <button
                      key={k}
                      type="button"
                      className={ageGroup === k ? `${styles.oopt} ${styles.ooptOn}` : styles.oopt}
                      onClick={() => setAgeGroup(k)}
                    >
                      {v}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className={styles.orow}>
              <div className={styles.orowHead} onClick={() => setOpenRow(o => (o === 'length' ? null : 'length'))}>
                <span className={styles.orowName}>{t.form.length}</span>
                <span className={styles.orowVal}>{t.form.lengths[length]}</span>
              </div>
              {openRow === 'length' && (
                <div className={styles.opts}>
                  {(Object.entries(t.form.lengths) as [GenerationRequest['length'], string][]).map(([k, v]) => (
                    <button
                      key={k}
                      type="button"
                      className={length === k ? `${styles.oopt} ${styles.ooptOn}` : styles.oopt}
                      onClick={() => setLength(k)}
                    >
                      {v}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className={styles.orow}>
              <div className={styles.orowHead} onClick={() => setOpenRow(o => (o === 'lang' ? null : 'lang'))}>
                <span className={styles.orowName}>{t.form.language}</span>
                <span className={styles.orowVal}>{t.form.languages[language]}</span>
              </div>
              {openRow === 'lang' && (
                <div className={styles.opts}>
                  {(Object.entries(t.form.languages) as ['uk' | 'en', string][]).map(([k, v]) => (
                    <button
                      key={k}
                      type="button"
                      className={language === k ? `${styles.oopt} ${styles.ooptOn}` : styles.oopt}
                      onClick={() => setLanguage(k)}
                    >
                      {v}
                    </button>
                  ))}
                </div>
              )}
            </div>

            <div className={styles.orow}>
              <div className={styles.orowHead} onClick={() => setOpenRow(o => (o === 'chars' ? null : 'chars'))}>
                <span className={styles.orowName}>{t.create.charactersName}</span>
                {includeCharacterIds.length > 0 && (
                  <span className={styles.orowVal}>{includeCharacterIds.length}</span>
                )}
              </div>
              {openRow === 'chars' && (
                <div style={{ marginTop: 14 }}>
                  <CharacterPicker selected={includeCharacterIds} onChange={setIncludeCharacterIds} />
                </div>
              )}
              <label className={isBranching ? `${styles.magicToggle} ${styles.magicToggleOn}` : styles.magicToggle}>
                <input
                  type="checkbox"
                  checked={isBranching}
                  onChange={e => setIsBranching(e.target.checked)}
                />
                <span aria-hidden="true">✨</span>
                <span>{(t as any).branching?.formToggle ?? 'Branching tale'}</span>
              </label>
            </div>
          </>
        )}
      </form>
      {error && <p className={styles.error}>{error}</p>}
      {isSuspended && <p className={styles.error}>{t.moderation.formDisabledSuspended}</p>}
    </div>
  )
}
