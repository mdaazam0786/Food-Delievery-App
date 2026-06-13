import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './ItemDetailScreen.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const BackIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="#1E2229" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <polyline points="15 18 9 12 15 6"/>
  </svg>
);

const HeartIcon = ({ filled }: { filled: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path
      d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
      fill={filled ? '#FF7622' : 'none'}
      stroke={filled ? '#FF7622' : '#9EA1B1'}
      strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
    />
  </svg>
);

const StarIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="#FFC107" aria-hidden="true">
    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
  </svg>
);

const TruckIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="1" y="3" width="15" height="13" rx="2" fill="#FF7622"/>
    <path d="M16 8h4l3 5v4h-7V8z" fill="#FF7622"/>
    <circle cx="5.5"  cy="18.5" r="2.5" fill="#FF7622"/>
    <circle cx="18.5" cy="18.5" r="2.5" fill="#FF7622"/>
  </svg>
);

const ClockIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#FF7622"/>
    <path d="M12 6v6l4 2" stroke="white" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const MinusIcon = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round" aria-hidden="true">
    <line x1="2" y1="7" x2="12" y2="7"/>
  </svg>
);

const PlusIconWhite = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round" aria-hidden="true">
    <line x1="7" y1="2" x2="7" y2="12"/>
    <line x1="2" y1="7" x2="12" y2="7"/>
  </svg>
);


// ─── Pizza Calzone hero illustration ─────────────────────────────────────────

const PizzaCalzoneIllustration = () => (
  <svg viewBox="0 0 300 300" fill="none" xmlns="http://www.w3.org/2000/svg"
    className="id-hero__pizza-svg" aria-label="Pizza Calzone European" role="img">

    {/* ── Plate ── */}
    <ellipse cx="150" cy="210" rx="130" ry="22" fill="rgba(0,0,0,0.08)"/>
    <circle  cx="150" cy="158" r="128" fill="#F5F0E8"/>
    <circle  cx="150" cy="158" r="122" fill="white" stroke="#EEE8DC" strokeWidth="1.5"/>
    <circle  cx="150" cy="158" r="114" fill="#FAFAF8"/>

    {/* ── Calzone folded shape ── */}
    {/* Bottom crust half-moon */}
    <path d="M38 170 Q60 260 150 268 Q240 260 262 170 Q220 200 150 202 Q80 200 38 170Z"
      fill="#E8834A"/>
    <path d="M40 168 Q62 256 150 264 Q238 256 260 168 Q218 198 150 200 Q82 198 40 168Z"
      fill="#F4A340"/>

    {/* Top folded dome */}
    <path d="M38 170 Q36 100 150 88 Q264 100 262 170 Q220 148 150 146 Q80 148 38 170Z"
      fill="#FFCC80"/>
    <path d="M42 168 Q40 104 150 92 Q260 104 258 168 Q218 146 150 144 Q82 146 42 168Z"
      fill="#FFE0B2"/>

    {/* Crust edge highlight */}
    <path d="M38 170 Q36 100 150 88 Q264 100 262 170"
      stroke="#E8834A" strokeWidth="8" fill="none" strokeLinecap="round"/>
    <path d="M40 168 Q38 102 150 90 Q262 102 260 168"
      stroke="#F4A340" strokeWidth="4" fill="none" strokeLinecap="round" opacity="0.6"/>

    {/* Fold seam line */}
    <path d="M42 168 Q80 148 150 146 Q220 148 258 168"
      stroke="#E8834A" strokeWidth="3" fill="none" strokeLinecap="round" opacity="0.7"/>

    {/* Filling peeking out at the seam */}
    {/* Mozzarella blobs */}
    {[[90,162],[118,158],[148,156],[178,158],[208,162]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="10" ry="6"
        fill="white" opacity="0.85" stroke="#F0E8D0" strokeWidth="0.5"/>
    ))}
    {/* Prosciutto slices */}
    {[[104,164],[150,160],[196,164]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="12" ry="5"
        fill="#E57373" opacity="0.8"/>
    ))}
    {/* Mushroom slices */}
    {[[80,166],[130,162],[170,162],[220,166]].map(([cx,cy],i)=>(
      <g key={i}>
        <ellipse cx={cx} cy={cy} rx="8" ry="4" fill="#8D6E63" opacity="0.85"/>
        <ellipse cx={cx} cy={cy-2} rx="6" ry="3" fill="#A1887F"/>
      </g>
    ))}

    {/* Baked texture dots on top crust */}
    {[
      [70,130],[90,110],[115,100],[140,96],[165,98],[190,108],[210,124],[225,142],
      [60,148],[240,148]
    ].map(([cx,cy],i)=>(
      <circle key={i} cx={cx} cy={cy} r="3.5"
        fill="#D4A017" opacity={0.3 + (i % 3) * 0.15}/>
    ))}

    {/* Herb specks on crust */}
    {[[100,118],[130,104],[160,100],[185,112],[205,130]].map(([cx,cy],i)=>(
      <circle key={i} cx={cx} cy={cy} r="2" fill="#388E3C" opacity="0.5"/>
    ))}

    {/* Tomato sauce drip at seam */}
    <path d="M120 164 Q122 172 120 178" stroke="#E53935" strokeWidth="3"
      strokeLinecap="round" fill="none" opacity="0.6"/>
    <path d="M178 164 Q180 170 178 176" stroke="#E53935" strokeWidth="2.5"
      strokeLinecap="round" fill="none" opacity="0.5"/>

    {/* Olive oil sheen on top */}
    <path d="M80 130 Q110 118 140 122 Q170 118 200 130"
      stroke="rgba(255,220,100,0.3)" strokeWidth="3" fill="none" strokeLinecap="round"/>

    {/* Basil leaves on top */}
    <ellipse cx="148" cy="108" rx="9" ry="5" fill="#388E3C" opacity="0.85"
      transform="rotate(-25 148 108)"/>
    <ellipse cx="168" cy="104" rx="8" ry="4" fill="#43A047" opacity="0.8"
      transform="rotate(15 168 104)"/>
    <ellipse cx="128" cy="112" rx="7" ry="4" fill="#388E3C" opacity="0.75"
      transform="rotate(-10 128 112)"/>

    {/* Parmesan shavings */}
    {[[95,140],[155,132],[205,138]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="7" ry="3"
        fill="#FFF9C4" opacity="0.7" transform={`rotate(${-20+i*20} ${cx} ${cy})`}/>
    ))}
  </svg>
);


