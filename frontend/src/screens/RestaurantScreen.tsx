import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import './RestaurantScreen.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const BackIconWhite = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <polyline points="15 18 9 12 15 6"/>
  </svg>
);

const DotsIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="white" aria-hidden="true">
    <circle cx="12" cy="5"  r="1.8"/>
    <circle cx="12" cy="12" r="1.8"/>
    <circle cx="12" cy="19" r="1.8"/>
  </svg>
);

const StarIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="#FFC107" aria-hidden="true">
    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
  </svg>
);

const TruckIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="1" y="3" width="15" height="13" rx="2" fill="#FF7622"/>
    <path d="M16 8h4l3 5v4h-7V8z" fill="#FF7622"/>
    <circle cx="5.5"  cy="18.5" r="2.5" fill="#FF7622"/>
    <circle cx="18.5" cy="18.5" r="2.5" fill="#FF7622"/>
  </svg>
);

const ClockIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#FF7622"/>
    <path d="M12 6v6l4 2" stroke="white" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const PlusIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round" aria-hidden="true">
    <line x1="8" y1="2" x2="8" y2="14"/>
    <line x1="2" y1="8" x2="14" y2="8"/>
  </svg>
);


// ─── Hero cover SVG (restaurant flat-lay scene) ───────────────────────────────

