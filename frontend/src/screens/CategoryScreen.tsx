import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './CategoryScreen.css';

// ─── SVG Icons ────────────────────────────────────────────────────────────────

const BackIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="#6B7280" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <polyline points="15 18 9 12 15 6"/>
  </svg>
);

const ChevronDownIcon = () => (
  <svg width="12" height="8" viewBox="0 0 12 8" fill="none"
    stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <polyline points="1 1 6 7 11 1"/>
  </svg>
);

const SearchIconDark = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="white" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <circle cx="11" cy="11" r="8"/>
    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
  </svg>
);

const SlidersIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="#6B7280" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <line x1="4"  y1="6"  x2="20" y2="6"/>
    <line x1="4"  y1="12" x2="20" y2="12"/>
    <line x1="4"  y1="18" x2="20" y2="18"/>
    <circle cx="8"  cy="6"  r="2.5" fill="white" stroke="#6B7280" strokeWidth="2"/>
    <circle cx="16" cy="12" r="2.5" fill="white" stroke="#6B7280" strokeWidth="2"/>
    <circle cx="10" cy="18" r="2.5" fill="white" stroke="#6B7280" strokeWidth="2"/>
  </svg>
);

const PlusIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round"
    aria-hidden="true">
    <line x1="8" y1="2" x2="8" y2="14"/>
    <line x1="2" y1="8" x2="14" y2="8"/>
  </svg>
);


// ─── Burger product SVG illustrations ────────────────────────────────────────

const BurgerBistro = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    {/* Plate shadow */}
    <ellipse cx="80" cy="128" rx="58" ry="10" fill="rgba(0,0,0,0.07)"/>
    {/* Bun top */}
    <ellipse cx="80" cy="52" rx="54" ry="30" fill="#F4A340"/>
    <ellipse cx="80" cy="46" rx="50" ry="26" fill="#FFCC80"/>
    <ellipse cx="80" cy="40" rx="46" ry="20" fill="#F4A340"/>
    {/* Sesame seeds */}
    {[[62,36],[76,32],[90,35],[70,42],[84,40],[96,38]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#D4A017" opacity="0.75"/>
    ))}
    {/* Lettuce ruffle */}
    <path d="M26 76 Q36 68 46 76 Q56 68 66 76 Q76 68 86 76 Q96 68 106 76 Q116 68 126 76 Q130 80 126 82 Q116 74 106 82 Q96 74 86 82 Q76 74 66 82 Q56 74 46 82 Q36 74 26 82 Q22 80 26 76Z"
      fill="#66BB6A"/>
    {/* Tomato slice */}
    <ellipse cx="80" cy="86" rx="46" ry="10" fill="#E53935" opacity="0.85"/>
    <line x1="80" y1="76" x2="80" y2="96" stroke="#C62828" strokeWidth="1" opacity="0.4"/>
    <line x1="60" y1="78" x2="100" y2="94" stroke="#C62828" strokeWidth="1" opacity="0.4"/>
    <line x1="100" y1="78" x2="60" y2="94" stroke="#C62828" strokeWidth="1" opacity="0.4"/>
    {/* Cheese slice */}
    <path d="M30 90 L130 90 L134 100 L26 100 Z" fill="#FFC107" opacity="0.9"/>
    {/* Patty */}
    <ellipse cx="80" cy="106" rx="50" ry="14" fill="#5D4037"/>
    <ellipse cx="80" cy="102" rx="48" ry="12" fill="#795548"/>
    {/* Grill marks */}
    <path d="M52 100 Q60 96 68 100" stroke="#4E342E" strokeWidth="2.5"
      strokeLinecap="round" fill="none" opacity="0.6"/>
    <path d="M72 98 Q80 94 88 98" stroke="#4E342E" strokeWidth="2.5"
      strokeLinecap="round" fill="none" opacity="0.6"/>
    <path d="M92 100 Q100 96 108 100" stroke="#4E342E" strokeWidth="2.5"
      strokeLinecap="round" fill="none" opacity="0.6"/>
    {/* Bun bottom */}
    <ellipse cx="80" cy="118" rx="52" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="116" rx="50" ry="10" fill="#F4A340"/>
  </svg>
);

