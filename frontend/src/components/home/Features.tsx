import { useReveal } from '../../lib/useReveal'
import { useLocale } from '../../lib/LocaleContext'
import { useTheme } from '../../lib/ThemeContext'
import { SectionParticles } from './SectionParticles'
import styles from './Features.module.css'

function MagicBookSvg({ dark }: { dark: boolean }) {
  const bookSpine  = dark ? '#5A1F28' : '#7C3006'
  const pageColor  = dark ? '#1C1838' : '#FFFBF0'
  const pageLine   = dark ? '#5A1F28' : '#C4A882'
  const textLine   = dark ? '#5B3A8A' : '#A0835E'

  return (
    <svg viewBox="0 0 360 260" fill="none" xmlns="http://www.w3.org/2000/svg"
      style={{ width: '100%', height: '100%', maxHeight: '260px' }} aria-hidden="true">

      {/* Ambient glow */}
      <ellipse cx="180" cy="155" rx="135" ry="75" fill={dark ? '#7E2A33' : '#D97706'} opacity="0.09"/>

      {/* Book shadow */}
      <ellipse cx="180" cy="210" rx="105" ry="11" fill={dark ? '#000' : '#2C1810'} opacity="0.18"/>

      {/* ── Left page ── */}
      <path d="M166 74 Q96 77 66 83 L64 196 Q96 193 166 196Z" fill={pageColor}/>
      <path d="M166 74 Q96 77 66 83 L64 196 Q96 193 166 196Z"
        fill="none" stroke={pageLine} strokeWidth="1"/>
      {[99,111,123,135,147,159,171,183].map((y, i) => (
        <path key={i} d={`M82 ${y} Q${112+i*1.5} ${y-1} ${152} ${y}`}
          stroke={textLine} strokeWidth="1.1" strokeLinecap="round"
          opacity={0.22 + (i % 3) * 0.09}/>
      ))}
      <path d="M82 195 Q103 194 128 195" stroke={textLine} strokeWidth="1.1" strokeLinecap="round" opacity="0.15"/>

      {/* ── Spine ── */}
      <rect x="163" y="72" width="34" height="126" rx="4" fill={bookSpine}/>
      <rect x="165" y="74" width="5" height="122" rx="2" fill={dark ? '#B5763A' : '#B45309'} opacity="0.45"/>
      <rect x="185" y="74" width="2" height="122" rx="1" fill={dark ? '#7E2A33' : '#92400E'} opacity="0.3"/>

      {/* ── Right page ── */}
      <path d="M197 74 Q267 77 297 83 L299 196 Q267 193 197 196Z" fill={pageColor}/>
      <path d="M197 74 Q267 77 297 83 L299 196 Q267 193 197 196Z"
        fill="none" stroke={pageLine} strokeWidth="1"/>

      {/* Fox on right page */}
      {/* tail (behind body) */}
      <path d="M252 163 Q278 148 272 126 Q267 108 256 114 Q252 121 258 130 Q263 138 255 148 Q251 156 252 163Z"
        fill="#C05818" opacity="0.8"/>
      <ellipse cx="266" cy="118" rx="11" ry="10" fill="#F2D498" opacity="0.85"/>
      {/* body */}
      <ellipse cx="230" cy="168" rx="21" ry="26" fill="#C05818" opacity="0.9"/>
      <ellipse cx="230" cy="178" rx="13" ry="16" fill="#F2D498" opacity="0.95"/>
      {/* head */}
      <circle cx="230" cy="139" r="19" fill="#C05818" opacity="0.9"/>
      {/* ear shadows */}
      <path d="M219 131 L213 111 L225 129Z" fill="#7C3006" opacity="0.35"/>
      <path d="M241 131 L247 111 L235 129Z" fill="#7C3006" opacity="0.35"/>
      {/* ears */}
      <path d="M218 130 L213 112 L225 128Z" fill="#C05818" opacity="0.9"/>
      <path d="M242 130 L247 112 L235 128Z" fill="#C05818" opacity="0.9"/>
      {/* muzzle */}
      <ellipse cx="230" cy="146" rx="10" ry="7.5" fill="#F2D498" opacity="0.95"/>
      {/* eyes */}
      <circle cx="223" cy="136" r="3.2" fill={dark ? '#B8F8D8' : '#E8F4FF'}/>
      <circle cx="237" cy="136" r="3.2" fill={dark ? '#B8F8D8' : '#E8F4FF'}/>
      <circle cx="223" cy="136" r="2" fill={dark ? '#28B868' : '#3A2810'}/>
      <circle cx="237" cy="136" r="2" fill={dark ? '#28B868' : '#3A2810'}/>
      <circle cx="222.2" cy="135.2" r="0.9" fill="#FFF" opacity="0.9"/>
      <circle cx="236.2" cy="135.2" r="0.9" fill="#FFF" opacity="0.9"/>
      {/* nose + mouth */}
      <path d="M227 142 L230 145 L233 142 Q230 140 227 142Z" fill="#2A1010"/>
      <path d="M227 145 Q230 149 233 145" stroke="#481818" strokeWidth="1.2" fill="none" strokeLinecap="round"/>
      {/* paws */}
      <ellipse cx="220" cy="190" rx="9" ry="5.5" fill="#C05818" opacity="0.88"/>
      <ellipse cx="240" cy="190" rx="9" ry="5.5" fill="#C05818" opacity="0.88"/>

      {/* tiny text lines below fox */}
      <path d="M207 184 Q224 183 258 184" stroke={textLine} strokeWidth="1" strokeLinecap="round" opacity="0.18"/>
      <path d="M207 192 Q227 191 252 192" stroke={textLine} strokeWidth="1" strokeLinecap="round" opacity="0.13"/>

      {/* ── Sparkles floating up ── */}
      {([
        [115, 62, 3.5, '#F59E0B', 0],
        [148, 46, 2.8, '#E6C77A', 0.5],
        [168, 54, 4.2, '#F59E0B', 0.9],
        [194, 42, 3, '#E6C77A', 0.25],
        [218, 56, 3.8, '#F59E0B', 1.1],
        [244, 44, 2.5, '#E6C77A', 0.65],
        [158, 33, 2.2, '#F59E0B', 1.4],
        [202, 34, 2.8, '#E6C77A', 0.85],
      ] as [number,number,number,string,number][]).map(([x,y,s,color,delay], i) => (
        <path key={i}
          d={`M${x} ${y-s} L${x+s*.38} ${y-s*.38} L${x+s} ${y} L${x+s*.38} ${y+s*.38} L${x} ${y+s} L${x-s*.38} ${y+s*.38} L${x-s} ${y} L${x-s*.38} ${y-s*.38}Z`}
          fill={color} opacity="0.85">
          <animate attributeName="opacity" values="0.85;0.12;0.85"
            dur={`${2.2+i*0.32}s`} begin={`${delay}s`} repeatCount="indefinite"/>
        </path>
      ))}

      {/* Rising dust particles */}
      {([145,163,180,198,216] as number[]).map((x, i) => (
        <circle key={i} cx={x} cy={66} r="1.8"
          fill={i % 2 === 0 ? '#F59E0B' : '#E6C77A'} opacity="0.35">
          <animate attributeName="cy" values="66;28;66"
            dur={`${3.2+i*0.4}s`} begin={`${i*0.28}s`} repeatCount="indefinite"/>
          <animate attributeName="opacity" values="0.35;0;0.35"
            dur={`${3.2+i*0.4}s`} begin={`${i*0.28}s`} repeatCount="indefinite"/>
        </circle>
      ))}
    </svg>
  )
}