const HeroCover = () => (
  <svg viewBox="0 0 430 240" fill="none" xmlns="http://www.w3.org/2000/svg"
    className="rp-hero__svg" aria-hidden="true" preserveAspectRatio="xMidYMid slice">

    {/* Dark warm background — restaurant ambience */}
    <rect width="430" height="240" fill="#1A0F00"/>

    {/* Bokeh light blobs — background atmosphere */}
    <circle cx="60"  cy="40"  r="55" fill="#FF7622" opacity="0.12"/>
    <circle cx="370" cy="60"  r="70" fill="#FF9A3C" opacity="0.10"/>
    <circle cx="200" cy="200" r="80" fill="#FF7622" opacity="0.08"/>
    <circle cx="420" cy="200" r="50" fill="#FFC107" opacity="0.07"/>

    {/* Wooden table surface */}
    <rect x="0" y="100" width="430" height="140" rx="0" fill="#3E2000"/>
    {/* Wood grain lines */}
    {[110,125,140,155,170,185,200,215,230].map((y,i) => (
      <path key={i}
        d={`M0 ${y} Q${80+i*10} ${y-4} ${200+i*5} ${y} Q${320+i*3} ${y+3} 430 ${y}`}
        stroke="#4A2800" strokeWidth="1.5" fill="none" opacity="0.6"/>
    ))}

    {/* ── Plate 1 — left: burger ── */}
    <ellipse cx="110" cy="175" rx="72" ry="52" fill="#2A1500" opacity="0.5"/>
    <ellipse cx="110" cy="170" rx="68" ry="48" fill="#F5F0E8"/>
    <ellipse cx="110" cy="168" rx="62" ry="42" fill="white"/>
    {/* Burger on plate */}
    <ellipse cx="110" cy="158" rx="38" ry="18" fill="#F4A340"/>
    <ellipse cx="110" cy="153" rx="34" ry="14" fill="#FFCC80"/>
    <rect x="72"  y="158" width="76" height="8"  rx="2" fill="#66BB6A"/>
    <rect x="72"  y="164" width="76" height="6"  rx="2" fill="#E53935" opacity="0.8"/>
    <path d="M72 170 L186 170 L188 176 L70 176 Z" fill="#FFC107" opacity="0.9"/>
    <ellipse cx="110" cy="180" rx="38" ry="10" fill="#5D4037"/>
    <ellipse cx="110" cy="177" rx="36" ry="8"  fill="#795548"/>
    <ellipse cx="110" cy="188" rx="40" ry="9"  fill="#E8834A"/>
    {/* Sesame */}
    {[[100,150],[110,147],[120,150]].map(([x,y],i)=>(
      <ellipse key={i} cx={x} cy={y} rx="2.5" ry="1.5" fill="#D4A017" opacity="0.7"/>
    ))}

    {/* ── Plate 2 — centre: pizza ── */}
    <ellipse cx="215" cy="178" rx="78" ry="56" fill="#2A1500" opacity="0.5"/>
    <ellipse cx="215" cy="172" rx="74" ry="52" fill="#F5F0E8"/>
    <ellipse cx="215" cy="170" rx="68" ry="46" fill="white"/>
    {/* Pizza */}
    <circle cx="215" cy="162" r="46" fill="#FF7622" opacity="0.15"/>
    <circle cx="215" cy="162" r="42" fill="#FFCC80"/>
    <circle cx="215" cy="162" r="36" fill="#FF7622" opacity="0.85"/>
    <circle cx="215" cy="162" r="28" fill="#FFCC80"/>
    {[
      [200,150],[228,148],[208,168],[228,166],[215,156],
      [202,162],[228,158]
    ].map(([cx,cy],i)=>(
      <circle key={i} cx={cx} cy={cy} r="5" fill="#E53935" opacity="0.85"/>
    ))}
    {[[210,156],[222,158],[206,164],[224,162]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="7" ry="4" fill="#FFF176" opacity="0.65"/>
    ))}
    <circle cx="215" cy="162" r="42" fill="none"
      stroke="#E8834A" strokeWidth="6" strokeDasharray="10 5"/>
    {/* Basil */}
    <ellipse cx="216" cy="152" rx="5" ry="3" fill="#388E3C" opacity="0.9"
      transform="rotate(-20 216 152)"/>

    {/* ── Plate 3 — right: salad bowl ── */}
    <ellipse cx="340" cy="175" rx="72" ry="52" fill="#2A1500" opacity="0.5"/>
    <ellipse cx="340" cy="170" rx="68" ry="48" fill="#F5F0E8"/>
    <ellipse cx="340" cy="168" rx="62" ry="42" fill="white"/>
    {/* Salad */}
    <ellipse cx="340" cy="162" rx="44" ry="28" fill="#E8F5E9"/>
    <ellipse cx="325" cy="158" rx="14" ry="9"  fill="#66BB6A" opacity="0.9"
      transform="rotate(-15 325 158)"/>
    <ellipse cx="352" cy="156" rx="12" ry="8"  fill="#8BC34A" opacity="0.9"
      transform="rotate(10 352 156)"/>
    <ellipse cx="340" cy="166" rx="16" ry="10" fill="#4CAF50" opacity="0.8"
      transform="rotate(-5 340 166)"/>
    <circle cx="328" cy="164" r="6" fill="#E53935" opacity="0.85"/>
    <circle cx="352" cy="162" r="5" fill="#E53935" opacity="0.85"/>
    <rect x="336" y="156" width="8" height="8" rx="2" fill="#F4A340"/>
    <path d="M318 160 Q330 154 342 160 Q348 163 356 160"
      stroke="#FFF176" strokeWidth="2" fill="none" strokeLinecap="round" opacity="0.7"/>

    {/* ── Drinks ── */}
    {/* Left glass */}
    <rect x="28" y="130" width="28" height="52" rx="5" fill="#1565C0" opacity="0.85"/>
    <rect x="24" y="126" width="36" height="10" rx="4" fill="#0D47A1"/>
    <rect x="38" y="108" width="5" height="24" rx="2" fill="#90CAF9"/>
    <path d="M30 140 Q42 136 54 140" stroke="white" strokeWidth="1.5"
      fill="none" opacity="0.3"/>
    {/* Right glass */}
    <rect x="374" y="132" width="26" height="48" rx="5" fill="#E53935" opacity="0.8"/>
    <rect x="370" y="128" width="34" height="9" rx="4" fill="#C62828"/>
    <rect x="383" y="112" width="5" height="22" rx="2" fill="#EF9A9A"/>

    {/* ── Cutlery ── */}
    {/* Fork left */}
    <rect x="168" y="118" width="4" height="50" rx="2" fill="#9E9E9E" opacity="0.7"/>
    <rect x="162" y="118" width="3" height="20" rx="1.5" fill="#9E9E9E" opacity="0.6"/>
    <rect x="174" y="118" width="3" height="20" rx="1.5" fill="#9E9E9E" opacity="0.6"/>
    {/* Knife right */}
    <rect x="258" y="118" width="4" height="50" rx="2" fill="#9E9E9E" opacity="0.7"/>
    <path d="M262 118 Q270 128 262 138" fill="#BDBDBD" opacity="0.7"/>

    {/* ── Ambient candle ── */}
    <rect x="196" y="88" width="8" height="22" rx="3" fill="#FFF9C4"/>
    <ellipse cx="200" cy="88" rx="4" ry="2" fill="#FFF176"/>
    <path d="M200 82 Q202 76 200 72" stroke="#FF8F00" strokeWidth="2.5"
      strokeLinecap="round" fill="none"/>
    <circle cx="200" cy="72" r="3" fill="#FFC107" opacity="0.8"/>

    {/* ── Napkin ── */}
    <path d="M380 140 Q400 130 420 140 L415 190 Q400 200 385 190 Z"
      fill="white" opacity="0.12"/>

    {/* ── Gradient overlay — top fade to dark for header legibility ── */}
    <defs>
      <linearGradient id="heroFade" x1="0" y1="0" x2="0" y2="1">
        <stop offset="0%"   stopColor="#000000" stopOpacity="0.55"/>
        <stop offset="45%"  stopColor="#000000" stopOpacity="0.10"/>
        <stop offset="100%" stopColor="#000000" stopOpacity="0.30"/>
      </linearGradient>
    </defs>
    <rect width="430" height="240" fill="url(#heroFade)"/>
  </svg>
);