const SmokinBurger = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    {/* Smoke wisps */}
    <path d="M60 18 Q64 10 60 4"  stroke="#B0BEC5" strokeWidth="2.5" strokeLinecap="round" fill="none" opacity="0.6"/>
    <path d="M80 14 Q84 6  80 0"  stroke="#B0BEC5" strokeWidth="2"   strokeLinecap="round" fill="none" opacity="0.5"/>
    <path d="M100 18 Q104 10 100 4" stroke="#B0BEC5" strokeWidth="2" strokeLinecap="round" fill="none" opacity="0.5"/>
    {/* Plate shadow */}
    <ellipse cx="80" cy="130" rx="60" ry="9" fill="rgba(0,0,0,0.07)"/>
    {/* Bun top — darker, smokier */}
    <ellipse cx="80" cy="50" rx="56" ry="28" fill="#E8834A"/>
    <ellipse cx="80" cy="44" rx="52" ry="24" fill="#F4A340"/>
    <ellipse cx="80" cy="38" rx="48" ry="18" fill="#FFCC80"/>
    {/* Sesame */}
    {[[64,34],[78,30],[92,33],[70,40],[86,38]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#D4A017" opacity="0.7"/>
    ))}
    {/* Caramelised onions */}
    <path d="M28 72 Q50 62 80 66 Q110 62 132 72 Q128 80 80 78 Q32 80 28 72Z"
      fill="#FF8F00" opacity="0.85"/>
    {/* Bacon strips */}
    <path d="M28 80 Q50 74 80 78 Q110 74 132 80 Q128 88 80 86 Q32 88 28 80Z"
      fill="#C62828" opacity="0.9"/>
    <path d="M32 82 Q56 78 80 82 Q104 78 128 82" stroke="#B71C1C"
      strokeWidth="1.5" fill="none" opacity="0.5"/>
    {/* Cheese — double layer */}
    <path d="M26 88 L134 88 L138 98 L22 98 Z" fill="#FFC107" opacity="0.95"/>
    <path d="M28 94 L132 94 L136 102 L24 102 Z" fill="#FFD54F" opacity="0.7"/>
    {/* Patty — thick smash */}
    <ellipse cx="80" cy="110" rx="52" ry="15" fill="#4E342E"/>
    <ellipse cx="80" cy="106" rx="50" ry="13" fill="#6D4C41"/>
    {/* Crispy edges */}
    <path d="M30 108 Q40 102 50 108 Q60 102 70 108 Q80 102 90 108 Q100 102 110 108 Q120 102 130 108"
      stroke="#3E2723" strokeWidth="3" fill="none" strokeLinecap="round" opacity="0.5"/>
    {/* Bun bottom */}
    <ellipse cx="80" cy="120" rx="54" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="118" rx="52" ry="10" fill="#F4A340"/>
  </svg>
);

const CrispyChickenBurger = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    {/* Bun top — lighter golden */}
    <ellipse cx="80" cy="48" rx="54" ry="28" fill="#FFCC80"/>
    <ellipse cx="80" cy="42" rx="50" ry="24" fill="#FFE0B2"/>
    <ellipse cx="80" cy="36" rx="46" ry="18" fill="#FFCC80"/>
    {/* Sesame */}
    {[[62,32],[76,28],[90,31],[68,38],[84,36],[96,34]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3" ry="1.8" fill="#D4A017" opacity="0.7"/>
    ))}
    {/* Coleslaw */}
    <path d="M26 72 Q50 64 80 68 Q110 64 134 72 Q130 80 80 78 Q30 80 26 72Z"
      fill="#F5F5F5"/>
    <path d="M30 74 Q55 68 80 72 Q105 68 130 74" stroke="#E0E0E0"
      strokeWidth="1" fill="none"/>
    {/* Pickles */}
    {[[46,80],[70,78],[94,80],[118,78]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="10" ry="5" fill="#8BC34A" opacity="0.85"/>
    ))}
    {/* Crispy chicken fillet */}
    <ellipse cx="80" cy="96" rx="52" ry="18" fill="#F4A340"/>
    <ellipse cx="80" cy="92" rx="50" ry="16" fill="#FFCC80"/>
    {/* Breading texture */}
    {[[50,90],[62,86],[74,90],[86,86],[98,90],[110,86]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="5" ry="3" fill="#E8834A" opacity="0.5"/>
    ))}
    {/* Sauce drip */}
    <path d="M60 96 Q64 106 62 112" stroke="#FF7622" strokeWidth="3"
      strokeLinecap="round" fill="none" opacity="0.7"/>
    <path d="M96 94 Q100 104 98 110" stroke="#FF7622" strokeWidth="2.5"
      strokeLinecap="round" fill="none" opacity="0.6"/>
    {/* Bun bottom */}
    <ellipse cx="80" cy="116" rx="52" ry="12" fill="#F4A340"/>
    <ellipse cx="80" cy="114" rx="50" ry="10" fill="#FFCC80"/>
  </svg>
);

