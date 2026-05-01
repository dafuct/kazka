import { useEffect, useRef, useState } from 'react'
import { useReveal } from '../../lib/useReveal'
import { useTheme } from '../../lib/ThemeContext'
import { SectionParticles } from './SectionParticles'
import styles from './HowItWorks.module.css'

function CastleIllustration() {
  const { theme } = useTheme()
  const d = theme === 'dark'

  const sky    = d ? '#0A071C' : '#5AACCE'
  const skyMid = d ? '#110C2E' : '#72B8E0'
  const mtn1   = d ? '#0C0920' : '#1E4828'
  const mtn2   = d ? '#100C28' : '#286835'
  const gnd    = d ? '#080614' : '#2A5228'
  const gndL   = d ? '#0E0C20' : '#3A7034'
  const st1    = d ? '#28204A' : '#D2C098'
  const st2    = d ? '#342858' : '#E0CEAA'
  const st3    = d ? '#1C1636' : '#B0A080'
  const rf1    = d ? '#281C4C' : '#5C3060'
  const rf2    = d ? '#3C2870' : '#7A408C'
  const wDark  = d ? '#0A0818' : '#5C3820'
  const wGlow  = '#FFD060'
  const flagR  = d ? '#CC2838' : '#D83040'
  const flagB  = d ? '#2440A8' : '#3050B8'
  const moonC  = '#E4ECFF'
  const starC  = '#CCD8FF'
  const sparkA = d ? '#B090FF' : '#7840D8'
  const sparkB = d ? '#70FFD8' : '#30B898'
  const grass  = d ? '#162014' : '#4A8A38'

  return (
    <svg viewBox="0 0 300 400" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
      <defs>
        <clipPath id="castleClip"><rect width="300" height="400" rx="12"/></clipPath>
      </defs>
      <g clipPath="url(#castleClip)">

        {/* Sky */}
        <rect width="300" height="400" fill={sky}/>
        <rect y="220" width="300" height="90" fill={skyMid} opacity="0.2"/>

        {/* Moon (night) */}
        {d && <>
          <circle cx="50" cy="60" r="26" fill={moonC}/>
          <circle cx="61" cy="51" r="21" fill={sky}/>
          {([
            [28,30,3.2],[68,20,2.8],[100,28,3.5],[135,16,2.8],[168,26,3.2],
            [198,18,2.5],[232,30,3],[265,22,2.8],[285,40,2],[20,52,1.8],
            [52,42,1.5],[90,48,1.8],[120,38,1.5],[152,44,1.8],[180,35,1.5],
            [210,50,1.8],[248,44,1.5],[275,35,1.8],[295,55,1.5],
          ] as [number,number,number][]).map(([x,y,s],i) => (
            <path key={i}
              d={`M${x} ${y-s} L${x+s*.38} ${y-s*.38} L${x+s} ${y} L${x+s*.38} ${y+s*.38} L${x} ${y+s} L${x-s*.38} ${y+s*.38} L${x-s} ${y} L${x-s*.38} ${y-s*.38}Z`}
              fill={starC} opacity="0.85">
              <animate attributeName="opacity" values="0.85;0.2;0.85" dur={`${2+i*0.18}s`} begin={`${i*0.24}s`} repeatCount="indefinite"/>
            </path>
          ))}
        </>}

        {/* Sun (day) */}
        {!d && <>
          <g>
            <animateTransform attributeName="transform" type="rotate" values="0,245,55;360,245,55" dur="90s" repeatCount="indefinite"/>
            {Array.from({length:10},(_,i) => {
              const a = (i*36)*Math.PI/180
              return <line key={i}
                x1={245+Math.cos(a)*35} y1={55+Math.sin(a)*35}
                x2={245+Math.cos(a)*(i%2?50:60)} y2={55+Math.sin(a)*(i%2?50:60)}
                stroke="#FAC000" strokeWidth={i%2?2.2:3.5} strokeLinecap="round"/>
            })}
          </g>
          <circle cx="245" cy="55" r="28" fill="#FFE040"/>
          <circle cx="245" cy="55" r="20" fill="#FFF860"/>
          <circle cx="237" cy="47" r="8" fill="#FFFAA0" opacity="0.5"/>
        </>}

        {/* Mountains */}
        <path d="M-10 310 Q20 260 55 278 Q85 294 110 260 Q135 232 152 255 Q168 276 192 248 Q218 222 248 256 Q272 280 310 264 L310 400 L-10 400Z" fill={mtn1}/>
        <path d="M-10 332 Q25 300 60 316 Q95 330 128 308 Q158 290 178 308 Q200 326 238 308 Q265 294 310 312 L310 400 L-10 400Z" fill={mtn2}/>

        {/* Ground */}
        <rect x="-10" y="348" width="320" height="62" fill={gnd}/>
        <path d="M-10 348 Q35 340 72 350 Q112 358 152 344 Q192 332 232 344 Q268 354 310 346 L310 356 L-10 356Z" fill={gndL}/>
        {([[25,348],[75,352],[138,345],[200,347],[258,350]] as [number,number][]).map(([x,y],i) => (
          <g key={i}>
            <path d={`M${x} ${y} Q${x-3} ${y-8} ${x-6} ${y-11}`} stroke={grass} strokeWidth="1.5" fill="none" strokeLinecap="round"/>
            <path d={`M${x} ${y} Q${x} ${y-9} ${x-1} ${y-13}`} stroke={grass} strokeWidth="1.5" fill="none" strokeLinecap="round"/>
            <path d={`M${x} ${y} Q${x+3} ${y-7} ${x+6} ${y-10}`} stroke={grass} strokeWidth="1.5" fill="none" strokeLinecap="round"/>
          </g>
        ))}

        {/* === CASTLE === */}

        {/* Back towers */}
        <rect x="42" y="232" width="36" height="72" fill={st3}/>
        {([44,53,62] as number[]).map((x,i) => i%2===0 ? <rect key={x} x={x} y="222" width="8" height="12" fill={st3} rx="1"/> : null)}
        <path d="M39 230 L60 176 L81 230Z" fill={rf1}/>
        <rect x="222" y="238" width="36" height="68" fill={st3}/>
        {([224,233,242] as number[]).map((x,i) => i%2===0 ? <rect key={x} x={x} y="228" width="8" height="12" fill={st3} rx="1"/> : null)}
        <path d="M219 236 L240 185 L261 236Z" fill={rf1}/>

        {/* Main wall */}
        <rect x="62" y="258" width="176" height="97" fill={st1}/>
        {([64,77,90,103,116,129,143,157,171,185,199,213,225] as number[]).map((x,i) => i%2===0 ? <rect key={x} x={x} y="248" width="10" height="14" fill={st1} rx="1"/> : null)}
        {([270,284,298,312,325] as number[]).map((y,i) => (
          <line key={i} x1="62" y1={y} x2="238" y2={y} stroke={st3} strokeWidth="0.7" opacity="0.35"/>
        ))}

        {/* Left tower */}
        <rect x="58" y="200" width="54" height="68" fill={st2}/>
        {([60,70,80,90,100] as number[]).map((x,i) => i%2===0 ? <rect key={x} x={x} y="188" width="10" height="15" fill={st2} rx="1"/> : null)}
        <path d="M54 198 L85 120 L116 198Z" fill={rf2}/>
        <path d="M54 198 L85 123 L116 198" stroke={rf1} strokeWidth="2" fill="none" opacity="0.7"/>
        {([215,228,242,255] as number[]).map((y,i) => <line key={i} x1="58" y1={y} x2="112" y2={y} stroke={st3} strokeWidth="0.6" opacity="0.3"/>)}

        {/* Center tower */}
        <rect x="108" y="165" width="84" height="103" fill={st1}/>
        {([110,122,134,146,158,170,180] as number[]).map((x,i) => i%2===0 ? <rect key={x} x={x} y="153" width="12" height="16" fill={st1} rx="1"/> : null)}
        <path d="M104 163 L150 52 L196 163Z" fill={rf2}/>
        <path d="M104 163 L150 55 L196 163" stroke={rf1} strokeWidth="2.5" fill="none" opacity="0.7"/>
        {([180,198,215,232,248] as number[]).map((y,i) => <line key={i} x1="108" y1={y} x2="192" y2={y} stroke={st3} strokeWidth="0.7" opacity="0.35"/>)}

        {/* Right tower */}
        <rect x="188" y="210" width="50" height="60" fill={st2}/>
        {([190,200,210,220,230] as number[]).map((x,i) => i%2===0 ? <rect key={x} x={x} y="198" width="10" height="15" fill={st2} rx="1"/> : null)}
        <path d="M184 208 L213 128 L242 208Z" fill={rf2}/>
        <path d="M184 208 L213 131 L242 208" stroke={rf1} strokeWidth="2" fill="none" opacity="0.7"/>
        {([224,237,250,262] as number[]).map((y,i) => <line key={i} x1="188" y1={y} x2="238" y2={y} stroke={st3} strokeWidth="0.6" opacity="0.3"/>)}

        {/* === WINDOWS === */}
        {/* Left tower */}
        <ellipse cx="85" cy="228" rx="11" ry="14" fill={wDark}/>
        <ellipse cx="85" cy="228" rx="7.5" ry="10.5" fill={wGlow} opacity={d ? '0.9' : '0.6'}>
          {d && <animate attributeName="opacity" values="0.9;0.4;1;0.5;0.9" dur="3.4s" repeatCount="indefinite"/>}
        </ellipse>
        <ellipse cx="85" cy="226" rx="3.5" ry="5" fill="#FFFCF0" opacity={d ? '0.85' : '0.5'}/>

        {/* Center tower main */}
        <ellipse cx="150" cy="200" rx="14" ry="18" fill={wDark}/>
        <ellipse cx="150" cy="200" rx="10" ry="13" fill={wGlow} opacity={d ? '0.95' : '0.65'}>
          {d && <animate attributeName="opacity" values="0.95;0.35;1;0.55;0.95" dur="2.9s" repeatCount="indefinite"/>}
        </ellipse>
        <ellipse cx="150" cy="198" rx="5" ry="7" fill="#FFFCF0" opacity={d ? '0.9' : '0.55'}/>
        <line x1="150" y1="185" x2="150" y2="215" stroke={wDark} strokeWidth="2" opacity="0.65"/>
        <line x1="140" y1="199" x2="160" y2="199" stroke={wDark} strokeWidth="2" opacity="0.65"/>

        {/* Center tower sides */}
        <ellipse cx="122" cy="232" rx="10" ry="12" fill={wDark}/>
        <ellipse cx="122" cy="232" rx="7" ry="8.5" fill={wGlow} opacity={d ? '0.85' : '0.58'}>
          {d && <animate attributeName="opacity" values="0.85;0.3;0.95;0.42;0.85" dur="4.2s" repeatCount="indefinite"/>}
        </ellipse>
        <ellipse cx="122" cy="230" rx="3" ry="4.5" fill="#FFFCF0" opacity={d ? '0.75' : '0.45'}/>
        <ellipse cx="178" cy="232" rx="10" ry="12" fill={wDark}/>
        <ellipse cx="178" cy="232" rx="7" ry="8.5" fill={wGlow} opacity={d ? '0.8' : '0.55'}>
          {d && <animate attributeName="opacity" values="0.8;0.38;0.92;0.4;0.8" dur="3.8s" repeatCount="indefinite"/>}
        </ellipse>
        <ellipse cx="178" cy="230" rx="3" ry="4.5" fill="#FFFCF0" opacity={d ? '0.72' : '0.42'}/>

        {/* Right tower */}
        <ellipse cx="213" cy="232" rx="10" ry="13" fill={wDark}/>
        <ellipse cx="213" cy="232" rx="7" ry="9" fill={wGlow} opacity={d ? '0.88' : '0.6'}>
          {d && <animate attributeName="opacity" values="0.88;0.35;0.98;0.48;0.88" dur="3.6s" repeatCount="indefinite"/>}
        </ellipse>
        <ellipse cx="213" cy="230" rx="3.5" ry="5" fill="#FFFCF0" opacity={d ? '0.78' : '0.48'}/>

        {/* Wall windows */}
        <ellipse cx="100" cy="280" rx="9" ry="11" fill={wDark}/>
        <ellipse cx="100" cy="280" rx="6" ry="7.5" fill={wGlow} opacity={d ? '0.75' : '0.5'}>
          {d && <animate attributeName="opacity" values="0.75;0.28;0.88;0.38;0.75" dur="4.8s" repeatCount="indefinite"/>}
        </ellipse>
        <ellipse cx="200" cy="280" rx="9" ry="11" fill={wDark}/>
        <ellipse cx="200" cy="280" rx="6" ry="7.5" fill={wGlow} opacity={d ? '0.78' : '0.52'}>
          {d && <animate attributeName="opacity" values="0.78;0.3;0.9;0.4;0.78" dur="4.5s" repeatCount="indefinite"/>}
        </ellipse>

        {/* === GATE === */}
        <path d="M125 355 Q125 292 150 288 Q175 292 175 355Z" fill={wDark}/>
        <path d="M129 355 Q129 295 150 292 Q171 295 171 355Z" fill={d ? '#080610' : '#4A3018'}/>
        {([132,140,150,160,168] as number[]).map(x => (
          <line key={x} x1={x} y1="294" x2={x} y2="355" stroke={wDark} strokeWidth="2" opacity="0.45"/>
        ))}
        {([302,314,328,340,352] as number[]).map(y => (
          <line key={y} x1="129" y1={y} x2="171" y2={y} stroke={wDark} strokeWidth="1.5" opacity="0.35"/>
        ))}

        {/* === FLAGS === */}
        <line x1="85" y1="120" x2="85" y2="98" stroke={st3} strokeWidth="2.5"/>
        <path d="M85 98 L107 105 L85 112Z" fill={flagR}>
          <animateTransform attributeName="transform" type="rotate"
            values="0 85 98; 4 85 98; 0 85 98; -2 85 98; 0 85 98"
            dur="3.0s" repeatCount="indefinite"/>
        </path>
        <line x1="150" y1="52" x2="150" y2="27" stroke={st3} strokeWidth="3"/>
        <path d="M150 27 L176 35 L150 43Z" fill={flagR}>
          <animateTransform attributeName="transform" type="rotate"
            values="0 150 27; 5 150 27; 0 150 27; -3 150 27; 0 150 27"
            dur="3.5s" repeatCount="indefinite"/>
        </path>
        <line x1="213" y1="128" x2="213" y2="108" stroke={st3} strokeWidth="2.5"/>
        <path d="M213 108 L233 115 L213 122Z" fill={flagB}>
          <animateTransform attributeName="transform" type="rotate"
            values="0 213 108; 3 213 108; 0 213 108; -2 213 108; 0 213 108"
            dur="2.8s" repeatCount="indefinite"/>
        </path>

        {/* === MAGIC SPARKLES === */}
        {([
          [40,200,sparkA,'3.8s','0s'],
          [262,185,sparkB,'4.2s','1.0s'],
          [78,155,sparkA,'5.0s','0.5s'],
          [238,220,sparkA,'3.6s','1.8s'],
          [25,250,sparkB,'4.6s','2.2s'],
          [275,255,sparkA,'4.0s','0.8s'],
          [152,95,sparkB,'3.2s','3.0s'],
          [110,135,sparkA,'4.4s','1.5s'],
          [195,145,sparkB,'3.9s','2.8s'],
        ] as [number,number,string,string,string][]).map(([x,y,col,dur,delay],i) => (
          <circle key={i} cx={x} cy={y} r="2.8" fill={col} opacity="0">
            <animate attributeName="opacity" values="0;0.9;0.6;0.8;0" dur={dur} begin={delay} repeatCount="indefinite"/>
            <animate attributeName="cy" values={`${y};${(y as number)-22};${(y as number)-44}`} dur={dur} begin={delay} repeatCount="indefinite"/>
            <animate attributeName="r" values="2.8;1.5;0.5" dur={dur} begin={delay} repeatCount="indefinite"/>
          </circle>
        ))}

      </g>
    </svg>
  )
}