function DayCycleSvg({ dark }: { dark: boolean }) {
  return (
    <svg viewBox="0 0 110 200" fill="none" xmlns="http://www.w3.org/2000/svg"
      style={{ width: '100%', height: '100%', transform: 'translateX(-10px)' }} aria-hidden="true">
      {/* ── Sun (top half) ── */}
      <g>
        <animateTransform attributeName="transform" type="rotate"
          values="0,55,44;360,55,44" dur="22s" repeatCount="indefinite"/>
        {Array.from({length:10},(_,i) => {
          const a=(i*36)*Math.PI/180
          return <line key={i}
            x1={55+Math.cos(a)*22} y1={44+Math.sin(a)*22}
            x2={55+Math.cos(a)*(i%2?29:34)} y2={44+Math.sin(a)*(i%2?29:34)}
            stroke="#F59E0B" strokeWidth={i%2?1.5:2.2} strokeLinecap="round" opacity="0.9"/>
        })}
      </g>
      <circle cx="55" cy="44" r="16" fill="#FFE040"/>
      <circle cx="55" cy="44" r="11" fill="#FFF060"/>
      <circle cx="49" cy="38" r="5" fill="#FFFAA0" opacity="0.45"/>

      {/* ── Horizon clouds ── */}
      <ellipse cx="28" cy="100" rx="20" ry="9" fill={dark ? '#2A1E5A' : '#FFF'} opacity="0.65"/>
      <ellipse cx="38" cy="95"  rx="15" ry="8" fill={dark ? '#2A1E5A' : '#FFF'} opacity="0.75"/>
      <ellipse cx="80" cy="103" rx="18" ry="8" fill={dark ? '#2A1E5A' : '#FFF'} opacity="0.55"/>
      <ellipse cx="90" cy="98"  rx="12" ry="7" fill={dark ? '#2A1E5A' : '#FFF'} opacity="0.65"/>

      {/* ── Horizon line ── */}
      <path d="M0 108 Q28 104 55 108 Q82 112 110 108 L110 111 Q82 115 55 111 Q28 107 0 111Z"
        fill={dark ? '#E6C77A' : '#7E2A33'} opacity="0.18"/>

      {/* ── Moon (bottom half) ── */}
      <circle cx="55" cy="162" r="18" fill={dark ? '#E4ECFF' : '#E6C77A'}/>
      <circle cx="63" cy="155" r="14" fill={dark ? '#060C14' : '#F2E8D2'}/>
      {/* moon crescent glow */}
      <circle cx="55" cy="162" r="22" fill={dark ? '#E4ECFF' : '#E6C77A'} opacity="0.07">
        <animate attributeName="opacity" values="0.07;0.18;0.07" dur="4s" repeatCount="indefinite"/>
      </circle>

      {/* ── Stars around moon ── */}
      {([
        [22,126,2.0,0], [84,120,1.7,0.6], [14,140,1.5,1.1],
        [96,138,1.9,0.3], [28,155,1.6,0.9], [90,158,1.4,1.5],
        [18,170,1.8,0.5], [97,172,1.5,1.2], [35,182,1.3,0.8],
        [80,184,1.7,0.2],
      ] as [number,number,number,number][]).map(([x,y,r,delay],i) => (
        <circle key={i} cx={x} cy={y} r={r}
          fill={dark ? '#CCD8FF' : '#7E2A33'} opacity="0.6">
          <animate attributeName="opacity" values="0.6;0.08;0.6"
            dur={`${2.4+i*0.28}s`} begin={`${delay}s`} repeatCount="indefinite"/>
        </circle>
      ))}
    </svg>
  )
}