// ─── Ingredient line-art icons ────────────────────────────────────────────────

const SaltShakerIcon = () => (
  <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
    <rect x="10" y="14" width="12" height="14" rx="4" stroke="#9EA1B1" strokeWidth="1.8"/>
    <path d="M13 14 Q13 8 16 6 Q19 8 19 14" stroke="#9EA1B1" strokeWidth="1.8"
      strokeLinecap="round" fill="none"/>
    <circle cx="16" cy="10" r="1.5" fill="#9EA1B1"/>
    <circle cx="14" cy="19" r="1.2" fill="#9EA1B1"/>
    <circle cx="18" cy="22" r="1.2" fill="#9EA1B1"/>
    <circle cx="14" cy="24" r="1.2" fill="#9EA1B1"/>
    <circle cx="18" cy="17" r="1.2" fill="#9EA1B1"/>
  </svg>
);

const GarlicIcon = () => (
  <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
    <path d="M16 26 Q10 24 9 18 Q8 12 16 10 Q24 12 23 18 Q22 24 16 26Z"
      stroke="#9EA1B1" strokeWidth="1.8" fill="none"/>
    <path d="M16 10 Q14 6 16 4 Q18 6 16 10" stroke="#9EA1B1" strokeWidth="1.8"
      strokeLinecap="round" fill="none"/>
    <path d="M12 16 Q14 14 16 16 Q18 14 20 16" stroke="#9EA1B1" strokeWidth="1.5"
      strokeLinecap="round" fill="none"/>
    <path d="M11 20 Q13 18 16 20 Q19 18 21 20" stroke="#9EA1B1" strokeWidth="1.5"
      strokeLinecap="round" fill="none"/>
    <line x1="16" y1="24" x2="16" y2="28" stroke="#9EA1B1" strokeWidth="1.8"
      strokeLinecap="round"/>
  </svg>
);