const STEPS = [
  {
    num: 'I',
    label: 'Крок перший',
    title: 'Розкажіть про дитину',
    desc: "Ім'я, вік, улюблені персонажі та настрій вечора — все, що зробить казку по-справжньому особливою.",
    revealClass: 'revealLeft',
  },
  {
    num: 'II',
    label: 'Крок другий',
    title: 'Оберіть магію',
    desc: 'Пригода в зачарованому лісі, подорож зірками чи тиха історія про дружбу — ви обираєте світ і тему.',
    revealClass: 'revealRight',
  },
  {
    num: 'III',
    label: 'Крок третій',
    title: 'Насолоджуйтесь казкою',
    desc: 'Штучний інтелект плете унікальну історію слово за словом — з ілюстраціями, готову до вечірнього читання.',
    revealClass: 'revealLeft',
  },
]

interface StepItemProps {
  step: (typeof STEPS)[number]
  index: number
}

function StepItem({ step, index }: StepItemProps) {
  const ref = useRef<HTMLDivElement>(null)
  const [visible, setVisible] = useState(false)
  const [numFlipped, setNumFlipped] = useState(false)
  const [lineDrawn, setLineDrawn] = useState(false)

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const obs = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setVisible(true)
          setTimeout(() => setNumFlipped(true), 100)
          setTimeout(() => setLineDrawn(true), 400)
          obs.unobserve(el)
        }
      },
      { threshold: 0.15, rootMargin: '0px 0px -40px 0px' }
    )
    obs.observe(el)
    return () => obs.disconnect()
  }, [])

  return (
    <div
      ref={ref}
      className={`${styles.step} ${step.revealClass} ${visible ? 'visible' : ''}`}
      style={{ transitionDelay: `${index * 0.1}s` }}
    >
      <div className={styles.stepNumWrap}>
        <div className={`${styles.stepNum} ${numFlipped ? styles.flipped : ''}`}>
          {step.num}
        </div>
      </div>
      <div className={styles.stepContent}>
        <div className={styles.stepLabel}>{step.label}</div>
        <h3 className={styles.stepTitle}>{step.title}</h3>
        <p className={styles.stepDesc}>{step.desc}</p>
      </div>
      {index < STEPS.length - 1 && (
        <div className={`${styles.stepLine} ${lineDrawn ? styles.drawn : ''}`} />
      )}
    </div>
  )
}