const DoublePattyCheese = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="132" rx="60" ry="9" fill="rgba(0,0,0,0.07)"/>
    {/* Tall bun top */}
    <ellipse cx="80" cy="42" rx="56" ry="30" fill="#F4A340"/>
    <ellipse cx="80" cy="36" rx="52" ry="26" fill="#FFCC80"/>
    <ellipse cx="80" cy="30" rx="48" ry="20" fill="#F4A340"/>
    {/* Sesame */}
    {[[60,26],[74,22],[88,25],[66,32],[82,30],[94,28]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#D4A017" opacity="0.75"/>
    ))}
    {/* Lettuce */}
    <path d="M24 68 Q34 60 44 68 Q54 60 64 68 Q74 60 84 68 Q94 60 104 68 Q114 60 124 68 Q128 72 124 74 Q114 66 104 74 Q94 66 84 74 Q74 66 64 74 Q54 66 44 74 Q34 66 24 74 Q20 72 24 68Z"
      fill="#66BB6A"/>
    {/* Cheese 1 */}
    <path d="M24 74 L136 74 L140 84 L20 84 Z" fill="#FFC107" opacity="0.95"/>
    {/* Patty 1 */}
    <ellipse cx="80" cy="92" rx="52" ry="13" fill="#5D4037"/>
    <ellipse cx="80" cy="88" rx="50" ry="11" fill="#795548"/>
    {/* Cheese 2 */}
    <path d="M24 96 L136 96 L140 106 L20 106 Z" fill="#FFD54F" opacity="0.9"/>
    {/* Patty 2 */}
    <ellipse cx="80" cy="112" rx="52" ry="13" fill="#4E342E"/>
    <ellipse cx="80" cy="108" rx="50" ry="11" fill="#6D4C41"/>
    {/* Grill marks both patties */}
    {[88,108].map((y,pi)=>(
      [52,72,92].map((x,i)=>(
        <path key={`${pi}-${i}`} d={`M${x} ${y-2} Q${x+8} ${y-6} ${x+16} ${y-2}`}
          stroke="#3E2723" strokeWidth="2" strokeLinecap="round" fill="none" opacity="0.5"/>
      ))
    ))}
    {/* Bun bottom */}
    <ellipse cx="80" cy="122" rx="54" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="120" rx="52" ry="10" fill="#F4A340"/>
  </svg>
);

const SpicyJalapenoBurger = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    {/* Bun top — red-tinted spicy */}
    <ellipse cx="80" cy="50" rx="54" ry="28" fill="#E8834A"/>
    <ellipse cx="80" cy="44" rx="50" ry="24" fill="#FF8A65"/>
    <ellipse cx="80" cy="38" rx="46" ry="18" fill="#FFAB91"/>
    {/* Sesame */}
    {[[62,34],[76,30],[90,33],[70,40],[86,38]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#BF360C" opacity="0.6"/>
    ))}
    {/* Jalapeño slices */}
    {[[44,72],[60,68],[76,72],[92,68],[108,72],[124,68]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="8" ry="5" fill="#388E3C" opacity="0.9"/>
    ))}
    {[[44,72],[60,68],[76,72],[92,68],[108,72],[124,68]].map(([cx,cy],i)=>(
      <line key={`s${i}`} x1={cx-5} y1={cy} x2={cx+5} y2={cy}
        stroke="#2E7D32" strokeWidth="1" opacity="0.6"/>
    ))}
    {/* Spicy sauce */}
    <path d="M26 78 Q50 70 80 74 Q110 70 134 78 Q130 88 80 86 Q30 88 26 78Z"
      fill="#FF5722" opacity="0.85"/>
    {/* Cheese */}
    <path d="M26 88 L134 88 L138 98 L22 98 Z" fill="#FFC107" opacity="0.9"/>
    {/* Patty */}
    <ellipse cx="80" cy="108" rx="52" ry="14" fill="#4E342E"/>
    <ellipse cx="80" cy="104" rx="50" ry="12" fill="#6D4C41"/>
    {/* Grill marks */}
    {[52,72,92].map((x,i)=>(
      <path key={i} d={`M${x} 102 Q${x+8} 98 ${x+16} 102`}
        stroke="#3E2723" strokeWidth="2.5" strokeLinecap="round" fill="none" opacity="0.5"/>
    ))}
    {/* Bun bottom */}
    <ellipse cx="80" cy="120" rx="52" ry="12" fill="#E8834A"/>
    <ellipse cx="80" cy="118" rx="50" ry="10" fill="#FF8A65"/>
  </svg>
);

