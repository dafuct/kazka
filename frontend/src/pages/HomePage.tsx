import { useEffect, useRef, Fragment } from 'react'
import { useTheme } from '../lib/ThemeContext'
import { HowItWorks } from '../components/home/HowItWorks'
import { Features } from '../components/home/Features'
import { StoryPreview } from '../components/home/StoryPreview'
import { ArchiveShelf } from '../components/home/ArchiveShelf'
import { NightCta } from '../components/home/NightCta'
import { useLocale } from '../lib/LocaleContext'
import { useStoryModal } from '../lib/StoryModalContext'
import { handleRipple } from '../lib/ripple'
import styles from './HomePage.module.css'

const HEADLINE = 'Казка яка чекає саме на вашу дитину'

function ParticleField() {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const field = ref.current
    if (!field) return
    const types = ['star', 'dot', 'circle', 'dash'] as const
    for (let i = 0; i < 50; i++) {
      const el = document.createElement('div')
      const type = types[i % 4]
      el.className = `${styles.particle} ${styles['particle_' + type]}`
      const x = Math.random() * 100
      const y = Math.random() * 100
      const dur = 8 + Math.random() * 12
      const delay = Math.random() * 10
      const opacity = 0.3 + Math.random() * 0.5
      el.style.cssText = `left:${x}%;top:${y}%;--p-opacity:${opacity};`
      if (type === 'star') {
        el.innerHTML = '<svg width="12" height="12" viewBox="0 0 16 16"><path d="M8 0L9.5 6.5L16 8L9.5 9.5L8 16L6.5 9.5L0 8L6.5 6.5Z" fill="currentColor"/></svg>'
        el.style.animation = `orbitalDrift ${dur}s ease-in-out ${delay}s infinite`
      } else if (type === 'dot') {
        const s = 2 + Math.random() * 3
        el.style.width = s + 'px'
        el.style.height = s + 'px'
        el.style.animation = `floatUp ${dur}s linear ${delay}s infinite`
      } else if (type === 'circle') {
        const s = 6 + Math.random() * 10
        el.style.width = s + 'px'
        el.style.height = s + 'px'
        el.style.animation = `expandFade ${dur * 0.6}s ease-out ${delay}s infinite`
      } else {
        el.style.width = (8 + Math.random() * 12) + 'px'
        el.style.height = '2px'
        el.style.animation = `floatUp ${dur}s linear ${delay}s infinite`
      }
      field.appendChild(el)
    }
    return () => { field.innerHTML = '' }
  }, [])
  return <div ref={ref} className={styles.particleField} aria-hidden="true" />
}