// ─── Burger SVG illustrations (same as CategoryScreen) ───────────────────────

const BurgerBistro = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="128" rx="58" ry="10" fill="rgba(0,0,0,0.07)"/>
    <ellipse cx="80" cy="52" rx="54" ry="30" fill="#F4A340"/>
    <ellipse cx="80" cy="46" rx="50" ry="26" fill="#FFCC80"/>
    <ellipse cx="80" cy="40" rx="46" ry="20" fill="#F4A340"/>
    {[[62,36],[76,32],[90,35],[70,42],[84,40],[96,38]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#D4A017" opacity="0.75"/>
    ))}
    <path d="M26 76 Q36 68 46 76 Q56 68 66 76 Q76 68 86 76 Q96 68 106 76 Q116 68 126 76 Q130 80 126 82 Q116 74 106 82 Q96 74 86 82 Q76 74 66 82 Q56 74 46 82 Q36 74 26 82 Q22 80 26 76Z" fill="#66BB6A"/>
    <ellipse cx="80" cy="86" rx="46" ry="10" fill="#E53935" opacity="0.85"/>
    <path d="M30 90 L130 90 L134 100 L26 100 Z" fill="#FFC107" opacity="0.9"/>
    <ellipse cx="80" cy="106" rx="50" ry="14" fill="#5D4037"/>
    <ellipse cx="80" cy="102" rx="48" ry="12" fill="#795548"/>
    {[52,72,92].map((x,i)=>(
      <path key={i} d={`M${x} 100 Q${x+8} 96 ${x+16} 100`}
        stroke="#4E342E" strokeWidth="2.5" strokeLinecap="round" fill="none" opacity="0.6"/>
    ))}
    <ellipse cx="80" cy="118" rx="52" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="116" rx="50" ry="10" fill="#F4A340"/>
  </svg>
);

const SmokinBurger = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <path d="M60 18 Q64 10 60 4" stroke="#B0BEC5" strokeWidth="2.5" strokeLinecap="round" fill="none" opacity="0.6"/>
    <path d="M80 14 Q84 6 80 0"  stroke="#B0BEC5" strokeWidth="2"   strokeLinecap="round" fill="none" opacity="0.5"/>
    <path d="M100 18 Q104 10 100 4" stroke="#B0BEC5" strokeWidth="2" strokeLinecap="round" fill="none" opacity="0.5"/>
    <ellipse cx="80" cy="130" rx="60" ry="9" fill="rgba(0,0,0,0.07)"/>
    <ellipse cx="80" cy="50" rx="56" ry="28" fill="#E8834A"/>
    <ellipse cx="80" cy="44" rx="52" ry="24" fill="#F4A340"/>
    <ellipse cx="80" cy="38" rx="48" ry="18" fill="#FFCC80"/>
    {[[64,34],[78,30],[92,33],[70,40],[86,38]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#D4A017" opacity="0.7"/>
    ))}
    <path d="M28 72 Q50 62 80 66 Q110 62 132 72 Q128 80 80 78 Q32 80 28 72Z" fill="#FF8F00" opacity="0.85"/>
    <path d="M28 80 Q50 74 80 78 Q110 74 132 80 Q128 88 80 86 Q32 88 28 80Z" fill="#C62828" opacity="0.9"/>
    <path d="M26 88 L134 88 L138 98 L22 98 Z" fill="#FFC107" opacity="0.95"/>
    <ellipse cx="80" cy="110" rx="52" ry="15" fill="#4E342E"/>
    <ellipse cx="80" cy="106" rx="50" ry="13" fill="#6D4C41"/>
    <ellipse cx="80" cy="120" rx="54" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="118" rx="52" ry="10" fill="#F4A340"/>
  </svg>
);