const MushroomSwissBurger = () => (
  <svg viewBox="0 0 160 140" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <ellipse cx="80" cy="130" rx="58" ry="9" fill="rgba(0,0,0,0.07)"/>
    {/* Bun top — dark brioche */}
    <ellipse cx="80" cy="50" rx="54" ry="28" fill="#8D6E63"/>
    <ellipse cx="80" cy="44" rx="50" ry="24" fill="#A1887F"/>
    <ellipse cx="80" cy="38" rx="46" ry="18" fill="#BCAAA4"/>
    {/* Sesame */}
    {[[62,34],[76,30],[90,33],[70,40],[86,38]].map(([cx,cy],i)=>(
      <ellipse key={i} cx={cx} cy={cy} rx="3.5" ry="2" fill="#5D4037" opacity="0.7"/>
    ))}
    {/* Sautéed mushrooms */}
    {[[44,70],[62,66],[80,70],[98,66],[116,70]].map(([cx,cy],i)=>(
      <g key={i}>
        <ellipse cx={cx} cy={cy} rx="10" ry="6" fill="#795548" opacity="0.9"/>
        <ellipse cx={cx} cy={cy-2} rx="8" ry="4" fill="#8D6E63"/>
      </g>
    ))}
    {/* Swiss cheese — holey */}
    <path d="M24 78 L136 78 L140 90 L20 90 Z" fill="#FFF9C4" opacity="0.95"/>
    <circle cx="50" cy="83" r="4" fill="#F9A825" opacity="0.4"/>
    <circle cx="72" cy="81" r="3" fill="#F9A825" opacity="0.4"/>
    <circle cx="94" cy="84" r="4" fill="#F9A825" opacity="0.4"/>
    <circle cx="116" cy="82" r="3" fill="#F9A825" opacity="0.4"/>
    {/* Patty */}
    <ellipse cx="80" cy="106" rx="52" ry="14" fill="#4E342E"/>
    <ellipse cx="80" cy="102" rx="50" ry="12" fill="#6D4C41"/>
    {/* Grill marks */}
    {[52,72,92].map((x,i)=>(
      <path key={i} d={`M${x} 100 Q${x+8} 96 ${x+16} 100`}
        stroke="#3E2723" strokeWidth="2.5" strokeLinecap="round" fill="none" opacity="0.5"/>
    ))}
    {/* Bun bottom */}
    <ellipse cx="80" cy="118" rx="52" ry="12" fill="#8D6E63"/>
    <ellipse cx="80" cy="116" rx="50" ry="10" fill="#A1887F"/>
  </svg>
);


// ─── Product data ─────────────────────────────────────────────────────────────

interface Product {
  id: string;
  name: string;
  kitchen: string;
  price: string;
  Illustration: React.FC;
}

const PRODUCTS: Product[] = [
  { id: '1', name: 'Burger Bistro',         kitchen: 'Bistro Kitchen',    price: '₹540', Illustration: BurgerBistro        },
  { id: '2', name: "Smokin' Burger",         kitchen: 'Smoke House',       price: '₹630', Illustration: SmokinBurger        },
  { id: '3', name: 'Crispy Chicken',         kitchen: 'Crispy Co.',        price: '₹495', Illustration: CrispyChickenBurger },
  { id: '4', name: 'Double Patty Cheese',    kitchen: 'Cheese Factory',    price: '₹720', Illustration: DoublePattyCheese   },
  { id: '5', name: 'Spicy Jalapeño',         kitchen: 'Spice Lab',         price: '₹585', Illustration: SpicyJalapenoBurger },
  { id: '6', name: 'Mushroom Swiss',         kitchen: 'Garden Grill',      price: '₹675', Illustration: MushroomSwissBurger },
];

