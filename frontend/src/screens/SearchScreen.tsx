import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useLocation } from '../context/LocationContext';
import { useCart } from '../context/CartContext';
import { searchRestaurants } from '../services/searchService';
import { filterAndSortRestaurants, type ProcessedRestaurant } from '../utils/geoUtils';
import './SearchScreen.css';

// ─── SVG Icons ────────────────────────────────────────────────────────────────

const BackIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="#6B7280" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <polyline points="15 18 9 12 15 6"/>
  </svg>
);

const SearchIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="#9EA1B1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <circle cx="11" cy="11" r="8"/>
    <line x1="21" y1="21" x2="16.65" y2="16.65"/>
  </svg>
);

const ClearIcon = () => (
  <svg width="14" height="14" viewBox="0 0 14 14" fill="none"
    stroke="#9EA1B1" strokeWidth="2" strokeLinecap="round"
    aria-hidden="true">
    <line x1="2" y1="2" x2="12" y2="12"/>
    <line x1="12" y1="2" x2="2" y2="12"/>
  </svg>
);

const BagIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
    stroke="white" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <path d="M6 2L3 6v14a2 2 0 002 2h14a2 2 0 002-2V6l-3-4z"/>
    <line x1="3" y1="6" x2="21" y2="6"/>
    <path d="M16 10a4 4 0 01-8 0"/>
  </svg>
);

const StarIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="#FFC107"
    aria-hidden="true">
    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
  </svg>
);


// ─── Restaurant thumbnail SVGs ────────────────────────────────────────────────

const ThumbPansi = () => (
  <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <rect width="64" height="64" rx="12" fill="#FFF3E8"/>
    {/* Plate */}
    <circle cx="32" cy="36" r="20" fill="#F5F5F5"/>
    <circle cx="32" cy="36" r="17" fill="white" stroke="#E0E0E0" strokeWidth="1"/>
    {/* Noodles */}
    <path d="M18 34 Q24 28 32 34 Q40 40 46 34" stroke="#FFCC80" strokeWidth="4"
      strokeLinecap="round" fill="none"/>
    <path d="M18 38 Q24 32 32 38 Q40 44 46 38" stroke="#FFCC80" strokeWidth="4"
      strokeLinecap="round" fill="none"/>
    {/* Toppings */}
    <circle cx="26" cy="32" r="4" fill="#E53935" opacity="0.85"/>
    <circle cx="38" cy="36" r="3.5" fill="#E53935" opacity="0.85"/>
    <circle cx="32" cy="30" r="3" fill="#66BB6A"/>
    {/* Chopsticks */}
    <line x1="22" y1="14" x2="30" y2="34" stroke="#795548" strokeWidth="2.5"
      strokeLinecap="round"/>
    <line x1="28" y1="12" x2="34" y2="32" stroke="#795548" strokeWidth="2.5"
      strokeLinecap="round"/>
  </svg>
);

const ThumbBurgerJoint = () => (
  <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <rect width="64" height="64" rx="12" fill="#FFF8E1"/>
    {/* Bun top */}
    <ellipse cx="32" cy="22" rx="18" ry="10" fill="#F4A340"/>
    <ellipse cx="32" cy="20" rx="16" ry="8" fill="#FFCC80"/>
    {/* Sesame */}
    <ellipse cx="26" cy="18" rx="2.5" ry="1.5" fill="#D4A017" opacity="0.7"/>
    <ellipse cx="33" cy="16" rx="2.5" ry="1.5" fill="#D4A017" opacity="0.7"/>
    <ellipse cx="39" cy="19" rx="2.5" ry="1.5" fill="#D4A017" opacity="0.7"/>
    {/* Lettuce */}
    <rect x="14" y="30" width="36" height="6" rx="2" fill="#8BC34A"/>
    {/* Patty */}
    <rect x="14" y="35" width="36" height="8" rx="3" fill="#795548"/>
    {/* Cheese */}
    <rect x="12" y="33" width="40" height="5" rx="2" fill="#FFC107" opacity="0.9"/>
    {/* Bun bottom */}
    <ellipse cx="32" cy="46" rx="18" ry="7" fill="#E8834A"/>
  </svg>
);