const ChiliIcon = () => (
  <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
    <path d="M20 8 Q24 6 24 10 Q24 16 18 22 Q14 26 10 24 Q8 20 12 16 Q16 12 20 8Z"
      stroke="#9EA1B1" strokeWidth="1.8" fill="none" strokeLinecap="round"/>
    <path d="M20 8 Q22 4 20 2" stroke="#9EA1B1" strokeWidth="1.8"
      strokeLinecap="round" fill="none"/>
    <path d="M22 6 Q26 4 26 2" stroke="#9EA1B1" strokeWidth="1.5"
      strokeLinecap="round" fill="none"/>
    <path d="M14 18 Q16 16 18 18" stroke="#9EA1B1" strokeWidth="1.5"
      strokeLinecap="round" fill="none"/>
  </svg>
);

const ChickenLegIcon = () => (
  <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
    <path d="M20 6 Q26 8 26 14 Q26 20 20 22 Q16 24 14 22 L10 26 Q8 28 6 26 Q4 24 6 22 L10 18 Q8 16 10 12 Q12 6 20 6Z"
      stroke="#9EA1B1" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M20 6 Q22 4 22 2" stroke="#9EA1B1" strokeWidth="1.8"
      strokeLinecap="round" fill="none"/>
    <path d="M14 10 Q16 12 18 14 Q20 16 18 18" stroke="#9EA1B1" strokeWidth="1.5"
      strokeLinecap="round" fill="none"/>
  </svg>
);

const MushroomIcon = () => (
  <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
    <path d="M8 18 Q6 10 16 8 Q26 10 24 18Z"
      stroke="#9EA1B1" strokeWidth="1.8" fill="none" strokeLinecap="round"/>
    <path d="M12 18 L12 24 Q12 26 14 26 L18 26 Q20 26 20 24 L20 18"
      stroke="#9EA1B1" strokeWidth="1.8" strokeLinecap="round" fill="none"/>
    <path d="M12 22 L20 22" stroke="#9EA1B1" strokeWidth="1.5" strokeLinecap="round"/>
    <circle cx="13" cy="13" r="1.5" fill="#9EA1B1" opacity="0.6"/>
    <circle cx="19" cy="12" r="1.5" fill="#9EA1B1" opacity="0.6"/>
  </svg>
);

const TomatoIcon = () => (
  <svg width="32" height="32" viewBox="0 0 32 32" fill="none" aria-hidden="true">
    <circle cx="16" cy="18" r="10" stroke="#9EA1B1" strokeWidth="1.8" fill="none"/>
    <path d="M16 8 Q14 4 16 2 Q18 4 16 8" stroke="#9EA1B1" strokeWidth="1.8"
      strokeLinecap="round" fill="none"/>
    <path d="M16 8 Q12 6 10 8" stroke="#9EA1B1" strokeWidth="1.5"
      strokeLinecap="round" fill="none"/>
    <path d="M16 8 Q20 6 22 8" stroke="#9EA1B1" strokeWidth="1.5"
      strokeLinecap="round" fill="none"/>
    <line x1="16" y1="10" x2="16" y2="26" stroke="#9EA1B1" strokeWidth="1.2"
      strokeLinecap="round" opacity="0.4"/>
    <line x1="8"  y1="18" x2="24" y2="18" stroke="#9EA1B1" strokeWidth="1.2"
      strokeLinecap="round" opacity="0.4"/>
  </svg>
);

const INGREDIENTS = [
  { id: 'salt',    label: 'Salt',     Icon: SaltShakerIcon },
  { id: 'garlic',  label: 'Garlic',   Icon: GarlicIcon     },
  { id: 'chili',   label: 'Chili',    Icon: ChiliIcon      },
  { id: 'chicken', label: 'Chicken',  Icon: ChickenLegIcon },
  { id: 'mushroom',label: 'Mushroom', Icon: MushroomIcon   },
  { id: 'tomato',  label: 'Tomato',   Icon: TomatoIcon     },
];


// ─── Size options ─────────────────────────────────────────────────────────────

const SIZES = ['10"', '14"', '16"'];

// ─── Main screen ──────────────────────────────────────────────────────────────