function HeroIllustration() {
  const { theme } = useTheme()
  const d = theme === 'dark'

  // ─── Colour palette ───────────────────────────────────────────────────
  const skyTop  = d ? '#0A1B38' : '#5AACCE'
  const skyHori = d ? '#0D2840' : '#72B8E0'
  const bgT1    = d ? '#060C14' : '#1E3E28'
  const bgT2    = d ? '#091824' : '#284A30'
  const bgT3    = d ? '#0E2232' : '#325838'
  const hazeC   = d ? '#1A4060' : '#A8D8F0'

  const trunkD  = d ? '#3A1A06' : '#5A2C08'
  const trunkM  = d ? '#582E10' : '#7A4416'

  const leafBk  = d ? '#092A0A' : '#1C4A18'
  const leafD   = d ? '#133218' : '#286024'
  const leafM   = d ? '#1E4A20' : '#368030'
  const leafL   = d ? '#2C6A28' : '#4C9E3A'
  const leafBr  = d ? '#3C8830' : '#62C040'

  const gndBk   = d ? '#040808' : '#142210'
  const gndD    = d ? '#070E08' : '#1E3A1C'
  const gndM    = d ? '#0C1A0C' : '#285C28'
  const clearC  = d ? '#0E2210' : '#2E6028'
  const grassH  = d ? '#1A4A1A' : '#4A8038'

  const foxBody = '#C05818'
  const foxLght = '#E07028'
  const foxDark = '#7C3006'
  const foxBell = '#F2D498'
  const foxWht  = '#FFF8F0'

  const mushC   = d ? '#24D8A8' : '#D85808'
  const mushBrt = d ? '#80FFE8' : '#FF8828'
  const mushStm = d ? '#68C0A8' : '#C8A860'

  const moonC   = '#E0ECFF'
  const starC   = '#D8E8FF'

  return (
    <svg viewBox="0 0 520 390" fill="none" xmlns="http://www.w3.org/2000/svg"
      className={styles.heroSvg} aria-hidden="true">
      <defs>
        <clipPath id="fcp"><rect width="520" height="390" rx="16"/></clipPath>
      </defs>

      <g clipPath="url(#fcp)">
        {/* ─── SKY ─────────────────────────────────────────────────────── */}
        <rect width="520" height="390" fill={skyTop}/>
        <rect y="180" width="520" height="120" fill={skyHori} opacity="0.3"/>

        {/* ─── MOON (night) ─────────────────────────────────────────────── */}
        {d && (<>
          <circle cx="420" cy="65" r="30" fill={moonC}/>
          <circle cx="433" cy="56" r="25" fill={skyTop}/>
          {/* 4-pointed stars */}
          {([
            [300,18,5.5],[340,38,4],[378,18,4.5],[440,40,4],[468,18,5],
            [496,34,3.5],[50,22,4],[92,14,5],[140,30,3.5],[186,20,4],
            [226,38,3],[265,16,4.5],[490,56,3],
          ] as [number,number,number][]).map(([x,y,s],i) => (
            <path key={i}
              d={`M${x} ${y-s} L${x+s*.38} ${y-s*.38} L${x+s} ${y} L${x+s*.38} ${y+s*.38} L${x} ${y+s} L${x-s*.38} ${y+s*.38} L${x-s} ${y} L${x-s*.38} ${y-s*.38}Z`}
              fill={starC} opacity="0.85">
              <animate attributeName="opacity" values="0.85;0.25;0.85" dur={`${2.2+i*0.33}s`} begin={`${i*0.28}s`} repeatCount="indefinite"/>
            </path>
          ))}
          {/* Tiny star dots */}
          {([
            [110,45,1.3],[162,42,1],[240,26,1.2],[290,44,1],[346,28,1.1],[396,50,1],
            [448,26,1.2],[78,38,1],[200,48,0.9],[258,34,1],[408,28,0.9],
          ] as [number,number,number][]).map(([x,y,r],i) => (
            <circle key={i} cx={x} cy={y} r={r} fill={starC} opacity="0.65"/>
          ))}
          {/* Diamond sparkle */}
          <path d="M488 320 L490 312 L492 320 L490 328Z" fill={starC} opacity="0.9">
            <animate attributeName="opacity" values="0.9;0.2;0.9" dur="3.5s" begin="0.5s" repeatCount="indefinite"/>
          </path>
        </>)}

        {/* ─── SUN (light) ──────────────────────────────────────────────── */}
        {!d && (<>
          <g>
            <animateTransform attributeName="transform" type="rotate" values="0,420,65;360,420,65" dur="80s" repeatCount="indefinite"/>
            {Array.from({length:12},(_,i) => {
              const a=(i*30)*Math.PI/180, r1=40, r2=i%2===0?66:54
              return <line key={i}
                x1={420+Math.cos(a)*r1} y1={65+Math.sin(a)*r1}
                x2={420+Math.cos(a)*r2} y2={65+Math.sin(a)*r2}
                stroke="#F8C200" strokeWidth={i%2===0?3.5:2.2} strokeLinecap="round"/>
            })}
          </g>
          <circle cx="420" cy="65" r="32" fill="#FFE200"/>
          <circle cx="420" cy="65" r="24" fill="#FFF060"/>
          <circle cx="412" cy="57" r="9" fill="#FFFAA0" opacity="0.5"/>
        </>)}

        {/* ─── DEPTH HAZE ───────────────────────────────────────────────── */}
        <ellipse cx="260" cy="215" rx="240" ry="75" fill={hazeC} opacity={d ? '0.12' : '0.07'}/>

        {/* ─── BACKGROUND FOREST SILHOUETTES ───────────────────────────── */}
        {/* Far left edge trees */}
        <ellipse cx="35" cy="165" rx="65" ry="80" fill={bgT1}/>
        <rect x="20" y="208" width="30" height="108" fill={bgT1}/>
        <ellipse cx="-2" cy="188" rx="50" ry="65" fill={bgT1}/>
        <rect x="-18" y="226" width="28" height="90" fill={bgT1}/>
        {/* Far right edge trees */}
        <ellipse cx="482" cy="158" rx="68" ry="78" fill={bgT1}/>
        <rect x="468" y="202" width="30" height="112" fill={bgT1}/>
        <ellipse cx="522" cy="182" rx="55" ry="68" fill={bgT1}/>
        <rect x="504" y="224" width="28" height="90" fill={bgT1}/>
        {/* Mid-distance trees */}
        <ellipse cx="195" cy="190" rx="44" ry="56" fill={bgT2}/>
        <rect x="183" y="232" width="22" height="72" fill={bgT2}/>
        <ellipse cx="352" cy="184" rx="50" ry="60" fill={bgT2}/>
        <rect x="338" y="230" width="28" height="74" fill={bgT2}/>
        {/* Furthest centre tree (lightest = most distant) */}
        <ellipse cx="272" cy="200" rx="38" ry="50" fill={bgT3}/>
        <rect x="263" y="236" width="18" height="58" fill={bgT3}/>

        {/* ─── GROUND LAYERS ───────────────────────────────────────────── */}
        <path d="M0 268 Q130 254 260 261 Q390 268 520 255 L520 390 L0 390Z" fill={gndBk}/>
        <path d="M0 292 Q130 280 260 288 Q390 294 520 282 L520 390 L0 390Z" fill={gndD}/>
        <path d="M0 316 Q130 306 260 312 Q390 318 520 308 L520 390 L0 390Z" fill={gndM}/>
        {/* Lit clearing where fox sits */}
        <ellipse cx="350" cy="326" rx="165" ry="54" fill={clearC} opacity="0.72"/>
        <ellipse cx="345" cy="316" rx="120" ry="32" fill={grassH} opacity="0.38"/>
        {/* Night: mushroom ambient glow on ground */}
        {d && <>
          <ellipse cx="80" cy="340" rx="35" ry="12" fill={mushC} opacity="0.07">
            <animate attributeName="opacity" values="0.07;0.02;0.09;0.03;0.07" dur="3.8s" begin="0.2s" repeatCount="indefinite"/>
          </ellipse>
          <ellipse cx="440" cy="336" rx="32" ry="11" fill={mushC} opacity="0.07">
            <animate attributeName="opacity" values="0.07;0.02;0.09;0.03;0.07" dur="4.2s" begin="1.5s" repeatCount="indefinite"/>
          </ellipse>
        </>}

        {/* ─── MAIN TREE TRUNK & BRANCHES ──────────────────────────────── */}
        {/* Main trunk body */}
        <path d="M105 340 Q114 298 120 256 Q125 220 128 188 Q130 162 132 144 L148 142 Q150 160 152 188 Q154 220 158 256 Q162 298 168 340Z" fill={trunkM}/>
        <path d="M108 340 Q116 298 122 256 Q127 222 130 190 Q132 164 133 146" stroke={trunkD} strokeWidth="11" fill="none" strokeLinecap="round" opacity="0.55"/>
        {/* Branch A — sweeping left */}
        <path d="M132 196 Q106 162 78 136 Q54 112 28 96" stroke={trunkM} strokeWidth="17" fill="none" strokeLinecap="round"/>
        <path d="M133 197 Q109 165 82 140 Q58 116 34 102" stroke={trunkD} strokeWidth="7" fill="none" strokeLinecap="round" opacity="0.45"/>
        {/* Branch C — sweeping right */}
        <path d="M148 186 Q174 158 206 136 Q234 116 262 104" stroke={trunkM} strokeWidth="11" fill="none" strokeLinecap="round"/>
        {/* Branch D — lower left */}
        <path d="M128 240 Q106 226 84 218 Q66 212 46 210" stroke={trunkM} strokeWidth="9" fill="none" strokeLinecap="round"/>

        {/* ─── FOLIAGE BLOBS (5 layers, darkest → brightest) ────────────── */}
        {/* Layer 1 — base shadow */}
        <ellipse cx="128" cy="152" rx="92" ry="78" fill={leafBk}/>
        <ellipse cx="44" cy="130" rx="58" ry="52" fill={leafBk}/>
        <ellipse cx="214" cy="160" rx="64" ry="54" fill={leafBk}/>
        <ellipse cx="150" cy="88" rx="56" ry="50" fill={leafBk}/>
        <ellipse cx="76" cy="112" rx="52" ry="44" fill={leafBk}/>
        <ellipse cx="226" cy="120" rx="48" ry="42" fill={leafBk}/>
        {/* Layer 2 — dark */}
        <ellipse cx="125" cy="154" rx="80" ry="68" fill={leafD}/>
        <ellipse cx="48" cy="132" rx="50" ry="46" fill={leafD}/>
        <ellipse cx="210" cy="162" rx="56" ry="48" fill={leafD}/>
        <ellipse cx="152" cy="92" rx="50" ry="44" fill={leafD}/>
        <ellipse cx="80" cy="114" rx="44" ry="38" fill={leafD}/>
        <ellipse cx="222" cy="124" rx="42" ry="36" fill={leafD}/>
        <ellipse cx="164" cy="144" rx="40" ry="34" fill={leafD}/>
        {/* Layer 3 — medium */}
        <ellipse cx="122" cy="156" rx="68" ry="60" fill={leafM}/>
        <ellipse cx="52" cy="134" rx="42" ry="38" fill={leafM}/>
        <ellipse cx="204" cy="165" rx="48" ry="42" fill={leafM}/>
        <ellipse cx="154" cy="96" rx="44" ry="38" fill={leafM}/>
        <ellipse cx="86" cy="118" rx="38" ry="32" fill={leafM}/>
        <ellipse cx="218" cy="128" rx="36" ry="30" fill={leafM}/>
        <ellipse cx="170" cy="148" rx="34" ry="28" fill={leafM}/>
        <ellipse cx="108" cy="134" rx="36" ry="30" fill={leafM}/>
        {/* Layer 4 — light */}
        <ellipse cx="118" cy="144" rx="52" ry="46" fill={leafL}/>
        <ellipse cx="58" cy="126" rx="34" ry="30" fill={leafL}/>
        <ellipse cx="198" cy="155" rx="38" ry="34" fill={leafL}/>
        <ellipse cx="156" cy="84" rx="36" ry="32" fill={leafL}/>
        <ellipse cx="92" cy="110" rx="32" ry="26" fill={leafL}/>
        <ellipse cx="178" cy="136" rx="30" ry="26" fill={leafL}/>
        <ellipse cx="112" cy="122" rx="30" ry="24" fill={leafL}/>
        {/* Layer 5 — bright highlights */}
        <ellipse cx="112" cy="134" rx="32" ry="26" fill={leafBr} opacity="0.8"/>
        <ellipse cx="62" cy="118" rx="22" ry="18" fill={leafBr} opacity="0.7"/>
        <ellipse cx="160" cy="78" rx="24" ry="20" fill={leafBr} opacity="0.75"/>
        <ellipse cx="192" cy="146" rx="24" ry="20" fill={leafBr} opacity="0.65"/>
        <ellipse cx="98" cy="104" rx="20" ry="16" fill={leafBr} opacity="0.65"/>
        <ellipse cx="140" cy="120" rx="26" ry="20" fill={leafBr} opacity="0.6"/>
        {/* Low-hanging foliage */}
        <ellipse cx="68" cy="196" rx="34" ry="26" fill={leafD}/>
        <ellipse cx="65" cy="196" rx="26" ry="19" fill={leafM}/>
        <ellipse cx="60" cy="192" rx="16" ry="12" fill={leafL}/>
        <ellipse cx="200" cy="202" rx="32" ry="22" fill={leafD}/>
        <ellipse cx="196" cy="200" rx="24" ry="16" fill={leafM}/>

        {/* ─── BACKGROUND FERNS ─────────────────────────────────────────── */}
        {[
          ['M310 290 Q298 272 294 260','M310 290 Q303 268 308 258','M310 290 Q319 271 322 260'],
          ['M390 282 Q380 266 376 255','M390 282 Q385 262 390 252','M390 282 Q398 264 402 255'],
        ].map((ps,gi) => (
          <g key={gi} opacity={d?'0.6':'0.7'}>
            {ps.map((p,pi) => <path key={pi} d={p} stroke={d?'#162C1A':'#1E4420'} strokeWidth="2.5" fill="none" strokeLinecap="round"/>)}
          </g>
        ))}

        {/* ─── MUSHROOMS ────────────────────────────────────────────────── */}
        {/* Left cluster */}
        <g transform="translate(55,322)">
          {d && <ellipse cx="0" cy="-2" rx="24" ry="16" fill={mushC} opacity="0.15">
            <animate attributeName="opacity" values="0.15;0.04;0.18;0.05;0.15" dur="3.5s" begin="0.4s" repeatCount="indefinite"/>
          </ellipse>}
          <ellipse cx="0" cy="0" rx="16" ry="10" fill={mushC}/>
          <ellipse cx="0" cy="-2" rx="13" ry="7.5" fill={mushBrt} opacity="0.75"/>
          <circle cx="-4" cy="-3" r="2.5" fill={mushBrt} opacity="0.55"/>
          <circle cx="4" cy="-5" r="1.8" fill={mushBrt} opacity="0.5"/>
          <rect x="-4.5" y="9" width="9" height="18" rx="2" fill={mushStm}/>
        </g>
        <g transform="translate(80,332)">
          {d && <ellipse cx="0" cy="-1" rx="16" ry="10" fill={mushC} opacity="0.12">
            <animate attributeName="opacity" values="0.12;0.03;0.15;0.04;0.12" dur="4s" begin="1s" repeatCount="indefinite"/>
          </ellipse>}
          <ellipse cx="0" cy="0" rx="10" ry="6.5" fill={mushC}/>
          <ellipse cx="0" cy="-1" rx="8" ry="5" fill={mushBrt} opacity="0.75"/>
          <rect x="-3" y="5" width="6" height="13" rx="1.5" fill={mushStm}/>
        </g>
        <g transform="translate(34,336)">
          <ellipse cx="0" cy="0" rx="9" ry="5.5" fill={mushC}/>
          <rect x="-2.5" y="4" width="5" height="11" rx="1" fill={mushStm}/>
        </g>
        {/* Right cluster */}
        <g transform="translate(448,318)">
          {d && <ellipse cx="0" cy="-2" rx="26" ry="17" fill={mushC} opacity="0.15">
            <animate attributeName="opacity" values="0.15;0.04;0.20;0.06;0.15" dur="4s" begin="1.3s" repeatCount="indefinite"/>
          </ellipse>}
          <ellipse cx="0" cy="0" rx="18" ry="11" fill={mushC}/>
          <ellipse cx="0" cy="-2" rx="14" ry="8" fill={mushBrt} opacity="0.75"/>
          <circle cx="-5" cy="-4" r="2.8" fill={mushBrt} opacity="0.55"/>
          <circle cx="5" cy="-5" r="2" fill={mushBrt} opacity="0.5"/>
          <rect x="-5" y="10" width="10" height="20" rx="2.5" fill={mushStm}/>
        </g>
        <g transform="translate(474,330)">
          {d && <ellipse cx="0" cy="-1" rx="14" ry="9" fill={mushC} opacity="0.10">
            <animate attributeName="opacity" values="0.10;0.03;0.13;0.04;0.10" dur="3.8s" begin="2.2s" repeatCount="indefinite"/>
          </ellipse>}
          <ellipse cx="0" cy="0" rx="9" ry="6" fill={mushC}/>
          <ellipse cx="0" cy="-1" rx="7" ry="4.5" fill={mushBrt} opacity="0.75"/>
          <rect x="-2.5" y="5" width="5" height="12" rx="1.5" fill={mushStm}/>
        </g>
        <g transform="translate(422,334)">
          <ellipse cx="0" cy="0" rx="8" ry="5" fill={mushC}/>
          <rect x="-2" y="4" width="4" height="10" rx="1" fill={mushStm}/>
        </g>

        {/* ─── FOX ──────────────────────────────────────────────────────── */}
        {/* Shadow */}
        <ellipse cx="358" cy="346" rx="50" ry="13" fill={gndBk} opacity="0.45"/>

        {/* Tail (animated sway) */}
        <g>
          <animateTransform attributeName="transform" type="rotate" values="-4,385,318;4,385,318;-4,385,318" dur="3.5s" repeatCount="indefinite"/>
          <path d="M392 308 Q420 292 440 268 Q456 248 452 228 Q448 210 433 208 Q418 206 417 220 Q416 232 428 238 Q437 242 432 258 Q422 274 402 284 Q390 292 392 305Z" fill={foxBody}/>
          <path d="M396 304 Q422 288 440 265 Q454 247 451 229 Q448 214 436 212 Q424 210 423 222 Q421 234 432 239 Q436 242 432 257 Q423 272 404 282 Q393 289 394 302Z" fill={foxLght} opacity="0.45"/>
          <ellipse cx="434" cy="212" rx="16" ry="14" fill={foxBell}/>
          <ellipse cx="433" cy="210" rx="12" ry="10" fill={foxWht} opacity="0.78"/>
        </g>

        {/* Body */}
        <ellipse cx="356" cy="300" rx="32" ry="42" fill={foxBody}/>
        <ellipse cx="356" cy="315" rx="18" ry="28" fill={foxBell} opacity="0.92"/>
        <path d="M330 285 Q356 268 382 285 Q372 274 356 272 Q340 274 330 285Z" fill={foxDark} opacity="0.3"/>

        {/* Head */}
        <circle cx="356" cy="254" r="30" fill={foxBody}/>
        <path d="M333 245 Q356 228 380 245 Q371 235 356 234 Q341 235 333 245Z" fill={foxDark} opacity="0.28"/>

        {/* Ears */}
        <path d="M336 240 L329 212 L352 234Z" fill={foxBody}/>
        <path d="M338 238 L333 214 L350 232Z" fill={foxDark} opacity="0.52"/>
        <path d="M377 240 L384 212 L361 234Z" fill={foxBody}/>
        <path d="M375 238 L380 214 L363 232Z" fill={foxDark} opacity="0.52"/>

        {/* Muzzle */}
        <ellipse cx="356" cy="267" rx="15" ry="11" fill={foxBell}/>
        <ellipse cx="356" cy="269" rx="12" ry="8" fill={foxWht} opacity="0.6"/>

        {/* Eyes — glowing teal at night, warm brown in day */}
        <circle cx="345" cy="251" r="7" fill={d ? '#B8F8D8' : '#E8F4FF'}/>
        <circle cx="345" cy="251" r="5" fill={d ? '#28B868' : '#3A2810'}/>
        <circle cx="345" cy="251" r="2.8" fill={d ? '#0A5C28' : '#1A1008'}/>
        <circle cx="343.5" cy="249.5" r="1.5" fill="#FFFFFF" opacity="0.9"/>
        <circle cx="368" cy="251" r="7" fill={d ? '#B8F8D8' : '#E8F4FF'}/>
        <circle cx="368" cy="251" r="5" fill={d ? '#28B868' : '#3A2810'}/>
        <circle cx="368" cy="251" r="2.8" fill={d ? '#0A5C28' : '#1A1008'}/>
        <circle cx="366.5" cy="249.5" r="1.5" fill="#FFFFFF" opacity="0.9"/>

        {/* Nose + mouth */}
        <path d="M352 263 L356 267 L360 263 Q356 260 352 263Z" fill="#2A1010"/>
        <path d="M352 267 Q356 272 360 267" stroke="#481818" strokeWidth="1.3" fill="none" strokeLinecap="round"/>

        {/* Front paws */}
        <ellipse cx="338" cy="336" rx="14" ry="9" fill={foxBody}/>
        <ellipse cx="374" cy="336" rx="14" ry="9" fill={foxBody}/>
        <ellipse cx="338" cy="340" rx="10" ry="5.5" fill={foxDark} opacity="0.28"/>
        <ellipse cx="374" cy="340" rx="10" ry="5.5" fill={foxDark} opacity="0.28"/>

        {/* ─── FIREFLIES (night only) ────────────────────────────────────── */}
        {d && ([
          [135,218,0],[168,205,0.9],[232,242,1.7],[300,215,0.4],
          [318,248,1.3],[246,188,2.1],[456,272,0.2],[486,244,1.9],
          [264,264,1.1],[198,248,0.6],[270,232,1.5],
        ] as [number,number,number][]).map(([x,y,t],i) => {
          const col = i%3===0 ? '#A0FFD0' : i%3===1 ? '#D8FFB0' : '#B0F8D8'
          return <circle key={i} cx={x} cy={y} r="1.8" fill={col} opacity="0.85">
            <animate attributeName="opacity" values="0.85;0.08;0.9;0.08;0.85" dur="2.8s" begin={`${t}s`} repeatCount="indefinite"/>
          </circle>
        })}

        {/* ─── GRASS TUFTS ──────────────────────────────────────────────── */}
        {([
          [120,350],[165,358],[225,352],[296,362],[322,354],[408,348],[468,356],
        ] as [number,number][]).map(([x,y],i) => (
          <g key={i}>
            <path d={`M${x} ${y} Q${x-3} ${y-9} ${x-7} ${y-13}`} stroke={grassH} strokeWidth="1.5" fill="none" strokeLinecap="round"/>
            <path d={`M${x} ${y} Q${x+1} ${y-10} ${x} ${y-15}`} stroke={grassH} strokeWidth="1.5" fill="none" strokeLinecap="round"/>
            <path d={`M${x} ${y} Q${x+4} ${y-8} ${x+7} ${y-11}`} stroke={grassH} strokeWidth="1.5" fill="none" strokeLinecap="round"/>
          </g>
        ))}


      </g>
    </svg>
  )
}

