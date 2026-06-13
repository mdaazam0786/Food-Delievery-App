import React, { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import './OnboardingScreen.css';

// ─── SVG Illustrations ────────────────────────────────────────────────────────

/** Screen A: Man on tablet surrounded by floating food items */
const IllustrationTabletMan: React.FC = () => (
  <svg viewBox="0 0 280 260" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    {/* Floating burger – top left */}
    <g transform="translate(18,30)">
      <ellipse cx="26" cy="34" rx="26" ry="8" fill="#F4A340" opacity="0.9"/>
      <rect x="4" y="22" width="44" height="12" rx="3" fill="#E8834A"/>
      <ellipse cx="26" cy="22" rx="22" ry="7" fill="#8BC34A"/>
      <ellipse cx="26" cy="16" rx="22" ry="8" fill="#F4A340"/>
      <ellipse cx="26" cy="10" rx="22" ry="8" fill="#FFCC80"/>
    </g>
    {/* Floating pizza slice – top right */}
    <g transform="translate(210,20)">
      <path d="M30 0 L0 52 L60 52 Z" fill="#FF7A28" opacity="0.85"/>
      <path d="M30 0 L0 52 L60 52 Z" fill="none" stroke="#E86020" strokeWidth="1.5"/>
      <circle cx="30" cy="32" r="5" fill="#E53935" opacity="0.9"/>
      <circle cx="18" cy="42" r="4" fill="#E53935" opacity="0.9"/>
      <circle cx="42" cy="40" r="4" fill="#E53935" opacity="0.9"/>
    </g>
    {/* Floating coffee cup – bottom left */}
    <g transform="translate(10,170)">
      <rect x="6" y="14" width="36" height="28" rx="4" fill="#795548"/>
      <path d="M42 20 Q54 20 54 28 Q54 36 42 36" stroke="#795548" strokeWidth="4" fill="none" strokeLinecap="round"/>
      <rect x="0" y="10" width="48" height="8" rx="3" fill="#5D4037"/>
      <path d="M16 4 Q20 0 24 4 Q28 0 32 4" stroke="#FF7A28" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
    </g>
    {/* Floating fries – bottom right */}
    <g transform="translate(218,168)">
      <rect x="8" y="20" width="36" height="28" rx="4" fill="#E53935"/>
      <rect x="12" y="0" width="6" height="24" rx="3" fill="#FFCC02"/>
      <rect x="21" y="0" width="6" height="28" rx="3" fill="#FFCC02"/>
      <rect x="30" y="0" width="6" height="22" rx="3" fill="#FFCC02"/>
    </g>
    {/* Person body */}
    <g transform="translate(72,60)">
      {/* Chair */}
      <rect x="10" y="130" width="110" height="8" rx="4" fill="#BDBDBD"/>
      <rect x="14" y="138" width="8" height="40" rx="4" fill="#9E9E9E"/>
      <rect x="108" y="138" width="8" height="40" rx="4" fill="#9E9E9E"/>
      <rect x="0" y="80" width="8" height="58" rx="4" fill="#9E9E9E"/>
      {/* Torso */}
      <rect x="28" y="72" width="76" height="62" rx="16" fill="#FF7A28"/>
      {/* Collar */}
      <path d="M52 72 L66 88 L80 72" fill="#E86020"/>
      {/* Arms holding tablet */}
      <path d="M28 90 Q8 100 12 120" stroke="#FFCC99" strokeWidth="14" strokeLinecap="round" fill="none"/>
      <path d="M104 90 Q124 100 120 120" stroke="#FFCC99" strokeWidth="14" strokeLinecap="round" fill="none"/>
      {/* Tablet */}
      <rect x="20" y="112" width="92" height="64" rx="8" fill="#1E2229"/>
      <rect x="24" y="116" width="84" height="56" rx="6" fill="#42A5F5" opacity="0.9"/>
      <rect x="30" y="122" width="72" height="8" rx="3" fill="white" opacity="0.6"/>
      <rect x="30" y="134" width="50" height="6" rx="3" fill="white" opacity="0.4"/>
      <rect x="30" y="144" width="60" height="6" rx="3" fill="white" opacity="0.4"/>
      <rect x="30" y="154" width="40" height="6" rx="3" fill="white" opacity="0.4"/>
      {/* Head */}
      <ellipse cx="66" cy="52" rx="30" ry="32" fill="#FFCC99"/>
      {/* Hair */}
      <path d="M36 44 Q40 16 66 18 Q92 16 96 44 Q88 28 66 28 Q44 28 36 44Z" fill="#4E342E"/>
      {/* Eyes */}
      <ellipse cx="54" cy="52" rx="4" ry="5" fill="#1E2229"/>
      <ellipse cx="78" cy="52" rx="4" ry="5" fill="#1E2229"/>
      <circle cx="55.5" cy="50.5" r="1.5" fill="white"/>
      <circle cx="79.5" cy="50.5" r="1.5" fill="white"/>
      {/* Smile */}
      <path d="M56 62 Q66 70 76 62" stroke="#E8834A" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
    </g>
  </svg>
);

/** Screen B: Chef in red hat stirring a steaming pot */
const IllustrationChef: React.FC = () => (
  <svg viewBox="0 0 280 260" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    {/* Floating spice stars */}
    <g fill="#FF7A28" opacity="0.7">
      <polygon points="30,30 33,22 36,30 44,30 38,35 40,43 33,38 26,43 28,35 22,30" transform="scale(0.7) translate(10,10)"/>
      <polygon points="240,50 243,42 246,50 254,50 248,55 250,63 243,58 236,63 238,55 232,50" transform="scale(0.6) translate(130,30)"/>
    </g>
    {/* Steam wisps */}
    <g stroke="#B0BEC5" strokeWidth="3" strokeLinecap="round" fill="none" opacity="0.7">
      <path d="M108 108 Q112 96 108 84"/>
      <path d="M140 104 Q144 90 140 76"/>
      <path d="M172 108 Q176 96 172 84"/>
    </g>
    {/* Pot */}
    <ellipse cx="140" cy="200" rx="68" ry="14" fill="#546E7A" opacity="0.4"/>
    <rect x="72" y="130" width="136" height="72" rx="12" fill="#607D8B"/>
    <rect x="72" y="130" width="136" height="20" rx="10" fill="#546E7A"/>
    {/* Pot handles */}
    <rect x="44" y="138" width="32" height="14" rx="7" fill="#455A64"/>
    <rect x="204" y="138" width="32" height="14" rx="7" fill="#455A64"/>
    {/* Soup surface */}
    <ellipse cx="140" cy="130" rx="68" ry="14" fill="#FF8F00" opacity="0.85"/>
    <ellipse cx="120" cy="128" rx="14" ry="5" fill="#FFB300" opacity="0.6"/>
    <ellipse cx="158" cy="132" rx="10" ry="4" fill="#FFB300" opacity="0.6"/>
    {/* Chef body */}
    <rect x="90" y="72" width="100" height="70" rx="18" fill="white"/>
    {/* Chef coat buttons */}
    <circle cx="140" cy="88" r="3" fill="#E0E0E0"/>
    <circle cx="140" cy="102" r="3" fill="#E0E0E0"/>
    <circle cx="140" cy="116" r="3" fill="#E0E0E0"/>
    {/* Arms */}
    <path d="M90 95 Q60 105 72 138" stroke="white" strokeWidth="22" strokeLinecap="round" fill="none"/>
    <path d="M190 95 Q220 105 208 138" stroke="white" strokeWidth="22" strokeLinecap="round" fill="none"/>
    {/* Hands */}
    <ellipse cx="74" cy="140" rx="12" ry="10" fill="#FFCC99"/>
    <ellipse cx="206" cy="140" rx="12" ry="10" fill="#FFCC99"/>
    {/* Ladle */}
    <line x1="80" y1="136" x2="148" y2="118" stroke="#78909C" strokeWidth="5" strokeLinecap="round"/>
    <ellipse cx="152" cy="116" rx="12" ry="9" fill="#90A4AE"/>
    {/* Neck */}
    <rect x="126" y="58" width="28" height="20" rx="8" fill="#FFCC99"/>
    {/* Head */}
    <ellipse cx="140" cy="46" rx="34" ry="36" fill="#FFCC99"/>
    {/* Chef hat */}
    <rect x="108" y="10" width="64" height="28" rx="6" fill="#E53935"/>
    <rect x="104" y="34" width="72" height="14" rx="4" fill="#E53935"/>
    <rect x="108" y="10" width="64" height="8" rx="4" fill="#EF9A9A" opacity="0.5"/>
    {/* Hat band */}
    <rect x="104" y="34" width="72" height="5" rx="2" fill="#C62828"/>
    {/* Eyes */}
    <ellipse cx="128" cy="46" rx="5" ry="6" fill="#1E2229"/>
    <ellipse cx="152" cy="46" rx="5" ry="6" fill="#1E2229"/>
    <circle cx="129.5" cy="44.5" r="2" fill="white"/>
    <circle cx="153.5" cy="44.5" r="2" fill="white"/>
    {/* Eyebrows */}
    <path d="M122 38 Q128 34 134 38" stroke="#4E342E" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
    <path d="M146 38 Q152 34 158 38" stroke="#4E342E" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
    {/* Smile */}
    <path d="M128 58 Q140 68 152 58" stroke="#E8834A" strokeWidth="2.5" fill="none" strokeLinecap="round"/>
    {/* Moustache */}
    <path d="M128 54 Q134 58 140 54 Q146 58 152 54" stroke="#4E342E" strokeWidth="2" fill="none" strokeLinecap="round"/>
  </svg>
);

/** Screen C: Delivery scooter with food bag */
const IllustrationDelivery: React.FC = () => (
  <svg viewBox="0 0 280 260" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    {/* Road */}
    <rect x="0" y="210" width="280" height="20" rx="4" fill="#ECEFF1"/>
    <rect x="20" y="218" width="40" height="5" rx="2" fill="#B0BEC5"/>
    <rect x="120" y="218" width="40" height="5" rx="2" fill="#B0BEC5"/>
    <rect x="220" y="218" width="40" height="5" rx="2" fill="#B0BEC5"/>
    {/* Rear wheel */}
    <circle cx="80" cy="200" r="30" fill="#37474F"/>
    <circle cx="80" cy="200" r="20" fill="#546E7A"/>
    <circle cx="80" cy="200" r="8" fill="#FF7A28"/>
    {/* Front wheel */}
    <circle cx="210" cy="200" r="30" fill="#37474F"/>
    <circle cx="210" cy="200" r="20" fill="#546E7A"/>
    <circle cx="210" cy="200" r="8" fill="#FF7A28"/>
    {/* Scooter body */}
    <path d="M80 170 Q100 140 140 138 L200 138 Q230 138 230 170 L210 170 Q200 155 140 155 Q110 155 100 170Z" fill="#FF7A28"/>
    <path d="M200 138 L220 120 L240 120 L230 138Z" fill="#E86020"/>
    {/* Seat */}
    <rect x="100" y="130" width="80" height="16" rx="8" fill="#1E2229"/>
    {/* Delivery box on back */}
    <rect x="44" y="120" width="60" height="52" rx="8" fill="white"/>
    <rect x="44" y="120" width="60" height="52" rx="8" stroke="#FF7A28" strokeWidth="3"/>
    <path d="M44 140 L104 140" stroke="#FF7A28" strokeWidth="2"/>
    <text x="74" y="135" textAnchor="middle" fontSize="10" fill="#FF7A28" fontWeight="bold">🍔</text>
    <text x="74" y="158" textAnchor="middle" fontSize="10" fill="#FF7A28" fontWeight="bold">FOOD</text>
    {/* Rider body */}
    <rect x="148" y="90" width="52" height="52" rx="14" fill="#FF7A28"/>
    {/* Rider head + helmet */}
    <ellipse cx="174" cy="76" rx="24" ry="26" fill="#FFCC99"/>
    <path d="M150 70 Q152 46 174 44 Q196 46 198 70 Q190 58 174 58 Q158 58 150 70Z" fill="#E53935"/>
    <rect x="148" y="68" width="52" height="10" rx="5" fill="#C62828"/>
    {/* Visor */}
    <path d="M152 72 Q174 80 196 72" stroke="#1E2229" strokeWidth="4" fill="none" strokeLinecap="round"/>
    {/* Handlebar arm */}
    <path d="M200 118 Q220 118 228 130" stroke="#FF7A28" strokeWidth="14" strokeLinecap="round" fill="none"/>
    {/* Speed lines */}
    <g stroke="#FF7A28" strokeWidth="3" strokeLinecap="round" opacity="0.5">
      <line x1="10" y1="160" x2="50" y2="160"/>
      <line x1="4" y1="175" x2="36" y2="175"/>
      <line x1="14" y1="190" x2="44" y2="190"/>
    </g>
  </svg>
);

/** Screen D: Bowl of food with chopsticks and floating ingredients */
const IllustrationBowl: React.FC = () => (
  <svg viewBox="0 0 280 260" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    {/* Floating veggies */}
    <g transform="translate(18,20)">
      <ellipse cx="20" cy="20" rx="18" ry="12" fill="#8BC34A" opacity="0.9"/>
      <ellipse cx="20" cy="16" rx="14" ry="8" fill="#AED581"/>
      <rect x="18" y="4" width="4" height="14" rx="2" fill="#558B2F"/>
    </g>
    <g transform="translate(220,30)">
      <circle cx="18" cy="18" r="18" fill="#FF5252" opacity="0.85"/>
      <circle cx="18" cy="18" r="10" fill="#FF8A80"/>
      <rect x="16" y="0" width="4" height="10" rx="2" fill="#558B2F"/>
    </g>
    <g transform="translate(14,180)">
      <ellipse cx="16" cy="12" rx="16" ry="10" fill="#FFB300" opacity="0.9"/>
      <path d="M4 12 Q16 4 28 12" fill="#FFA000"/>
    </g>
    <g transform="translate(228,175)">
      <rect x="0" y="4" width="32" height="20" rx="6" fill="#FF7A28" opacity="0.85"/>
      <rect x="4" y="0" width="6" height="10" rx="3" fill="#FFCC02"/>
      <rect x="13" y="0" width="6" height="12" rx="3" fill="#FFCC02"/>
      <rect x="22" y="0" width="6" height="8" rx="3" fill="#FFCC02"/>
    </g>
    {/* Bowl shadow */}
    <ellipse cx="140" cy="218" rx="72" ry="12" fill="#BDBDBD" opacity="0.3"/>
    {/* Bowl body */}
    <path d="M60 148 Q60 220 140 220 Q220 220 220 148Z" fill="white"/>
    <path d="M60 148 Q60 220 140 220 Q220 220 220 148Z" stroke="#E0E0E0" strokeWidth="2" fill="none"/>
    {/* Bowl rim */}
    <ellipse cx="140" cy="148" rx="80" ry="18" fill="white" stroke="#E0E0E0" strokeWidth="2"/>
    <ellipse cx="140" cy="148" rx="80" ry="18" fill="#FF7A28" opacity="0.08"/>
    {/* Noodles / food inside */}
    <path d="M90 165 Q110 155 130 168 Q150 180 170 165 Q190 152 200 168" stroke="#FFCC02" strokeWidth="6" strokeLinecap="round" fill="none"/>
    <path d="M85 178 Q105 168 125 180 Q145 192 165 178 Q185 165 205 180" stroke="#FFCC02" strokeWidth="6" strokeLinecap="round" fill="none"/>
    {/* Toppings */}
    <circle cx="120" cy="162" r="8" fill="#E53935" opacity="0.85"/>
    <circle cx="155" cy="170" r="7" fill="#E53935" opacity="0.85"/>
    <circle cx="138" cy="158" r="6" fill="#8BC34A" opacity="0.9"/>
    {/* Chopsticks */}
    <line x1="118" y1="100" x2="148" y2="160" stroke="#795548" strokeWidth="5" strokeLinecap="round"/>
    <line x1="132" y1="96" x2="158" y2="156" stroke="#795548" strokeWidth="5" strokeLinecap="round"/>
    {/* Steam */}
    <g stroke="#B0BEC5" strokeWidth="2.5" strokeLinecap="round" fill="none" opacity="0.6">
      <path d="M120 130 Q124 120 120 110"/>
      <path d="M140 126 Q144 114 140 102"/>
      <path d="M160 130 Q164 120 160 110"/>
    </g>
  </svg>
);

// ─── Slide data ───────────────────────────────────────────────────────────────

interface Slide {
  id: string;
  title: string;
  subtitle: string;
  Illustration: React.FC;
}

const SLIDES: Slide[] = [
  {
    id: '1',
    title: 'All your favorites',
    subtitle:
      'Get all your loved foods in one place, you just place the order we do the rest.',
    Illustration: IllustrationTabletMan,
  },
  {
    id: '2',
    title: 'Order from chosen chef',
    subtitle:
      'Pick from hundreds of talented chefs and get restaurant-quality meals delivered to your door.',
    Illustration: IllustrationChef,
  },
  {
    id: '3',
    title: 'Free delivery offers',
    subtitle:
      'Free delivery on your first order and exclusive deals every week just for you.',
    Illustration: IllustrationDelivery,
  },
  {
    id: '4',
    title: 'Choose your food',
    subtitle:
      'Discover the best foods from over 1,000 restaurants and fast delivery to your doorstep.',
    Illustration: IllustrationBowl,
  },
];

// ─── Dot indicator ────────────────────────────────────────────────────────────

interface DotsProps {
  total: number;
  activeIndex: number;
  onDotClick: (index: number) => void;
}

const CarouselDots: React.FC<DotsProps> = ({ total, activeIndex, onDotClick }) => (
  <div className="ob-dots" role="tablist" aria-label="Slide indicator">
    {Array.from({ length: total }).map((_, i) => (
      <button
        key={i}
        role="tab"
        aria-selected={i === activeIndex}
        aria-label={`Go to slide ${i + 1}`}
        className={`ob-dot${i === activeIndex ? ' ob-dot--active' : ''}`}
        onClick={() => onDotClick(i)}
      />
    ))}
  </div>
);

// ─── Main screen ──────────────────────────────────────────────────────────────

const OnboardingScreen: React.FC = () => {
  const navigate = useNavigate();
  const [activeIndex, setActiveIndex] = useState(0);

  const isLastSlide = activeIndex === SLIDES.length - 1;
  const goToLogin = useCallback(() => navigate('/login', { replace: true }), [navigate]);

  const handleNext = () => {
    if (isLastSlide) {
      goToLogin();
    } else {
      setActiveIndex((prev) => prev + 1);
    }
  };

  const { title, subtitle, Illustration } = SLIDES[activeIndex];

  return (
    <div className="ob-page">
      <div className="ob-viewport">

        {/* ── Illustration area ── */}
        <div className="ob-illustration-area">
          <div className="ob-illustration-circle">
            <div className="ob-illustration-inner">
              <Illustration />
            </div>
          </div>
        </div>

        {/* ── Text block ── */}
        <div className="ob-text">
          <h2 className="ob-title">{title}</h2>
          <p className="ob-subtitle">{subtitle}</p>
        </div>

        {/* ── Dots ── */}
        <CarouselDots
          total={SLIDES.length}
          activeIndex={activeIndex}
          onDotClick={setActiveIndex}
        />

        {/* ── Actions ── */}
        <div className="ob-actions">
          <button className="ob-btn-next" onClick={handleNext}>
            {isLastSlide ? 'GET STARTED' : 'NEXT'}
          </button>
          <button className="ob-btn-skip" onClick={goToLogin} aria-label="Skip onboarding">
            Skip
          </button>
        </div>

      </div>
    </div>
  );
};

export default OnboardingScreen;
