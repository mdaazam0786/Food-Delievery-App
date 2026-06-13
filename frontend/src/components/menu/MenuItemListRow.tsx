import React, { useState } from 'react';
import { formatINR } from '../../utils/currencyFormatter';
import { useCart } from '../../context/CartContext';
import { useToast } from '../../context/ToastContext';
import { useCartOperations } from '../../hooks/useCartOperations';
import ConfirmDialog from '../common/ConfirmDialog';
import type { MenuItemResponse } from '../../services/restaurantService';
import './MenuItemListRow.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const VegBadgeIcon = () => (
  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="4" y="4" width="16" height="16" rx="2" stroke="#22C55E" strokeWidth="2"/>
  </svg>
);

const NonVegBadgeIcon = () => (
  <svg width="10" height="10" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polygon points="12,4 20,16 4,16" stroke="#DC2626" strokeWidth="2" fill="none"/>
  </svg>
);

const StarIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="#FFC107" aria-hidden="true">
    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
  </svg>
);

const AddIcon = () => (
  <svg width="14" height="14" viewBox="0 0 16 16" fill="none"
    stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" aria-hidden="true">
    <line x1="8" y1="2" x2="8" y2="14"/>
    <line x1="2" y1="8" x2="14" y2="8"/>
  </svg>
);

// ─── Component ────────────────────────────────────────────────────────────────

interface MenuItemListRowProps {
  item: MenuItemResponse;
  restaurantId: string;
  onAddClick?: (item: MenuItemResponse) => void;
}

const MenuItemListRow: React.FC<MenuItemListRowProps> = ({ item, restaurantId, onAddClick }) => {
  const { items: cartItems, restaurantId: cartRestaurantId, clearCart } = useCart();
  const { showToast } = useToast();
  const { addToCart } = useCartOperations();
  const [showConfirm, setShowConfirm] = useState(false);
  const [pendingItem, setPendingItem] = useState<MenuItemResponse | null>(null);
  const [isExpanded, setIsExpanded] = useState(false);

  // Use the isVeg field from the backend
  const isVeg = item.isVeg;
  console.log(`MenuItemListRow - Item: ${item.name}, isVeg: ${isVeg}, type: ${typeof isVeg}`);

  const handleAddClick = async () => {
    // Check if cart has items from a different restaurant
    if (cartItems.length > 0 && cartRestaurantId && cartRestaurantId !== restaurantId) {
      // Show confirmation dialog
      setPendingItem(item);
      setShowConfirm(true);
      return;
    }

    // Same restaurant or empty cart, add item directly
    const success = await addToCart(item, restaurantId);
    if (success) {
      onAddClick?.(item);
    }
  };

  const handleConfirmClearCart = async () => {
    setShowConfirm(false);
    if (pendingItem) {
      try {
        await clearCart();
        const success = await addToCart(pendingItem, restaurantId);
        if (success) {
          onAddClick?.(pendingItem);
        }
      } catch (err) {
        showToast('Failed to add item', 'error');
      }
      setPendingItem(null);
    }
  };

  const handleCancelClearCart = () => {
    setShowConfirm(false);
    setPendingItem(null);
  };

  return (
    <>
      <article className="mil-row">
        {/* Left column: item details */}
        <div className="mil-row__details">
          {/* Dietary badge and bestseller flag */}
          <div className="mil-row__badges">
            <div className="mil-row__badge-icon">
              {isVeg ? <VegBadgeIcon /> : <NonVegBadgeIcon />}
            </div>
            {/* TODO: Add bestseller flag when available in MenuItemResponse */}
          </div>

          {/* Item name and price */}
          <div className="mil-row__header">
            <h3 className="mil-row__name">{item.name}</h3>
            <span className="mil-row__price">{formatINR(item.price)}</span>
          </div>

          {/* Rating */}
          {typeof item.rating === 'number' && item.rating > 0 && (
            <div className="mil-row__rating">
              <StarIcon />
              <span className="mil-row__rating-text">{item.rating}</span>
              {item.totalRatings && item.totalRatings > 0 && (
                <span className="mil-row__rating-count">({item.totalRatings})</span>
              )}
            </div>
          )}

          {/* Description with expand/collapse */}
          {item.description && (
            <div className="mil-row__description-wrapper">
              <p className={`mil-row__description ${isExpanded ? 'mil-row__description--expanded' : ''}`}>
                {item.description}
              </p>
              {item.description.length > 80 && (
                <button
                  className="mil-row__expand-btn"
                  onClick={() => setIsExpanded(!isExpanded)}
                  aria-expanded={isExpanded}
                >
                  {isExpanded ? 'less' : 'more'}
                </button>
              )}
            </div>
          )}
        </div>

        {/* Right column: image and add button */}
        <div className="mil-row__image-container">
          {item.imageUrl && item.imageUrl.trim() ? (
            <img
              src={item.imageUrl}
              alt={item.name}
              className="mil-row__image"
              loading="lazy"
              onError={(e) => {
                (e.currentTarget as HTMLImageElement).style.display = 'none';
              }}
            />
          ) : (
            <div className="mil-row__image-fallback" aria-hidden="true" />
          )}

          {/* ADD button overlapping bottom-center */}
          <button
            className="mil-row__add-btn"
            onClick={handleAddClick}
            aria-label={`Add ${item.name} to cart`}
            title="Add to cart"
          >
            <AddIcon />
            <span className="mil-row__add-text">ADD</span>
          </button>
        </div>
      </article>

      {/* Confirmation dialog for multi-restaurant cart */}
      {showConfirm && (
        <ConfirmDialog
          message="Items already in cart\nYour cart contains items from another restaurant. Would you like to reset your cart for adding items from this restaurant?"
          confirmText="Yes"
          cancelText="No"
          onConfirm={handleConfirmClearCart}
          onCancel={handleCancelClearCart}
        />
      )}
    </>
  );
};

export default MenuItemListRow;