const ItemDetailScreen: React.FC = () => {
  const navigate = useNavigate();
  const [liked,    setLiked]    = useState(false);
  const [size,     setSize]     = useState('14"');
  const [quantity, setQuantity] = useState(2);

  const unitPrice = 16;
  const total = unitPrice * quantity;

  return (
    <div className="id-page">
      <div className="id-viewport">

        {/* ── Top nav bar ── */}
        <header className="id-nav">
          <button className="id-nav__back" onClick={() => navigate(-1)}
            aria-label="Go back">
            <BackIcon />
          </button>
          <h1 className="id-nav__title">Details</h1>
          {/* Spacer to keep title centred */}
          <div className="id-nav__spacer" aria-hidden="true"/>
        </header>

        {/* ── Scrollable body ── */}
        <div className="id-body">
          <div className="id-content">

          {/* ── Hero card ── */}
          <div className="id-hero">
            {/* Pastel orange background card */}
            <div className="id-hero__card">
              {/* Pizza illustration — offset upward over the card */}
              <div className="id-hero__img-wrap">
                <PizzaCalzoneIllustration />
              </div>

              {/* Heart button — bottom-right of card */}
              <button
                className={`id-hero__heart${liked ? ' id-hero__heart--liked' : ''}`}
                onClick={() => setLiked((v) => !v)}
                aria-label={liked ? 'Remove from favourites' : 'Add to favourites'}
                aria-pressed={liked}
              >
                <HeartIcon filled={liked} />
              </button>
            </div>
          </div>

          </div>

          {/* ── Right panel: text + controls ── */}
          <div className="id-detail-panel">

          {/* ── Text detail profile ── */}
          <section className="id-detail">
            <h2 className="id-detail__name">Pizza Calzone European</h2>
            <p className="id-detail__desc">
              Prosciutto e funghi is a pizza variety featuring a classic
              combination of prosciutto cotto (cooked ham) and mushrooms.
              Folded calzone-style and baked to golden perfection with
              mozzarella, fresh basil and a rich tomato base.
            </p>

            {/* Meta badges */}
            <div className="id-detail__meta">
              <span className="id-detail__badge">
                <StarIcon />
                <span className="id-detail__badge-text">4.7</span>
              </span>
              <span className="id-detail__badge">
                <TruckIcon />
                <span className="id-detail__badge-text id-detail__badge-text--orange">
                  Free
                </span>
              </span>
              <span className="id-detail__badge">
                <ClockIcon />
                <span className="id-detail__badge-text id-detail__badge-text--orange">
                  20 min
                </span>
              </span>
            </div>
          </section>

          {/* ── Size selector ── */}
          <section className="id-size" aria-label="Select size">
            <span className="id-size__label">SIZE:</span>
            <div className="id-size__options" role="radiogroup" aria-label="Pizza size">
              {SIZES.map((s) => (
                <button
                  key={s}
                  role="radio"
                  aria-checked={size === s}
                  className={`id-size__node${size === s ? ' id-size__node--active' : ''}`}
                  onClick={() => setSize(s)}
                >
                  {s}
                </button>
              ))}
            </div>
          </section>

          {/* ── Ingredients tray ── */}
          <section className="id-ingredients" aria-label="Ingredients">
            <h3 className="id-ingredients__label">INGREDIENTS</h3>
            <div className="id-ingredients__tray">
              {INGREDIENTS.map(({ id, label, Icon }) => (
                <div key={id} className="id-ingredients__item">
                  <div className="id-ingredients__bubble" aria-hidden="true">
                    <Icon />
                  </div>
                  <span className="id-ingredients__name">{label}</span>
                </div>
              ))}
            </div>
          </section>

          {/* Bottom padding so sticky bar doesn't cover content */}
          <div className="id-bottom-pad" aria-hidden="true"/>
          </div>{/* end id-detail-panel */}
          </div>{/* end id-content */}
        </div>

        {/* ── Sticky bottom bar ── */}
        <div className="id-bar" role="region" aria-label="Order controls">
          {/* Price */}
          <div className="id-bar__price-block">
            <span className="id-bar__price-label">Total price</span>
            <span className="id-bar__price">₹{total}</span>
          </div>

          {/* Quantity adjuster */}
          <div className="id-bar__qty" role="group" aria-label="Quantity">
            <button
              className="id-bar__qty-btn"
              onClick={() => setQuantity((q) => Math.max(1, q - 1))}
              aria-label="Decrease quantity"
              disabled={quantity <= 1}
            >
              <MinusIcon />
            </button>
            <span className="id-bar__qty-count" aria-live="polite"
              aria-atomic="true">{quantity}</span>
            <button
              className="id-bar__qty-btn"
              onClick={() => setQuantity((q) => q + 1)}
              aria-label="Increase quantity"
            >
              <PlusIconWhite />
            </button>
          </div>
        </div>

    </div>
  );
};

export default ItemDetailScreen;