// ─── Product card ─────────────────────────────────────────────────────────────

interface ProductCardProps {
  item: Product;
  onAdd: (id: string) => void;
  added: boolean;
}

const ProductCard: React.FC<ProductCardProps> = ({ item, onAdd, added }) => (
  <article className="cg-card">
    {/* Illustration area */}
    <div className="cg-card__img-wrap">
      <item.Illustration />
    </div>

    {/* Text */}
    <div className="cg-card__body">
      <h3 className="cg-card__name">{item.name}</h3>
      <p className="cg-card__kitchen">{item.kitchen}</p>

      {/* Price + add button */}
      <div className="cg-card__footer">
        <span className="cg-card__price">{item.price}</span>
        <button
          className={`cg-card__add-btn${added ? ' cg-card__add-btn--added' : ''}`}
          onClick={() => onAdd(item.id)}
          aria-label={added ? `${item.name} added to cart` : `Add ${item.name} to cart`}
        >
          {added ? (
            /* Checkmark when added */
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

// ─── Main screen ──────────────────────────────────────────────────────────────

const CATEGORIES_LIST = ['Burger', 'Pizza', 'Hot Dog', 'Sushi', 'Chicken', 'Salad'];

const CategoryScreen: React.FC = () => {
  const navigate = useNavigate();
  const [activeCategory, setActiveCategory] = useState('Burger');
  const [addedItems, setAddedItems] = useState<Set<string>>(new Set());
  const [dropdownOpen, setDropdownOpen] = useState(false);

  const handleAdd = (id: string) => {
    setAddedItems((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  return (
    <div className="cg-page">
      <div className="cg-viewport">

        {/* ── Top control bar ── */}
        <header className="cg-bar">
          {/* Back */}
          <button className="cg-bar__btn cg-bar__btn--light"
            onClick={() => navigate(-1)} aria-label="Go back">
            <BackIcon />
          </button>

          {/* Category dropdown capsule */}
          <div className="cg-bar__dropdown-wrap">
            <button
              className="cg-bar__dropdown"
              onClick={() => setDropdownOpen((o) => !o)}
              aria-haspopup="listbox"
              aria-expanded={dropdownOpen}
            >
              <span className="cg-bar__dropdown-label">
                {activeCategory.toUpperCase()}
              </span>
              <ChevronDownIcon />
            </button>

            {/* Dropdown list */}
            {dropdownOpen && (
              <ul className="cg-bar__dropdown-list" role="listbox"
                aria-label="Select category">
                {CATEGORIES_LIST.map((cat) => (
                  <li key={cat}
                    role="option"
                    aria-selected={cat === activeCategory}
                    className={`cg-bar__dropdown-item${cat === activeCategory ? ' cg-bar__dropdown-item--active' : ''}`}
                    onClick={() => { setActiveCategory(cat); setDropdownOpen(false); }}
                  >
                    {cat}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Right icon pair */}
          <div className="cg-bar__right">
            <button className="cg-bar__btn cg-bar__btn--dark"
              onClick={() => navigate('/search')} aria-label="Search">
              <SearchIconDark />
            </button>
            <button className="cg-bar__btn cg-bar__btn--light"
              aria-label="Filter options">
              <SlidersIcon />
            </button>
          </div>
        </header>

        {/* ── Scrollable content ── */}
        <main className="cg-main">

          {/* Section heading */}
          <div className="cg-heading">
            <h1 className="cg-heading__title">
              Popular {activeCategory}s
            </h1>
            <span className="cg-heading__count">
              {PRODUCTS.length} items
            </span>
          </div>

          {/* 2-column product grid */}
          <div className="cg-grid" role="list">
            {PRODUCTS.map((product) => (
              <div key={product.id} role="listitem">
                <ProductCard
                  item={product}
                  onAdd={handleAdd}
                  added={addedItems.has(product.id)}
                />
              </div>
            ))}
          </div>

        </main>
      </div>
    </div>
  );
};

export default CategoryScreen;