export function HowItWorks() {
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: illustRef, visible: illustVisible } = useReveal()

  return (
    <section className={styles.section} id="how">
      <SectionParticles />
      <div className={styles.bgIllust} aria-hidden="true">
        <svg viewBox="0 0 300 400" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
          <circle cx="200" cy="80" r="60" fill="url(#howMG)" opacity="0.4"/>
          <path d="M215 55 A35 35 0 1 0 215 105 A25 25 0 1 1 215 55Z" fill="#EDD9A3" opacity="0.5"/>
          <path d="M0 120 C40 110 60 140 100 125 C120 118 130 130 150 120" stroke="#6B4C3B" strokeWidth="1.2" fill="none" opacity="0.25"/>
          <path d="M100 125 C105 105 115 95 125 80" stroke="#6B4C3B" strokeWidth="0.8" fill="none" opacity="0.2"/>
          <ellipse cx="125" cy="78" rx="5" ry="10" transform="rotate(-20 125 78)" fill="#166534" opacity="0.15"/>
          <path d="M20 400 C25 350 35 300 30 250 C28 220 20 180 0 150" stroke="#6B4C3B" strokeWidth="2" fill="none" opacity="0.15"/>
          <circle cx="80" cy="200" r="3" fill="#F59E0B" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.15;0.5" dur="3s" repeatCount="indefinite"/>
          </circle>
          <circle cx="180" cy="260" r="2.5" fill="#EDD9A3" opacity="0.45">
            <animate attributeName="opacity" values="0.45;0.1;0.45" dur="4s" repeatCount="indefinite"/>
          </circle>
          <path d="M250 40L251 36L255 38L251 35L250 31L249 35L245 33L249 36Z" fill="#C4B5FD" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.2;0.5" dur="4s" repeatCount="indefinite"/>
          </path>
          <defs>
            <radialGradient id="howMG" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0%" stopColor="#EDD9A3" stopOpacity="0.35"/>
              <stop offset="100%" stopColor="#EDD9A3" stopOpacity="0"/>
            </radialGradient>
          </defs>
        </svg>
      </div>
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>Як це працює</div>
          <div className={styles.title}>Три кроки до чарівної казки</div>
        </div>

        <div className={styles.layout}>
          <div className={styles.steps}>
            {STEPS.map((step, i) => (
              <StepItem key={i} step={step} index={i} />
            ))}
          </div>

          <div
            ref={illustRef}
            className={`${styles.illustWrap} reveal ${illustVisible ? 'visible' : ''}`}
          >
            <CastleIllustration />
          </div>
        </div>
      </div>
    </section>
  )
}