function DayCycleHorizontalSvg({ dark }: { dark: boolean }) {
  return (
    <svg viewBox="0 0 220 100" fill="none" xmlns="http://www.w3.org/2000/svg"
      style={{ width: '100%', height: '100%', maxHeight: '100px' }} aria-hidden="true">
      {/* Sun (left) */}
      <g>
        <animateTransform attributeName="transform" type="rotate"
          values="0,55,50;360,55,50" dur="22s" repeatCount="indefinite"/>
        {Array.from({length:10},(_,i) => {
          const a=(i*36)*Math.PI/180
          return <line key={i}
            x1={55+Math.cos(a)*22} y1={50+Math.sin(a)*22}
            x2={55+Math.cos(a)*(i%2?29:34)} y2={50+Math.sin(a)*(i%2?29:34)}
            stroke="#F59E0B" strokeWidth={i%2?1.5:2.2} strokeLinecap="round" opacity="0.9"/>
        })}
      </g>
      <circle cx="55" cy="50" r="16" fill="#FFE040"/>
      <circle cx="55" cy="50" r="11" fill="#FFF060"/>
      <circle cx="49" cy="44" r="5" fill="#FFFAA0" opacity="0.45"/>

      {/* Moon (right) */}
      <circle cx="165" cy="50" r="22" fill={dark ? '#E4ECFF' : '#E6C77A'} opacity="0.07">
        <animate attributeName="opacity" values="0.07;0.18;0.07" dur="4s" repeatCount="indefinite"/>
      </circle>
      <circle cx="165" cy="50" r="18" fill={dark ? '#E4ECFF' : '#E6C77A'}/>
      <circle cx="173" cy="43" r="14" fill={dark ? '#060C14' : '#F2E8D2'}/>

      {/* Stars between */}
      {([
        [108,32,1.6,0.2], [120,68,1.4,0.7], [98,55,1.3,1.1],
        [130,42,1.5,0.4], [196,30,1.4,0.9], [200,72,1.3,1.3],
      ] as [number,number,number,number][]).map(([x,y,r,delay],i) => (
        <circle key={i} cx={x} cy={y} r={r}
          fill={dark ? '#CCD8FF' : '#7E2A33'} opacity="0.6">
          <animate attributeName="opacity" values="0.6;0.08;0.6"
            dur={`${2.4+i*0.28}s`} begin={`${delay}s`} repeatCount="indefinite"/>
        </circle>
      ))}
    </svg>
  )
}

