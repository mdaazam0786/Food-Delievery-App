import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import MenuCategoryAccordion from '../components/menu/MenuCategoryAccordion';
import { getRestaurantDetails, getMenuItems, type RestaurantResponse, type MenuItemResponse } from '../services/restaurantService';
import './RestaurantProfileScreen.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const BackIconWhite = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
    aria-hidden="true">
    <polyline points="15 18 9 12 15 6"/>
  </svg>
);

const LocationIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5A2.5 2.5 0 1112 6a2.5 2.5 0 010 5.5z" fill="#FF7622"/>
  </svg>
);

const ClockIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#FF7622"/>
    <path d="M12 6v6l4 2" stroke="white" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const SearchIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#9CA3AF" strokeWidth="2" aria-hidden="true">
    <circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>
  </svg>
);

const VegBadgeIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="4" y="4" width="16" height="16" rx="2" stroke="#22C55E" strokeWidth="2"/>
  </svg>
);

const NonVegBadgeIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polygon points="12,4 20,16 4,16" stroke="#DC2626" strokeWidth="2" fill="none"/>
  </svg>
);

const MenuBookIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="white" aria-hidden="true">
    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20M4 4.5A2.5 2.5 0 0 1 6.5 7H20"/>
    <path d="M4 4.5h16v12H4z" stroke="white" fill="none" strokeWidth="1.5"/>
  </svg>
);

// ─── Loading skeleton ──────────────────────────────────────────────────────────

const LoadingSkeleton: React.FC = () => (
  <div className="rps-loading" role="status" aria-label="Loading restaurant">
    <div className="rps-loading__banner" />
    <div className="rps-loading__header">
      <div className="rps-loading__title" />
      <div className="rps-loading__desc" />
    </div>
    <div className="rps-loading__grid">
      {Array.from({ length: 6 }).map((_, i) => (
        <div key={i} className="rps-loading__card" />
      ))}
    </div>
  </div>
);

// ─── Empty state ──────────────────────────────────────────────────────────────

const EmptyMenuState: React.FC = () => (
  <div className="rps-empty" role="status" aria-live="polite">
    <svg width="64" height="64" viewBox="0 0 64 64" fill="none" aria-hidden="true">
      <rect x="12" y="12" width="40" height="40" rx="8" fill="#F0F2F8"/>
      <path d="M28 32h8M32 28v8" stroke="#D0D5DD" strokeWidth="2" strokeLinecap="round"/>
    </svg>
    <p className="rps-empty__title">No menu items available</p>
    <p className="rps-empty__desc">This restaurant hasn't added menu items yet.</p>
  </div>
);

// ─── Error state ──────────────────────────────────────────────────────────────

const ErrorState: React.FC<{ error: string; onRetry: () => void }> = ({ error, onRetry }) => (
  <div className="rps-error" role="alert" aria-live="assertive">
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="#EF4444" strokeWidth="2"/>
      <line x1="15" y1="9" x2="9" y2="15" stroke="#EF4444" strokeWidth="2.5" strokeLinecap="round"/>
      <line x1="9" y1="9" x2="15" y2="15" stroke="#EF4444" strokeWidth="2.5" strokeLinecap="round"/>
    </svg>
    <p className="rps-error__msg">{error}</p>
    <button className="rps-error__retry" onClick={onRetry}>Try Again</button>
  </div>
);

// ─── Main component ────────────────────────────────────────────────────────────

