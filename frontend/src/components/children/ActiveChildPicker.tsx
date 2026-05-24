import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useChildren } from '../../lib/ChildrenContext'
import { useLocale } from '../../lib/LocaleContext'
import { AvatarInitials } from './AvatarInitials'
import styles from './ActiveChildPicker.module.css'

export function ActiveChildPicker() {
  const { children, active, setActive } = useChildren()
  const { t } = useLocale()
  const [open, setOpen] = useState(false)

  if (children.length === 0) {
    return (
      <Link to="/settings/children/new" className={styles.empty}>
        + {(t as any).children?.addChild ?? 'Add child'}
      </Link>
    )
  }

  return (
    <div className={styles.wrap}>
      <button className={styles.trigger} onClick={() => setOpen(v => !v)} aria-expanded={open}>
        {active && <AvatarInitials name={active.name} seed={active.avatarSeed} size={24} />}
        <span>{active?.name ?? ((t as any).children?.pickerNoneLabel ?? '—')}</span>
        <span className={styles.caret}>▾</span>
      </button>
      {open && (
        <ul className={styles.menu} onMouseLeave={() => setOpen(false)}>
          {children.map(c => (
            <li key={c.id}>
              <button onClick={() => { setActive(c.id); setOpen(false) }}>
                <AvatarInitials name={c.name} seed={c.avatarSeed} size={22} />
                <span>{c.name}</span>
                {active?.id === c.id && <span className={styles.check}>✓</span>}
              </button>
            </li>
          ))}
          <li className={styles.divider} aria-hidden />
          <li><Link to="/settings/children" onClick={() => setOpen(false)}>{(t as any).children?.manageLink ?? 'Manage children'} →</Link></li>
          <li><Link to="/settings/children/new" onClick={() => setOpen(false)}>+ {(t as any).children?.addChild ?? 'Add child'}</Link></li>
        </ul>
      )}
    </div>
  )
}