function NarratorsSvg({ dark }: { dark: boolean }) {
  const sage = dark ? '#6EE7B7' : '#059669'
  const warm = dark ? '#FCD34D' : '#D97706'
  const wind = dark ? '#E6C77A' : '#7E2A33'

  return (
    <svg viewBox="0 0 80 200" fill="none" xmlns="http://www.w3.org/2000/svg"
      style={{ width: '100%', height: '100%' }} aria-hidden="true">

      {/* Forest Owl Sage */}
      <g>
        <animateTransform attributeName="transform" type="translate"
          values="0 0;0 -3;0 0" dur="4s" repeatCount="indefinite"/>
        <ellipse cx="40" cy="44" rx="13" ry="15" fill={dark ? '#112811' : '#ECFDF5'} opacity="0.9"/>
        <path d="M31 34 L28 24 L35 31Z" fill={sage} opacity="0.8"/>
        <path d="M49 34 L52 24 L45 31Z" fill={sage} opacity="0.8"/>
        <path d="M27 46 Q22 53 25 58 Q29 51 32 45" fill={sage} opacity="0.45"/>
        <path d="M53 46 Q58 53 55 58 Q51 51 48 45" fill={sage} opacity="0.45"/>
        <circle cx="34.5" cy="40" r="5.5" fill={dark ? '#0A1F0A' : '#FFF'} opacity="0.95"/>
        <circle cx="45.5" cy="40" r="5.5" fill={dark ? '#0A1F0A' : '#FFF'} opacity="0.95"/>
        <circle cx="34.5" cy="40" r="3.5" fill={sage}/>
        <circle cx="45.5" cy="40" r="3.5" fill={sage}/>
        <circle cx="34.5" cy="40" r="2" fill={dark ? '#0A1F0A' : '#064E3B'}/>
        <circle cx="45.5" cy="40" r="2" fill={dark ? '#0A1F0A' : '#064E3B'}/>
        <circle cx="33.2" cy="38.7" r="0.9" fill="#FFF" opacity="0.9"/>
        <circle cx="44.2" cy="38.7" r="0.9" fill="#FFF" opacity="0.9"/>
        <path d="M37.5 45 L40 49 L42.5 45Q40 43.5 37.5 45Z" fill={warm} opacity="0.9"/>
        <ellipse cx="40" cy="51" rx="7" ry="6" fill={dark ? '#1A3A1A' : '#D1FAE5'} opacity="0.65"/>
      </g>

      {/* Star Wanderer */}
      <g>
        <animateTransform attributeName="transform" type="translate"
          values="0 0;0 -3;0 0" dur="5.2s" begin="0.9s" repeatCount="indefinite"/>
        <path d="M40 84 L42.6 92 L51 92 L44.2 97 L46.8 105 L40 100.2 L33.2 105 L35.8 97 L29 92 L37.4 92Z"
          fill={warm} opacity="0.95"/>
        <circle cx="37" cy="95" r="1.1" fill={dark ? '#1C0A00' : '#78350F'}/>
        <circle cx="43" cy="95" r="1.1" fill={dark ? '#1C0A00' : '#78350F'}/>
        <path d="M37.5 98 Q40 100 42.5 98" stroke={dark ? '#1C0A00' : '#78350F'} strokeWidth="1" fill="none" strokeLinecap="round"/>
        <circle cx="20" cy="90" r="1.4" fill={warm} opacity="0.55">
          <animate attributeName="opacity" values="0.55;0.05;0.55" dur="2.1s" repeatCount="indefinite"/>
        </circle>
        <circle cx="60" cy="93" r="1.1" fill={warm} opacity="0.45">
          <animate attributeName="opacity" values="0.45;0.05;0.45" dur="2.7s" begin="0.6s" repeatCount="indefinite"/>
        </circle>
        <circle cx="23" cy="104" r="0.9" fill={warm} opacity="0.35">
          <animate attributeName="opacity" values="0.35;0.05;0.35" dur="3.3s" begin="1.1s" repeatCount="indefinite"/>
        </circle>
      </g>

      {/* Wind Spirit */}
      <g>
        <animateTransform attributeName="transform" type="translate"
          values="0 0;0 -3;0 0" dur="6.1s" begin="1.8s" repeatCount="indefinite"/>
        <path d="M55 148 Q68 142 65 155 Q62 164 50 160 Q39 156 42 146 Q46 136 57 138 Q70 140 68 151"
          stroke={wind} strokeWidth="2.2" fill="none" strokeLinecap="round" opacity="0.85"
          strokeDasharray="60" strokeDashoffset="0">
          <animate attributeName="stroke-dashoffset" values="0;60;0" dur="4s" repeatCount="indefinite"/>
        </path>
        <circle cx="52" cy="151" r="5.5" fill={wind} opacity="0.2">
          <animate attributeName="opacity" values="0.2;0.45;0.2" dur="3s" repeatCount="indefinite"/>
        </circle>
        <circle cx="52" cy="151" r="2.5" fill={wind} opacity="0.75"/>
        <path d="M22 140 Q29 136 27 145 Q25 151 20 149"
          stroke={wind} strokeWidth="1.3" fill="none" strokeLinecap="round" opacity="0.4">
          <animate attributeName="opacity" values="0.4;0.05;0.4" dur="3.8s" begin="1s" repeatCount="indefinite"/>
        </path>
        <path d="M27 160 Q34 156 33 164"
          stroke={wind} strokeWidth="1" fill="none" strokeLinecap="round" opacity="0.35">
          <animate attributeName="opacity" values="0.35;0.05;0.35" dur="4.2s" begin="1.8s" repeatCount="indefinite"/>
        </path>
        <path d="M62 136 L63 133 L64 136 L67 137 L64 138 L63 141 L62 138 L59 137Z"
          fill={wind} opacity="0.65">
          <animate attributeName="opacity" values="0.65;0.1;0.65" dur="2.6s" begin="0.4s" repeatCount="indefinite"/>
        </path>
      </g>
    </svg>
  )
}