const RestaurantProfileScreen: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { items } = useCart();

  const [restaurant, setRestaurant] = useState<RestaurantResponse | null>(null);
  const [menuItems, setMenuItems] = useState<MenuItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Search and filter state
  const [searchQuery, setSearchQuery] = useState('');
  const [showVegOnly, setShowVegOnly] = useState(false);
  const [showNonVegOnly, setShowNonVegOnly] = useState(false);
  const [showBestseller, setShowBestseller] = useState(false);

  // Extract unique categories from menu items
  const categories = Array.from(
    new Set(menuItems.map((item) => item.category).filter(Boolean))
  );

  // Fetch restaurant and menu data
  useEffect(() => {
    if (!id) {
      setError('Restaurant ID not found.');
      setLoading(false);
      return;
    }

    const fetchRestaurant = async () => {
      setLoading(true);
      setError(null);
      try {
        const [resData, menuData] = await Promise.all([
          getRestaurantDetails(id),
          getMenuItems(id),
        ]);
        setRestaurant(resData);
        setMenuItems(menuData);
        console.log('Menu items loaded:', menuData.map(item => ({ name: item.name, isVeg: item.isVeg })));
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load restaurant');
      } finally {
        setLoading(false);
      }
    };

    fetchRestaurant();
  }, [id]);

  const handleMenuItemAdd = (item: MenuItemResponse) => {
    // TODO: Add to cart functionality
    console.log('Add to cart:', item);
  };

  // Filter menu items based on search and dietary preferences
  const filteredMenuItems = menuItems.filter((item) => {
    // Search filter (by name or description)
    const matchesSearch = searchQuery === '' || 
      item.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      item.description.toLowerCase().includes(searchQuery.toLowerCase());

    // Dietary filters using the isVeg field from backend
    console.log(`Item: ${item.name}, isVeg: ${item.isVeg}, showVegOnly: ${showVegOnly}, showNonVegOnly: ${showNonVegOnly}`);
    
    if (showVegOnly && !item.isVeg) {
      console.log(`Filtering out ${item.name} - Veg only requested but item is non-veg`);
      return false;
    }
    if (showNonVegOnly && item.isVeg) {
      console.log(`Filtering out ${item.name} - Non-veg only requested but item is veg`);
      return false;
    }
    
    // Bestseller filter would require a bestseller flag from backend
    // if (showBestseller && !item.bestseller) return false;

    return matchesSearch;
  });

  const handleRetry = () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    getRestaurantDetails(id)
      .then((resData) => {
        setRestaurant(resData);
        return getMenuItems(id);
      })
      .then((menuData) => {
        setMenuItems(menuData);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : 'Failed to load restaurant');
      })
      .finally(() => {
        setLoading(false);
      });
  };

  if (loading) {
    return <LoadingSkeleton />;
  }

  if (error) {
    return <ErrorState error={error} onRetry={handleRetry} />;
  }

  if (!restaurant) {
    return (
      <ErrorState
        error="Restaurant not found."
        onRetry={() => navigate(-1)}
      />
    );
  }

  return (
    <div className="rps">
      {/* ── Restaurant Header Card ── */}
      <div className="rps-header-card">
        {/* Back button */}
        <button
          className="rps-header-card__back"
          onClick={() => navigate(-1)}
          aria-label="Go back"
        >
          <BackIconWhite />
        </button>

        {/* Restaurant image banner (minimal) */}
        <div className="rps-header-card__image">
          {restaurant.imageUrl && restaurant.imageUrl.trim() ? (
            <img
              src={restaurant.imageUrl}
              alt={restaurant.name}
              loading="lazy"
              onError={(e) => {
                (e.currentTarget as HTMLImageElement).style.display = 'none';
              }}
            />
          ) : (
            <div className="rps-header-card__image-fallback" aria-hidden="true" />
          )}
        </div>

        {/* Main info card */}
        <div className="rps-header-card__container">
          <div className="rps-header-card__info">
            <div className="rps-header-card__header">
              <h1 className="rps-header-card__name">{restaurant.name}</h1>
              {/* Rating badge would go here if restaurant data included ratings */}
            </div>

            {/* Average price tag */}
            <p className="rps-header-card__price">₹250 for two</p>

            {/* Location and delivery time timeline */}
            <div className="rps-header-card__timeline">
              <div className="rps-header-card__timeline-item">
                <LocationIcon />
                <span className="rps-header-card__timeline-text">Noor Nagar, Okhla</span>
              </div>
              <div className="rps-header-card__timeline-dot" />
              <div className="rps-header-card__timeline-item">
                <ClockIcon />
                <span className="rps-header-card__timeline-text">30-35 mins</span>
              </div>
            </div>

            <p className="rps-header-card__description">{restaurant.description}</p>
          </div>
        </div>
      </div>

      {/* ── Menu section ── */}
      <section className="rps-menu">
        {/* Search bar */}
        <div className="rps-search-container">
          <div className="rps-search">
            <input
              type="search"
              className="rps-search__input"
              placeholder="Search for dishes"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search dishes"
            />
            <SearchIcon />
          </div>

          {/* Dietary filters */}
          <div className="rps-filters">
            <button
              className={`rps-filter-btn ${showVegOnly ? 'rps-filter-btn--active' : ''}`}
              onClick={() => {
                setShowVegOnly(!showVegOnly);
                if (showNonVegOnly) setShowNonVegOnly(false);
              }}
              aria-label="Toggle vegetarian only"
              aria-pressed={showVegOnly}
            >
              <VegBadgeIcon />
              <span>Veg Only</span>
            </button>

            <button
              className={`rps-filter-btn ${showNonVegOnly ? 'rps-filter-btn--active' : ''}`}
              onClick={() => {
                setShowNonVegOnly(!showNonVegOnly);
                if (showVegOnly) setShowVegOnly(false);
              }}
              aria-label="Toggle non-vegetarian only"
              aria-pressed={showNonVegOnly}
            >
              <NonVegBadgeIcon />
              <span>Non-Veg Only</span>
            </button>

            <button
              className={`rps-filter-btn ${showBestseller ? 'rps-filter-btn--active' : ''}`}
              onClick={() => setShowBestseller(!showBestseller)}
              aria-label="Toggle bestseller filter"
              aria-pressed={showBestseller}
            >
              <span className="rps-filter-btn__star">⭐</span>
              <span>Bestseller</span>
            </button>
          </div>
        </div>
        {/* Menu items grouped by category in accordions */}
        {filteredMenuItems.length > 0 ? (
          <div className="rps-menu-list">
            {categories.map((category) => {
              const categoryItems = filteredMenuItems.filter((item) => item.category === category);
              if (categoryItems.length === 0) return null;

              return (
                <MenuCategoryAccordion
                  key={category}
                  category={category}
                  items={categoryItems}
                  restaurantId={restaurant.id}
                  onItemAdd={handleMenuItemAdd}
                />
              );
            })}
          </div>
        ) : (
          <EmptyMenuState />
        )}
      </section>

      {/* ── Floating Menu Button ── */}
      <button className="rps-float-menu" aria-label="Open menu" title="Open menu">
        <MenuBookIcon />
        <span className="rps-float-menu__text">MENU</span>
      </button>

      {/* ── Floating Cart Banner ── */}
      {items.length > 0 && (
        <div className="rps-cart-banner">
          <div className="rps-cart-banner__text">
            <span className="rps-cart-banner__icon">🛒</span>
            <span className="rps-cart-banner__count">{items.length} items added</span>
          </div>
          <button 
            className="rps-cart-banner__button"
            onClick={() => navigate('/checkout')}
          >
            VIEW CART
          </button>
        </div>
      )}
    </div>
  );
};

export default RestaurantProfileScreen;