const ThumbPizzaPlace = () => (
  <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <rect width="64" height="64" rx="12" fill="#FFF3E8"/>
    <circle cx="32" cy="34" r="22" fill="#FF7622" opacity="0.15"/>
    <circle cx="32" cy="34" r="19" fill="#FFCC80"/>
    <circle cx="32" cy="34" r="15" fill="#FF7622" opacity="0.85"/>
    <circle cx="32" cy="34" r="11" fill="#FFCC80"/>
    {[
      [26,28],[36,26],[28,38],[38,36],[32,32]
    ].map(([cx,cy],i) => (
      <circle key={i} cx={cx} cy={cy} r="3.5" fill="#E53935" opacity="0.85"/>
    ))}
    <circle cx="32" cy="34" r="19" fill="none"
      stroke="#E8834A" strokeWidth="5" strokeDasharray="8 5"/>
  </svg>
);

const ThumbSaladBar = () => (
  <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <rect width="64" height="64" rx="12" fill="#E8F5E9"/>
    <circle cx="32" cy="36" r="20" fill="#F5F5F5"/>
    <circle cx="32" cy="36" r="17" fill="white" stroke="#E0E0E0" strokeWidth="1"/>
    {/* Greens */}
    <ellipse cx="26" cy="32" rx="9" ry="6" fill="#66BB6A" opacity="0.9"
      transform="rotate(-15 26 32)"/>
    <ellipse cx="38" cy="30" rx="8" ry="5" fill="#8BC34A" opacity="0.9"
      transform="rotate(10 38 30)"/>
    <ellipse cx="32" cy="38" rx="10" ry="6" fill="#4CAF50" opacity="0.8"
      transform="rotate(-5 32 38)"/>
    {/* Tomatoes */}
    <circle cx="24" cy="38" r="4" fill="#E53935" opacity="0.85"/>
    <circle cx="40" cy="36" r="3.5" fill="#E53935" opacity="0.85"/>
    {/* Croutons */}
    <rect x="29" y="30" width="6" height="6" rx="2" fill="#F4A340"/>
    <rect x="34" y="38" width="5" height="5" rx="1.5" fill="#F4A340"/>
    {/* Dressing drizzle */}
    <path d="M20 34 Q28 28 36 34 Q40 37 44 34" stroke="#FFF176"
      strokeWidth="2" fill="none" strokeLinecap="round" opacity="0.8"/>
  </svg>
);

const ThumbSushiSpot = () => (
  <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
    <rect width="64" height="64" rx="12" fill="#E3F2FD"/>
    {/* Plate */}
    <ellipse cx="32" cy="42" rx="22" ry="8" fill="#F5F5F5"/>
    <ellipse cx="32" cy="40" rx="20" ry="6" fill="white" stroke="#E0E0E0" strokeWidth="1"/>
    {/* Sushi roll 1 */}
    <ellipse cx="22" cy="34" rx="8" ry="10" fill="#1A1A2E"/>
    <ellipse cx="22" cy="34" rx="6" ry="8" fill="white"/>
    <ellipse cx="22" cy="34" rx="4" ry="5" fill="#E53935" opacity="0.85"/>
    <ellipse cx="22" cy="34" rx="2" ry="2.5" fill="#FF8A65"/>
    {/* Sushi roll 2 */}
    <ellipse cx="38" cy="33" rx="8" ry="10" fill="#1A1A2E"/>
    <ellipse cx="38" cy="33" rx="6" ry="8" fill="white"/>
    <ellipse cx="38" cy="33" rx="4" ry="5" fill="#FF7622" opacity="0.85"/>
    <ellipse cx="38" cy="33" rx="2" ry="2.5" fill="#FFCC80"/>
    {/* Wasabi + ginger */}
    <ellipse cx="32" cy="40" rx="4" ry="2.5" fill="#66BB6A" opacity="0.8"/>
    <ellipse cx="32" cy="38" rx="3" ry="2" fill="#FFCDD2" opacity="0.8"/>
    {/* Chopsticks */}
    <line x1="10" y1="18" x2="24" y2="36" stroke="#795548" strokeWidth="2"
      strokeLinecap="round"/>
    <line x1="14" y1="16" x2="26" y2="34" stroke="#795548" strokeWidth="2"
      strokeLinecap="round"/>
  </svg>
);