function NarratorsHorizontalSvg({ dark }: { dark: boolean }) {
  const sage = dark ? '#6EE7B7' : '#059669'
  const warm = dark ? '#FCD34D' : '#D97706'
  const wind = dark ? '#E6C77A' : '#7E2A33'

  return (
    <svg viewBox="0 0 240 80" fill="none" xmlns="http://www.w3.org/2000/svg"
      style={{ width: '100%', height: '100%', maxHeight: '80px' }} aria-hidden="true">
      {/* Forest Owl Sage (left) */}
      <g>
        <animateTransform attributeName="transform" type="translate"
          values="0 0;0 -2;0 0" dur="4s" repeatCount="indefinite"/>
        <ellipse cx="40" cy="40" rx="13" ry="15" fill={dark ? '#112811' : '#ECFDF5'} opacity="0.9"/>
        <path d="M31 30 L28 20 L35 27Z" fill={sage} opacity="0.8"/>
        <path d="M49 30 L52 20 L45 27Z" fill={sage} opacity="0.8"/>
        <circle cx="34.5" cy="36" r="5.5" fill={dark ? '#0A1F0A' : '#FFF'} opacity="0.95"/>
        <circle cx="45.5" cy="36" r="5.5" fill={dark ? '#0A1F0A' : '#FFF'} opacity="0.95"/>
        <circle cx="34.5" cy="36" r="3.5" fill={sage}/>
        <circle cx="45.5" cy="36" r="3.5" fill={sage}/>
        <circle cx="34.5" cy="36" r="2" fill={dark ? '#0A1F0A' : '#064E3B'}/>
        <circle cx="45.5" cy="36" r="2" fill={dark ? '#0A1F0A' : '#064E3B'}/>
        <circle cx="33.2" cy="34.7" r="0.9" fill="#FFF" opacity="0.9"/>
        <circle cx="44.2" cy="34.7" r="0.9" fill="#FFF" opacity="0.9"/>
        <path d="M37.5 41 L40 45 L42.5 41Q40 39.5 37.5 41Z" fill={warm} opacity="0.9"/>
      </g>

      {/* Star Wanderer (middle) */}
      <g>
        <animateTransform attributeName="transform" type="translate"
          values="0 0;0 -2;0 0" dur="5.2s" begin="0.9s" repeatCount="indefinite"/>
        <path d="M120 22 L122.6 30 L131 30 L124.2 35 L126.8 43 L120 38.2 L113.2 43 L115.8 35 L109 30 L117.4 30Z"
          fill={warm} opacity="0.95"/>
        <circle cx="117" cy="33" r="1.1" fill={dark ? '#1C0A00' : '#78350F'}/>
        <circle cx="123" cy="33" r="1.1" fill={dark ? '#1C0A00' : '#78350F'}/>
        <path d="M117.5 36 Q120 38 122.5 36" stroke={dark ? '#1C0A00' : '#78350F'} strokeWidth="1" fill="none" strokeLinecap="round"/>
        <circle cx="100" cy="28" r="1.4" fill={warm} opacity="0.55">
          <animate attributeName="opacity" values="0.55;0.05;0.55" dur="2.1s" repeatCount="indefinite"/>
        </circle>
        <circle cx="140" cy="50" r="1.1" fill={warm} opacity="0.45">
          <animate attributeName="opacity" values="0.45;0.05;0.45" dur="2.7s" begin="0.6s" repeatCount="indefinite"/>
        </circle>
      </g>

      {/* Wind Spirit (right) */}
      <g>
        <animateTransform attributeName="transform" type="translate"
          values="0 0;0 -2;0 0" dur="6.1s" begin="1.8s" repeatCount="indefinite"/>
        <path d="M210 38 Q223 32 220 45 Q217 54 205 50 Q194 46 197 36 Q201 26 212 28 Q225 30 223 41"
          stroke={wind} strokeWidth="2.2" fill="none" strokeLinecap="round" opacity="0.85"
          strokeDasharray="60" strokeDashoffset="0">
          <animate attributeName="stroke-dashoffset" values="0;60;0" dur="4s" repeatCount="indefinite"/>
        </path>
        <circle cx="207" cy="41" r="5.5" fill={wind} opacity="0.2">
          <animate attributeName="opacity" values="0.2;0.45;0.2" dur="3s" repeatCount="indefinite"/>
        </circle>
        <circle cx="207" cy="41" r="2.5" fill={wind} opacity="0.75"/>
        <path d="M180 30 L181 27 L182 30 L185 31 L182 32 L181 35 L180 32 L177 31Z"
          fill={wind} opacity="0.65">
          <animate attributeName="opacity" values="0.65;0.1;0.65" dur="2.6s" begin="0.4s" repeatCount="indefinite"/>
        </path>
      </g>
    </svg>
  )
}