const CrispyChicken = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    <ellipse cx="80" cy="48" rx="54" ry="28" fill="#FFCC80"/>
    <ellipse cx="80" cy="42" rx="50" ry="24" fill="#FFE0B2"/>
    <ellipse cx="80" cy="36" rx="46" ry="18" fill="#FFCC80"/>
    {[[62,32],[76,28],[90,31],[68,38],[84,36],[96,34]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3" ry="1.8" fill="#D4A017" opacity="0.7"/>
    ))}
    <path d="M26 72 Q50 64 80 68 Q110 64 134 72 Q130 80 80 78 Q30 80 26 72Z" fill="#F5F5F5"/>
    {[[46,80],[70,78],[94,80],[118,78]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="10" ry="5" fill="#8BC34A" opacity="0.85"/>
    ))}
    <ellipse cx="80" cy="96" rx="52" ry="18" fill="#F4A340"/>
    <ellipse cx="80" cy="92" rx="50" ry="16" fill="#FFCC80"/>
    <path d="M60 96 Q64 106 62 112" stroke="#FF7622" strokeWidth="3" strokeLinecap="round" fill="none" opacity="0.7"/>
    <ellipse cx="80" cy="116" rx="52" ry="12" fill="#F4A340"/>
    <ellipse cx="80" cy="114" rx="50" ry="10" fill="#FFCC80"/>
  </svg>
);

const DoublePattyCheese = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="132" rx="60" ry="9" fill="rgba(0,0,0,0.07)"/>
    <ellipse cx="80" cy="42" rx="56" ry="30" fill="#F4A340"/>
    <ellipse cx="80" cy="36" rx="52" ry="26" fill="#FFCC80"/>
    <ellipse cx="80" cy="30" rx="48" ry="20" fill="#F4A340"/>
    {[[60,26],[74,22],[88,25],[66,32],[82,30],[94,28]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#D4A017" opacity="0.75"/>
    ))}
    <path d="M24 68 Q34 60 44 68 Q54 60 64 68 Q74 60 84 68 Q94 60 104 68 Q114 60 124 68 Q128 72 124 74 Q114 66 104 74 Q94 66 84 74 Q74 66 64 74 Q54 66 44 74 Q34 66 24 74 Q20 72 24 68Z" fill="#66BB6A"/>
    <path d="M24 74 L136 74 L140 84 L20 84 Z" fill="#FFC107" opacity="0.95"/>
    <ellipse cx="80" cy="92" rx="52" ry="13" fill="#5D4037"/>
    <ellipse cx="80" cy="88" rx="50" ry="11" fill="#795548"/>
    <path d="M24 96 L136 96 L140 106 L20 106 Z" fill="#FFD54F" opacity="0.9"/>
    <ellipse cx="80" cy="112" rx="52" ry="13" fill="#4E342E"/>
    <ellipse cx="80" cy="108" rx="50" ry="11" fill="#6D4C41"/>
    <ellipse cx="80" cy="122" rx="54" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="120" rx="52" ry="10" fill="#F4A340"/>
  </svg>
);