// ─── Data ─────────────────────────────────────────────────────────────────────

const RECENT_KEYWORDS = ['Burger', 'Sandwich', 'Pizza', 'Sushi', 'Chicken', 'Salad'];

interface SuggestedRestaurant {
  id: string;
  name: string;
  cuisine: string;
  rating: string;
  Thumb: React.FC;
}

const SUGGESTED: SuggestedRestaurant[] = [
  { id: '1', name: 'Pansi Restaurant',    cuisine: 'Noodles · Asian · Rice',    rating: '4.7', Thumb: ThumbPansi       },
  { id: '2', name: 'Burger Joint',        cuisine: 'Burgers · American · Fries', rating: '4.5', Thumb: ThumbBurgerJoint },
  { id: '3', name: 'Pizza Palace',        cuisine: 'Pizza · Italian · Pasta',    rating: '4.8', Thumb: ThumbPizzaPlace  },
  { id: '4', name: 'The Salad Bar',       cuisine: 'Salads · Healthy · Wraps',   rating: '4.3', Thumb: ThumbSaladBar    },
  { id: '5', name: 'Sushi Spot',          cuisine: 'Sushi · Japanese · Rolls',   rating: '4.9', Thumb: ThumbSushiSpot   },
];

// ─── Component ────────────────────────────────────────────────────────────────

