import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import HomeSideDrawer from '../components/home/HomeSideDrawer';
import { useLocation } from '../context/LocationContext';
import { useCart } from '../context/CartContext';
import { fetchNearbyRestaurants, searchRestaurants } from '../services/searchService';
import { fetchCurrentUser, type UserProfile } from '../services/userService';
import {
  CATEGORY_IMAGES,
  RESTAURANT_FALLBACK,
} from '../constants/homeImages';
import { categoryGreeting } from '../utils/userDisplay';
import { filterAndSortRestaurants, type ProcessedRestaurant } from '../utils/geoUtils';
import './HomeScreen.css';

/* ── Main screen ──────────────────────────────────────────────────────────── */

const HomeScreen: React.FC = () => {
  const navigate = useNavigate();
  const { coords, address, loading: locLoading } = useLocation();
  const { items } = useCart();

  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [user,         setUser]         = useState<UserProfile | null>(null);
  const [userLoading,  setUserLoading]  = useState(true);
  const [restaurants,  setRestaurants]  = useState<ProcessedRestaurant[]>([]);
  const [restLoading,  setRestLoading]  = useState(false);
  const [restError,    setRestError]    = useState<string | null>(null);
  const [searchQuery,  setSearchQuery]  = useState('');

  const catTrackRef = useRef<HTMLDivElement>(null);

  const categoryTitle = categoryGreeting(user, userLoading);

  useEffect(() => {
    fetchCurrentUser()
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setUserLoading(false));
  }, []);

  useEffect(() => {
    if (!coords) return;
    let cancelled = false;
    setRestLoading(true);
    setRestError(null);

    fetchNearbyRestaurants(coords.lat, coords.lng, 5, 50)
      .then((items) => {
        if (cancelled) return;
        setRestaurants(filterAndSortRestaurants(items, coords.lat, coords.lng, 5));
      })
      .catch((err: Error) => {
        if (!cancelled) setRestError(err.message);
      })
      .finally(() => {
        if (!cancelled) setRestLoading(false);
      });

    return () => { cancelled = true; };
  }, [coords]);

  const scrollCategories = useCallback((dir: -1 | 1) => {
    catTrackRef.current?.scrollBy({ left: dir * 320, behavior: 'smooth' });
  }, []);

  const handleCategoryClick = (categoryLabel: string) => {
    if (coords) {
      // Call the search API with category as query
      searchRestaurants(categoryLabel, coords.lat, coords.lng, 50)
        .then((results) => {
          const processed = filterAndSortRestaurants(results, coords.lat, coords.lng, 50, categoryLabel);
          setRestaurants(processed);
          // Navigate to search results page
          navigate(`/search?q=${encodeURIComponent(categoryLabel)}`);
        })
        .catch((err) => {
          setRestError(err.message);
        });
    } else {
      // If no coordinates yet, just navigate to search page
      navigate(`/search?q=${encodeURIComponent(categoryLabel)}`);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim() && coords) {
      // Call the search API with query and coordinates
      searchRestaurants(searchQuery.trim(), coords.lat, coords.lng, 50)
        .then((results) => {
          const processed = filterAndSortRestaurants(results, coords.lat, coords.lng, 50, searchQuery.trim());
          setRestaurants(processed);
          // Navigate to search results page
          navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
        })
        .catch((err) => {
          setRestError(err.message);
        });
    } else if (searchQuery.trim()) {
      // If no coordinates yet, just navigate to search page
      navigate(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <div className="hf">
      <HomeSideDrawer open={isDrawerOpen} onClose={() => setIsDrawerOpen(false)} />

      {/* ── Top bar ── */}
      <header className="hf-header">
        <button
          className="hf-header__menu"
          aria-label="Open navigation menu"
          onClick={() => setIsDrawerOpen((open) => !open)}
        >
          <span /><span /><span />
        </button>

        <div className="hf-header__loc">
          <span className="hf-header__loc-label">Deliver to</span>
          <span className="hf-header__loc-addr">
            {locLoading ? 'Locating…' : address}
          </span>
        </div>

        <button 
          className="hf-header__cart" 
          aria-label="Cart"
          onClick={() => navigate('/checkout')}
        >
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 002 1.61h9.72a2 2 0 002-1.61L23 6H6"/>
          </svg>
          {items.length > 0 && (
            <span className="hf-header__cart-badge">{items.length}</span>
          )}
        </button>
      </header>

      {/* ── Hero ── */}
      <section className="hf-hero">
        <h1 className="hf-hero__headline">
          Order food &amp; groceries. Discover best restaurants.
        </h1>

        <form className="hf-hero__search-row" onSubmit={handleSearchSubmit}>
          <button type="button" className="hf-hero__addr-pill" aria-label="Delivery address">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="#FC8019" aria-hidden="true">
              <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5A2.5 2.5 0 1112 6a2.5 2.5 0 010 5.5z"/>
            </svg>
            <span className="hf-hero__addr-text">
              {locLoading ? 'Locating…' : address}
            </span>
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#888" strokeWidth="2.5" aria-hidden="true">
              <path d="M6 9l6 6 6-6"/>
            </svg>
          </button>

          <div className="hf-hero__search-pill">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#9CA3AF" strokeWidth="2.5" aria-hidden="true">
              <circle cx="11" cy="11" r="7"/><path d="M21 21l-4.35-4.35"/>
            </svg>
            <input
              type="search"
              className="hf-hero__search-input"
              placeholder="Search for restaurant, item or more"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
            />
          </div>
        </form>
      </section>

      {/* ── Category slider ── */}
      <section className="hf-categories">
        <div className="hf-categories__head">
          <h2 className="hf-categories__title">{categoryTitle}</h2>
          <div className="hf-categories__nav">
            <button
              className="hf-categories__chev"
              aria-label="Scroll categories left"
              onClick={() => scrollCategories(-1)}
            >
              ‹
            </button>
            <button
              className="hf-categories__chev"
              aria-label="Scroll categories right"
              onClick={() => scrollCategories(1)}
            >
              ›
            </button>
          </div>
        </div>

        <div className="hf-categories__track" ref={catTrackRef}>
          {CATEGORY_IMAGES.map((cat) => (
            <button 
              key={cat.id} 
              className="hf-cat" 
              type="button"
              onClick={() => handleCategoryClick(cat.label)}
              aria-label={`Search for ${cat.label}`}
            >
              <div className="hf-cat__icon">
                <img src={cat.src} alt={cat.alt} className="hf-cat__img" loading="lazy" />
              </div>
              <span className="hf-cat__label">{cat.label}</span>
            </button>
          ))}
        </div>
      </section>

      <hr className="hf-section-break" />

      {/* ── Nearby restaurants ── */}
      <section className="hf-restaurants" aria-labelledby="hf-restaurants-heading">
        <h2 id="hf-restaurants-heading" className="hf-restaurants__title">
          {'Restaurants Near You (< 5km)'}
        </h2>

        {restLoading && (
          <p className="hf-restaurants__status">Finding restaurants near you…</p>
        )}
        {restError && (
          <p className="hf-restaurants__status hf-restaurants__status--error">{restError}</p>
        )}
        {!restLoading && !restError && restaurants.length === 0 && coords && (
          <p className="hf-restaurants__status">No restaurants within 5 km.</p>
        )}
        {!coords && !locLoading && (
          <p className="hf-restaurants__status">Enable location to see nearby restaurants.</p>
        )}

        <div className="hf-restaurants__grid">
          {restaurants.map((r) => (
            <article 
              key={r.id} 
              className="hf-card"
              onClick={() => navigate(`/restaurant/${r.id}`)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  navigate(`/restaurant/${r.id}`);
                }
              }}
              aria-label={`View ${r.name} restaurant`}
            >
              <div className="hf-card__thumb">
                {r.imageUrl && r.imageUrl.trim() ? (
                  <img
                    src={r.imageUrl}
                    alt={r.name}
                    className="hf-card__img"
                    loading="lazy"
                    onError={(e) => {
                      // If the URL exists but the image fails to load, swap to fallback
                      (e.currentTarget as HTMLImageElement).src = RESTAURANT_FALLBACK;
                    }}
                  />
                ) : (
                  <div className="hf-card__img-fallback" aria-hidden="true" />
                )}
                {r.discount && 
                  typeof r.discount === 'string' && 
                  r.discount.trim() && 
                  r.discount !== '0' && 
                  !r.discount.match(/^0+%?$/) && (
                  <span className="hf-card__ribbon">{r.discount}</span>
                )}
              </div>
              <div className="hf-card__body">
                <div className="hf-card__row">
                  <h3 className="hf-card__name">{r.name}</h3>
                  {typeof r.rating === 'number' && r.rating > 0 && (
                    <span className="hf-card__rating">
                      {r.rating}★ {r.totalRatings && r.totalRatings > 0 && `(${r.totalRatings})`}
                    </span>
                  )}
                </div>
                <p className="hf-card__tags">{r.tags}</p>
                <p className="hf-card__eta">{r.deliveryMins} min</p>
              </div>
            </article>
          ))}
        </div>
      </section>

      {/* Floating Cart Banner */}
      {items.length > 0 && (
        <div className="hf-cart-banner">
          <div className="hf-cart-banner__text">
            <span className="hf-cart-banner__icon">🛒</span>
            <span className="hf-cart-banner__count">{items.length} items added</span>
          </div>
          <button 
            className="hf-cart-banner__button"
            onClick={() => navigate('/checkout')}
          >
            VIEW CART
          </button>
        </div>
      )}
    </div>
  );
};

export default HomeScreen;