const SpicyJalapeno = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    <ellipse cx="80" cy="50" rx="54" ry="28" fill="#E8834A"/>
    <ellipse cx="80" cy="44" rx="50" ry="24" fill="#FF8A65"/>
    <ellipse cx="80" cy="38" rx="46" ry="18" fill="#FFAB91"/>
    {[[44,72],[60,68],[76,72],[92,68],[108,72],[124,68]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="8" ry="5" fill="#388E3C" opacity="0.9"/>
    ))}
    <path d="M26 78 Q50 70 80 74 Q110 70 134 78 Q130 88 80 86 Q30 88 26 78Z" fill="#FF5722" opacity="0.85"/>
    <path d="M26 88 L134 88 L138 98 L22 98 Z" fill="#FFC107" opacity="0.9"/>
    <ellipse cx="80" cy="108" rx="52" ry="14" fill="#4E342E"/>
    <ellipse cx="80" cy="104" rx="50" ry="12" fill="#6D4C41"/>
    <ellipse cx="80" cy="120" rx="52" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="118" rx="50" ry="10" fill="#FF8A65"/>
  </svg>
);

const MushroomSwiss = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    <ellipse cx="80" cy="50" rx="54" ry="28" fill="#8D6E63"/>
    <ellipse cx="80" cy="44" rx="50" ry="24" fill="#A1887F"/>
    <ellipse cx="80" cy="38" rx="46" ry="18" fill="#BCAAA4"/>
    {[[44,70],[62,66],[80,70],[98,66],[116,70]].map(([cx,cy],i)=>(
      <g key={i}>
        <ellipse cx={cx} cy={cy} rx="10" ry="6" fill="#795548" opacity="0.9"/>
        <ellipse cx={cx} cy={cy-2} rx="8" ry="4" fill="#8D6E63"/>
      </g>
    ))}
    <path d="M24 78 L136 78 L140 90 L20 90 Z" fill="#FFF9C4" opacity="0.95"/>
    {[50,72,94,116].map((cx,i)=>(
      <circle key={i} cx={cx} cy={i%2===0?83:81} r={i%2===0?4:3} fill="#F9A825" opacity="0.4"/>
    ))}
    <ellipse cx="80" cy="106" rx="52" ry="14" fill="#4E342E"/>
    <ellipse cx="80" cy="102" rx="50" ry="12" fill="#6D4C41"/>
    <ellipse cx="80" cy="118" rx="52" ry="12" fill="#8D6E63"/>
    <ellipse cx="80" cy="116" rx="50" ry="10" fill="#A1887F"/>
  </svg>
);

// Pizza illustrations for the Pizza tab
const MargheritaPizza = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    <circle cx="80" cy="80" r="58" fill="#FF7622" opacity="0.12"/>
    <circle cx="80" cy="80" r="52" fill="#FFCC80"/>
    <circle cx="80" cy="80" r="44" fill="#FF7622" opacity="0.85"/>
    <circle cx="80" cy="80" r="36" fill="#FFCC80"/>
    {[[64,64],[94,62],[70,88],[96,86],[80,72],[68,78],[92,78]].map(([cx,cy],i)=>(
      <circle key={i} cx={cx} cy={cy} r="6" fill="#E53935" opacity="0.85"/>
    ))}
    {[[76,70],[88,72],[72,80],[90,78]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="8" ry="5" fill="#FFF176" opacity="0.65"/>
    ))}
    <circle cx="80" cy="80" r="52" fill="none" stroke="#E8834A" strokeWidth="7" strokeDasharray="11 6"/>
    <ellipse cx="80" cy="68" rx="5" ry="3" fill="#388E3C" opacity="0.9" transform="rotate(-20 80 68)"/>
    <ellipse cx="92" cy="88" rx="5" ry="3" fill="#388E3C" opacity="0.9" transform="rotate(15 92 88)"/>
  </svg>
);

const PepperoniPizza = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    <circle cx="80" cy="80" r="52" fill="#FFCC80"/>
    <circle cx="80" cy="80" r="44" fill="#FF7622" opacity="0.9"/>
    <circle cx="80" cy="80" r="36" fill="#FFCC80"/>
    {[[64,64],[94,62],[70,88],[96,86],[80,72],[68,78],[92,78],[80,90]].map(([cx,cy],i)=>(
      <circle key={i} cx={cx} cy={cy} r="7" fill="#C62828" opacity="0.9"/>
    ))}
    {[[64,64],[94,62],[70,88],[96,86],[80,72],[68,78],[92,78],[80,90]].map(([cx,cy],i)=>(
      <circle key={i} cx={cx-1} cy={cy-1} r="3" fill="#E53935" opacity="0.5"/>
    ))}
    <circle cx="80" cy="80" r="52" fill="none" stroke="#E8834A" strokeWidth="7" strokeDasharray="11 6"/>
  </svg>
);