function ArchiveSvg({ dark }: { dark: boolean }) {
  return (
    <svg viewBox="0 0 300 220" fill="none" xmlns="http://www.w3.org/2000/svg"
      style={{ width: '100%', height: '100%', display: 'block' }} aria-hidden="true">
      <defs>
        <radialGradient id="archGlow" cx="50%" cy="65%" r="55%">
          <stop offset="0%" stopColor={dark ? '#7E2A33' : '#F59E0B'} stopOpacity="0.32"/>
          <stop offset="100%" stopColor={dark ? '#7E2A33' : '#F59E0B'} stopOpacity="0"/>
        </radialGradient>
      </defs>
      <rect width="300" height="220" fill="url(#archGlow)"/>

      {/* Book 1: purple — crescent moon cover */}
      <rect x="72" y="65" width="26" height="90" rx="2" fill={dark ? '#6E2530' : '#7E2A33'}/>
      <rect x="72" y="65" width="4" height="90" rx="1" fill={dark ? '#7E2A33' : '#9C2F4A'} opacity="0.5"/>
      <rect x="76" y="92" width="16" height="2" rx="1" fill="#FFF" opacity="0.35"/>
      <rect x="78" y="97" width="12" height="2" rx="1" fill="#FFF" opacity="0.22"/>
      <circle cx="85" cy="79" r="6.5" fill={dark ? '#E6C77A' : '#F2E8D2'} opacity="0.6"/>
      <circle cx="88.5" cy="76" r="5.2" fill={dark ? '#6E2530' : '#7E2A33'} opacity="0.95"/>

      {/* Book 2: tall gold — sun rays cover */}
      <g transform="rotate(-2 133 100)">
        <rect x="108" y="45" width="50" height="110" rx="2" fill={dark ? '#92400E' : '#B45309'}/>
        <rect x="108" y="45" width="6" height="110" rx="1" fill={dark ? '#B45309' : '#D97706'} opacity="0.55"/>
        <rect x="116" y="78" width="30" height="2" rx="1" fill="#FFF" opacity="0.38"/>
        <rect x="118" y="83" width="26" height="2" rx="1" fill="#FFF" opacity="0.26"/>
        <rect x="118" y="88" width="26" height="2" rx="1" fill="#FFF" opacity="0.17"/>
        {Array.from({length:8},(_,i) => {
          const a = i * 45 * Math.PI/180
          return <line key={i}
            x1={133+Math.cos(a)*9} y1={62+Math.sin(a)*9}
            x2={133+Math.cos(a)*14} y2={62+Math.sin(a)*14}
            stroke={dark ? '#FCD34D' : '#FEF3C7'} strokeWidth="1.5" strokeLinecap="round" opacity="0.75"/>
        })}
        <circle cx="133" cy="62" r="7.5" fill={dark ? '#FCD34D' : '#F59E0B'} opacity="0.88"/>
      </g>

      {/* Book 3: teal — leaf cover */}
      <rect x="167" y="72" width="32" height="83" rx="2" fill={dark ? '#065F46' : '#047857'}/>
      <rect x="167" y="72" width="4" height="83" rx="1" fill={dark ? '#059669' : '#10B981'} opacity="0.5"/>
      <rect x="173" y="98" width="20" height="2" rx="1" fill="#FFF" opacity="0.35"/>
      <rect x="175" y="103" width="16" height="2" rx="1" fill="#FFF" opacity="0.22"/>
      <path d="M183 82 Q191 79 189 87 Q187 92 183 89 Q179 85 183 82Z"
        fill={dark ? '#6EE7B7' : '#A7F3D0'} opacity="0.75"/>
      <path d="M183 82 L183 93" stroke={dark ? '#6EE7B7' : '#059669'} strokeWidth="1.1" opacity="0.5"/>

      {/* Book 4: rose — heart cover */}
      <rect x="207" y="80" width="22" height="75" rx="2" fill={dark ? '#9D174D' : '#BE185D'}/>
      <rect x="207" y="80" width="3" height="75" rx="1" fill={dark ? '#BE185D' : '#EC4899'} opacity="0.5"/>
      <rect x="212" y="106" width="13" height="2" rx="1" fill="#FFF" opacity="0.35"/>
      <rect x="213" y="111" width="11" height="2" rx="1" fill="#FFF" opacity="0.22"/>
      <path d="M218 91 C218 89 216 87 214 89 C212 87 210 89 210 91 L214 95.5Z"
        fill={dark ? '#F9A8D4' : '#FDF2F8'} opacity="0.8"/>

      {/* Shelf plank */}
      <rect x="60" y="155" width="182" height="8" rx="3" fill={dark ? '#2D1B6B' : '#C4975A'}/>
      <rect x="60" y="155" width="182" height="2" rx="1" fill={dark ? '#4C2FA0' : '#D4AA70'} opacity="0.6"/>
      <ellipse cx="151" cy="167" rx="90" ry="7" fill={dark ? '#000' : '#8B6914'} opacity="0.14"/>

      {/* Floating sparkles */}
      {([
        [50, 52, 2.5, '#F59E0B', 0],
        [252, 46, 2, '#E6C77A', 0.8],
        [42, 105, 1.8, '#FCD34D', 1.4],
        [264, 88, 1.5, '#E6C77A', 0.3],
        [56, 140, 1.6, '#F59E0B', 1.9],
        [248, 132, 1.8, '#F9A8D4', 1.1],
      ] as [number,number,number,string,number][]).map(([x,y,r,color,delay], i) => (
        <circle key={i} cx={x} cy={y} r={r} fill={color} opacity="0.6">
          <animate attributeName="opacity" values="0.6;0.08;0.6"
            dur={`${2.4+i*0.35}s`} begin={`${delay}s`} repeatCount="indefinite"/>
        </circle>
      ))}

      {/* 4-point star accents */}
      <path d="M45 68 L46.2 64 L47.4 68 L51.4 69.2 L47.4 70.4 L46.2 74.4 L45 70.4 L41 69.2Z"
        fill={dark ? '#FCD34D' : '#F59E0B'} opacity="0.55">
        <animate attributeName="opacity" values="0.55;0.08;0.55" dur="3.2s" begin="0.5s" repeatCount="indefinite"/>
      </path>
      <path d="M257 110 L258.2 106 L259.4 110 L263.4 111.2 L259.4 112.4 L258.2 116.4 L257 112.4 L253 111.2Z"
        fill={dark ? '#E6C77A' : '#7E2A33'} opacity="0.5">
        <animate attributeName="opacity" values="0.5;0.08;0.5" dur="4s" begin="1.6s" repeatCount="indefinite"/>
      </path>
    </svg>
  )
}