export function HomePage() {
  const { t } = useLocale()
  const { openModal } = useStoryModal()
  const heroImgRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    let ticking = false
    const onScroll = () => {
      if (!ticking) {
        requestAnimationFrame(() => {
          if (heroImgRef.current) {
            heroImgRef.current.style.transform = `translateY(${window.scrollY * 0.12}px)`
          }
          ticking = false
        })
        ticking = true
      }
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div>
      {/* ── HERO ── */}
      <section className={styles.hero}>
        <ParticleField />
        <div className={styles.heroInner}>
          <div className={styles.heroText}>
            <div className={styles.heroLabel}>✦ Персональні казки для вашої дитини</div>
            <h1 className={styles.heroHeadline}>
              {HEADLINE.split(' ').map((word, i) => (
                <Fragment key={i}>
                  <span
                    className={styles.heroWord}
                    style={{ animationDelay: `${i * 0.1}s` }}
                  >{word}</span>{' '}
                </Fragment>
              ))}
            </h1>
            <p className={styles.heroSub}>
              Кожен вечір — унікальна історія, створена штучним інтелектом з теплом і турботою. Казка, яка знає ім'я вашої дитини, пам'ятає її мрії та росте разом із нею.
            </p>
            <div className={styles.heroButtons}>
              <a
                href="#"
                className={styles.btnPrimary}
                onClick={(e) => { e.preventDefault(); openModal(); handleRipple(e) }}
              >
                {t.home.cta} →
              </a>
              <a href="#preview" className={styles.btnSecondary}>
                Переглянути приклад
              </a>
            </div>
            <div className={styles.heroProof}>
              <strong>★ 4.9</strong> · 2&thinsp;400+ казок створено цього місяця
            </div>
          </div>

          <div ref={heroImgRef} className={styles.heroImageWrap}>
            <div className={styles.heroImage}>
              <HeroIllustration />
            </div>
          </div>
        </div>
      </section>

      <HowItWorks />
      <Features />
      <StoryPreview />
      <ArchiveShelf />
      <NightCta />
    </div>
  )
}