// ─── Data ─────────────────────────────────────────────────────────────────────

interface MenuItem {
  id: string;
  name: string;
  kitchen: string;
  price: string;
  Illustration: React.FC;
}

const MENU_TABS = ['Burger', 'Sandwich', 'Pizza', 'Chicken', 'Drinks'];

const MENU_ITEMS: Record<string, MenuItem[]> = {
  Burger: [
    { id: 'b1', name: 'Burger Bistro',      kitchen: 'Spicy Kitchen',  price: '₹540', Illustration: BurgerBistro   },
    { id: 'b2', name: "Smokin' Burger",      kitchen: 'Smoke House',    price: '₹630', Illustration: SmokinBurger   },
    { id: 'b3', name: 'Crispy Chicken',      kitchen: 'Crispy Co.',     price: '₹495', Illustration: CrispyChicken  },
    { id: 'b4', name: 'Double Patty Cheese', kitchen: 'Cheese Factory', price: '₹720', Illustration: DoublePattyCheese },
    { id: 'b5', name: 'Spicy Jalapeño',      kitchen: 'Spice Lab',      price: '₹585', Illustration: SpicyJalapeno  },
    { id: 'b6', name: 'Mushroom Swiss',      kitchen: 'Garden Grill',   price: '₹675', Illustration: MushroomSwiss  },
    { id: 'b7', name: 'Classic Smash',       kitchen: 'Spicy Kitchen',  price: '₹450', Illustration: BurgerBistro   },
    { id: 'b8', name: 'BBQ Ranch',           kitchen: 'Ranch House',    price: '₹585', Illustration: SmokinBurger   },
    { id: 'b9', name: 'Truffle Burger',      kitchen: 'Fine Grill',     price: '₹810', Illustration: MushroomSwiss  },
    { id: 'b10',name: 'Avocado Burger',      kitchen: 'Green Kitchen',  price: '₹675', Illustration: CrispyChicken  },
  ],
  Pizza: [
    { id: 'p1', name: 'Margherita',          kitchen: 'Spicy Kitchen',  price: '₹630', Illustration: MargheritaPizza },
    { id: 'p2', name: 'Pepperoni',           kitchen: 'Spicy Kitchen',  price: '₹720', Illustration: PepperoniPizza  },
    { id: 'p3', name: 'BBQ Chicken',         kitchen: 'Spicy Kitchen',  price: '₹765', Illustration: MargheritaPizza },
    { id: 'p4', name: 'Veggie Supreme',      kitchen: 'Green Kitchen',  price: '₹675', Illustration: PepperoniPizza  },
  ],
  Sandwich: [
    { id: 's1', name: 'Club Sandwich',       kitchen: 'Spicy Kitchen',  price: '₹405',  Illustration: CrispyChicken  },
    { id: 's2', name: 'BLT Deluxe',          kitchen: 'Deli House',     price: '₹450', Illustration: BurgerBistro   },
  ],
  Chicken: [
    { id: 'c1', name: 'Fried Chicken',       kitchen: 'Crispy Co.',     price: '₹540', Illustration: CrispyChicken  },
    { id: 'c2', name: 'Grilled Thighs',      kitchen: 'Grill Master',   price: '₹630', Illustration: SpicyJalapeno  },
  ],
  Drinks: [
    { id: 'd1', name: 'Mango Shake',         kitchen: 'Spicy Kitchen',  price: '₹225',  Illustration: BurgerBistro   },
    { id: 'd2', name: 'Lemonade',            kitchen: 'Spicy Kitchen',  price: '₹180',  Illustration: MargheritaPizza },
  ],
};

// ─── Product card ─────────────────────────────────────────────────────────────

interface ProductCardProps {
  item: MenuItem;
  added: boolean;
  onAdd: (id: string) => void;
}