export function Features() {
  const { t } = useLocale()
  const { theme } = useTheme()
  const dark = theme === 'dark'
  const { ref: headRef, visible: headVisible } = useReveal()
  const { ref: c1, visible: v1 } = useReveal({ threshold: 0.1 })
  const { ref: c2, visible: v2 } = useReveal({ threshold: 0.1 })
  const { ref: c3, visible: v3 } = useReveal({ threshold: 0.1 })
  const { ref: c4, visible: v4 } = useReveal({ threshold: 0.1 })

  return (
    <section className={styles.section} id="features">
      <SectionParticles />
      <div className={styles.bgIllust} aria-hidden="true">
        <svg viewBox="0 0 300 400" fill="none" xmlns="http://www.w3.org/2000/svg" width="100%" height="100%">
          <circle cx="200" cy="80" r="60" fill="url(#featMG)" opacity="0.4"/>
          <path d="M215 55 A35 35 0 1 0 215 105 A25 25 0 1 1 215 55Z" fill="#EDD9A3" opacity="0.5"/>
          <path d="M0 120 C40 110 60 140 100 125" stroke="#6B4C3B" strokeWidth="1.2" fill="none" opacity="0.25"/>
          <ellipse cx="125" cy="78" rx="5" ry="10" transform="rotate(-20 125 78)" fill="#166534" opacity="0.15"/>
          <path d="M20 400 C25 350 35 300 30 250 C28 220 20 180 0 150" stroke="#6B4C3B" strokeWidth="2" fill="none" opacity="0.15"/>
          <circle cx="80" cy="200" r="3" fill="#F59E0B" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.15;0.5" dur="3s" repeatCount="indefinite"/>
          </circle>
          <path d="M250 40L251 36L255 38L251 35L250 31L249 35L245 33L249 36Z" fill="#E6C77A" opacity="0.5">
            <animate attributeName="opacity" values="0.5;0.2;0.5" dur="4s" repeatCount="indefinite"/>
          </path>
          <defs>
            <radialGradient id="featMG" cx="0.5" cy="0.5" r="0.5">
              <stop offset="0%" stopColor="#EDD9A3" stopOpacity="0.35"/>
              <stop offset="100%" stopColor="#EDD9A3" stopOpacity="0"/>
            </radialGradient>
          </defs>
        </svg>
      </div>
      <div className={styles.inner}>
        <div ref={headRef} className={`reveal ${headVisible ? 'visible' : ''}`}>
          <div className={styles.label}>{t.features.label}</div>
          <div className={styles.title}>{t.features.title}</div>
        </div>

        <div className={styles.bento}>
          {/* Large card — split layout */}
          <div ref={c1} className={`${styles.card} ${styles.cardLarge} reveal ${v1 ? 'visible' : ''}`}>
            <div className={styles.largeLeft}>
              <h3>{t.features.cards[0].title}</h3>
              <p>{t.features.cards[0].desc}</p>
            </div>
            <div className={styles.largeRight}>
              <MagicBookSvg dark={dark} />
            </div>
          </div>

          {/* Medium card */}
          <div ref={c2} className={`${styles.card} ${styles.cardMedium} reveal ${v2 ? 'visible' : ''}`}>
            <div className={styles.mediumText}>
              <h3>{t.features.cards[1].title}</h3>
              <p>{t.features.cards[1].desc}</p>
            </div>
            <div className={styles.mediumImg}>
              <div className={styles.svgVertical}><DayCycleSvg dark={dark} /></div>
              <div className={styles.svgHorizontal}><DayCycleHorizontalSvg dark={dark} /></div>
            </div>
          </div>

          {/* Text-only card */}
          <div ref={c3} className={`${styles.card} ${styles.cardText} reveal ${v3 ? 'visible' : ''}`}>
            <div className={styles.cardTextContent}>
              <h3>{t.features.cards[2].title}</h3>
              <p>{t.features.cards[2].desc}</p>
            </div>
            <div className={styles.cardTextImg}>
              <div className={styles.svgVertical}><NarratorsSvg dark={dark} /></div>
              <div className={styles.svgHorizontal}><NarratorsHorizontalSvg dark={dark} /></div>
            </div>
          </div>

          {/* Image card */}
          <div ref={c4} className={`${styles.card} ${styles.cardImg} reveal ${v4 ? 'visible' : ''}`}>
            <div className={styles.bgImage}>
              <div className={styles.archiveIllust}>
                <ArchiveSvg dark={dark} />
              </div>
            </div>
            <div className={styles.imgText}>
              <h3>{t.features.cards[3].title}</h3>
              <p>{t.features.cards[3].desc}</p>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