const SearchScreen: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { coords } = useLocation();
  const { items } = useCart();
  
  const [query, setQuery] = useState('');
  const [hasSearched, setHasSearched] = useState(false);
  const [restaurants, setRestaurants] = useState<ProcessedRestaurant[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Initialize from URL query parameter if present
  useEffect(() => {
    const urlQuery = searchParams.get('q');
    if (urlQuery) {
      setQuery(urlQuery);
      // Automatically search if query is provided in URL
      performSearch(urlQuery);
    } else {
      inputRef.current?.focus();
    }
  }, [searchParams]);

  // Execute search with query and coordinates
  const performSearch = async (searchQuery: string) => {
    if (!searchQuery.trim()) return;
    
    console.log('[SearchScreen] performSearch called with:', searchQuery);
    setHasSearched(true);
    setIsLoading(true);
    setError(null);

    try {
      console.log('[SearchScreen] Available coords:', coords);
      
      // Use coords if available, otherwise use default coordinates
      const lat = coords?.lat ?? 28.5672578;  // Default to Jamia Nagar
      const lng = coords?.lng ?? 77.2893970;
      
      console.log('[SearchScreen] Using coordinates:', { lat, lng });
      
      // Always make the search call with coordinates
      const results = await searchRestaurants(searchQuery.trim(), lat, lng, 50);
      console.log('[SearchScreen] Got results:', results);
      
      const processed = filterAndSortRestaurants(results, lat, lng, 50, searchQuery.trim());
      console.log('[SearchScreen] Processed results:', processed);
      
      setRestaurants(processed);
    } catch (err) {
      console.error('[SearchScreen] Search error:', err);
      setError(err instanceof Error ? err.message : 'Failed to search restaurants');
      setRestaurants([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (query.trim()) {
      performSearch(query);
    }
  };

  const handleKeywordClick = (kw: string) => {
    setQuery(kw);
    setQuery(kw); // Set state twice to ensure it's updated before search
    performSearch(kw);
  };

  const handleClear = () => {
    setQuery('');
    setHasSearched(false);
    setRestaurants([]);
    setError(null);
    inputRef.current?.focus();
  };

  return (
    <div className="sh-page">
      <div className="sh-viewport">

        {/* ── Navigation header ── */}
        <header className="sh-header">
          <button
            className="sh-header__icon-btn sh-header__icon-btn--light"
            onClick={() => navigate(-1)}
            aria-label="Go back"
          >
            <BackIcon />
          </button>

          <h1 className="sh-header__title">Search</h1>

          <button
            className="sh-header__icon-btn sh-header__icon-btn--dark"
            onClick={() => navigate('/checkout')}
            aria-label={`Shopping cart, ${items.length} items`}
          >
            <BagIcon />
            {items.length > 0 && (
              <span className="sh-header__badge" aria-hidden="true">{items.length}</span>
            )}
          </button>
        </header>

        {/* ── Scrollable content ── */}
        <div className="sh-body">

          {/* ── Search input ── */}
          <form className="sh-input-wrap" onSubmit={handleSearchSubmit}>
            <span className="sh-input__icon"><SearchIcon /></span>
            <input
              ref={inputRef}
              className="sh-input__field"
              type="search"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search dishes, restaurants"
              aria-label="Search"
            />
            {query && (
              <button
                type="button"
                className="sh-input__clear"
                onClick={handleClear}
                aria-label="Clear search"
              >
                <ClearIcon />
              </button>
            )}
          </form>

          {/* ── Recent keywords ── */}
          {!hasSearched && (
            <section className="sh-section" aria-label="Recent keywords">
              <h2 className="sh-section__title">Recent Keywords</h2>
              <div className="sh-keywords" role="list">
                {RECENT_KEYWORDS.map((kw) => (
                  <button
                    key={kw}
                    role="listitem"
                    className={`sh-keyword${query === kw ? ' sh-keyword--active' : ''}`}
                    onClick={() => handleKeywordClick(kw)}
                    aria-pressed={query === kw}
                  >
                    {kw}
                  </button>
                ))}
              </div>
            </section>
          )}

          {/* ── Search results or empty state ── */}
          {!hasSearched ? (
            <section className="sh-section sh-empty-state" aria-label="Start searching">
              <div className="sh-empty-message">
                <div className="sh-empty-icon">🔍</div>
                <p className="sh-empty-text">Search for your favorite dishes or restaurants</p>
              </div>
            </section>
          ) : (
            <section className="sh-section" aria-label="Search results">
              <h2 className="sh-section__title">
                {query.trim() ? `Results for "${query}"` : 'Results'}
              </h2>

              {isLoading && (
                <p className="sh-status">Searching restaurants…</p>
              )}

              {error && (
                <p className="sh-status sh-status--error">{error}</p>
              )}

              {!isLoading && restaurants.length === 0 && !error && (
                <p className="sh-empty">No restaurants found for "{query}"</p>
              )}

              {restaurants.length > 0 && (
                <ul className="sh-results-list" role="list">
                  {restaurants.map((r) => (
                    <li
                      key={r.id}
                      className="sh-result-card"
                      role="listitem"
                      onClick={() => navigate(`/restaurant/${r.id}`)}
                    >
                      {/* Restaurant thumbnail */}
                      <div className="sh-result-thumb">
                        {r.imageUrl && r.imageUrl.trim() ? (
                          <img
                            src={r.imageUrl}
                            alt={r.name}
                            className="sh-result-img"
                            loading="lazy"
                          />
                        ) : (
                          <div className="sh-result-img-fallback" aria-hidden="true" />
                        )}
                      </div>

                      {/* Restaurant info */}
                      <div className="sh-result-info">
                        <h3 className="sh-result-name">{r.name}</h3>
                        <p className="sh-result-tags">{r.tags}</p>
                        <div className="sh-result-meta">
                          {typeof r.rating === 'number' && r.rating > 0 && (
                            <span className="sh-result-rating">
                              <StarIcon />
                              {r.rating}
                              {r.totalRatings && r.totalRatings > 0 && (
                                <span className="sh-result-count">({r.totalRatings})</span>
                              )}
                            </span>
                          )}
                          <span className="sh-result-eta">{r.deliveryMins} min</span>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )}

        </div>
      </div>
    </div>
  );
};

export default SearchScreen;