const ProductCard: React.FC<ProductCardProps> = ({ item, added, onAdd }) => {
  const navigate = useNavigate();
  return (
  <article className="rp-card" onClick={() => navigate('/item')}
    style={{ cursor: 'pointer' }}>
    <div className="rp-card__img">
      <item.Illustration />
    </div>
    <div className="rp-card__body">
      <h3 className="rp-card__name">{item.name}</h3>
      <p className="rp-card__kitchen">{item.kitchen}</p>
      <div className="rp-card__footer">
        <span className="rp-card__price">{item.price}</span>
        <button
          className={`rp-card__add${added ? ' rp-card__add--done' : ''}`}
          onClick={(e) => { e.stopPropagation(); onAdd(item.id); }}
          aria-label={added ? `${item.name} added` : `Add ${item.name}`}
        >
          {added ? (
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none"
              stroke="white" strokeWidth="2.5" strokeLinecap="round"
              strokeLinejoin="round" aria-hidden="true">
              <polyline points="2 7 5.5 10.5 12 3.5"/>
            </svg>
          ) : (
            <PlusIcon />
          )}
        </button>
      </div>
    </div>
  </article>
  );
};

// ─── Main screen ──────────────────────────────────────────────────────────────

const RestaurantScreen: React.FC = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('Burger');
  const [addedItems, setAddedItems] = useState<Set<string>>(new Set());
  const tabsRef = useRef<HTMLDivElement>(null);

  const handleAdd = (id: string) => {
    setAddedItems((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const currentItems = MENU_ITEMS[activeTab] ?? [];

  return (
    <div className="rp-page">
      <div className="rp-viewport">

        {/* ── Hero cover ── */}
        <div className="rp-hero">
          <HeroCover />

          {/* Overlay buttons */}
          <div className="rp-hero__overlay">
            <button className="rp-hero__btn" onClick={() => navigate(-1)}
              aria-label="Go back">
              <BackIconWhite />
            </button>
            <button className="rp-hero__btn" aria-label="More options">
              <DotsIcon />
            </button>
          </div>
        </div>

        {/* ── Scrollable content ── */}
        <main className="rp-main">

          {/* ── Profile summary ── */}
          <section className="rp-profile">
            <div className="rp-profile__inner">
            <h1 className="rp-profile__name">Spicy Restaurant</h1>
            <p className="rp-profile__desc">
              Authentic flavours crafted with the finest ingredients. From
              juicy smash burgers to wood-fired pizzas — every bite tells a story.
            </p>

            {/* Meta row */}
            <div className="rp-profile__meta">
              <div className="rp-profile__meta-item">
                <StarIcon />
                <span className="rp-profile__meta-val">4.7</span>
                <span className="rp-profile__meta-label">(2.4k)</span>
              </div>
              <span className="rp-profile__meta-dot" aria-hidden="true"/>
              <div className="rp-profile__meta-item">
                <TruckIcon />
                <span className="rp-profile__meta-val rp-profile__meta-val--orange">Free</span>
                <span className="rp-profile__meta-label">delivery</span>
              </div>
              <span className="rp-profile__meta-dot" aria-hidden="true"/>
              <div className="rp-profile__meta-item">
                <ClockIcon />
                <span className="rp-profile__meta-val rp-profile__meta-val--orange">20 min</span>
              </div>
            </div>
            </div>
          </section>

          {/* ── Menu tab nav ── */}
          <div className="rp-tabs" ref={tabsRef} role="tablist"
            aria-label="Menu categories">
            {MENU_TABS.map((tab) => (
              <button
                key={tab}
                role="tab"
                aria-selected={tab === activeTab}
                className={`rp-tab${tab === activeTab ? ' rp-tab--active' : ''}`}
                onClick={() => setActiveTab(tab)}
              >
                {tab}
              </button>
            ))}
          </div>

          {/* ── Product sub-section ── */}
          <section className="rp-section" aria-label={`${activeTab} menu items`}>
            <h2 className="rp-section__title">
              {activeTab}
              <span className="rp-section__count">({currentItems.length})</span>
            </h2>

            <div className="rp-grid" role="list">
              {currentItems.map((item) => (
                <div key={item.id} role="listitem">
                  <ProductCard
                    item={item}
                    added={addedItems.has(item.id)}
                    onAdd={handleAdd}
                  />
                </div>
              ))}
            </div>
          </section>

        </main>
      </div>
    </div>
  );
};

export default RestaurantScreen;
