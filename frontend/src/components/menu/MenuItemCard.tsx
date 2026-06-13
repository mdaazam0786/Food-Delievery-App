import React, { useState } from 'react';
import { formatINR } from '../../utils/currencyFormatter';
import { useCart } from '../../context/CartContext';
import ConfirmDialog from '../common/ConfirmDialog';
import type { MenuItemResponse } from '../../services/restaurantService';
import './MenuItemCard.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const StarIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="#FFC107" aria-hidden="true">
    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
  </svg>
);

const PlusIcon = () => (
  <svg width="16" height="16" viewBox="0 0 16 16" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round" aria-hidden="true">
    <line x1="8" y1="2" x2="8" y2="14"/>
    <line x1="2" y1="8" x2="14" y2="8"/>
  </svg>
);

const MinusIcon = () => (
  <svg width="14" height="14" viewBox="0 0 16 16" fill="none"
    stroke="white" strokeWidth="2.5" strokeLinecap="round" aria-hidden="true">
    <line x1="2" y1="8" x2="14" y2="8"/>
  </svg>
);

// ─── Inline Quantity Counter Component ─────────────────────────────────────

interface QuantityCounterProps {
  quantity: number;
  onIncrement: () => void;
  onDecrement: () => void;
  isLoading?: boolean;
}

const QuantityCounter: React.FC<QuantityCounterProps> = ({
  quantity,
  onIncrement,
  onDecrement,
  isLoading = false,
}) => {
  return (
    <div className="mic-quantity-counter">
      <button
        className="mic-quantity-counter__btn mic-quantity-counter__btn--minus"
        onClick={onDecrement}
        disabled={isLoading}
        aria-label="Decrease quantity"
        title="Remove one item"
      >
        <MinusIcon />
      </button>
      <span className="mic-quantity-counter__count">{quantity}</span>
      <button
        className="mic-quantity-counter__btn mic-quantity-counter__btn--plus"
        onClick={onIncrement}
        disabled={isLoading}
        aria-label="Increase quantity"
        title="Add one item"
      >
        <PlusIcon />
      </button>
    </div>
  );
};

// ─── Component ────────────────────────────────────────────────────────────────

interface MenuItemCardProps {
  item: MenuItemResponse;
  restaurantId: string;
  onAddClick?: (item: MenuItemResponse) => void;
}

const MenuItemCard: React.FC<MenuItemCardProps> = ({ item, restaurantId, onAddClick }) => {
  const { items: cartItems, restaurantId: cartRestaurantId, addItem, clearCart, updateQuantity } = useCart();
  const [showConfirm, setShowConfirm] = useState(false);
  const [pendingItem, setPendingItem] = useState<MenuItemResponse | null>(null);
  const [isUpdating, setIsUpdating] = useState(false);

  // Find the quantity of this item in the cart (0 if not present)
  const cartQuantity = cartItems.find((ci) => ci.itemId === item.id)?.quantity ?? 0;

  const handleAddClick = () => {
    // Check if cart has items from a different restaurant
    if (cartItems.length > 0 && cartRestaurantId && cartRestaurantId !== restaurantId) {
      // Show confirmation dialog
      setPendingItem(item);
      setShowConfirm(true);
      return;
    }

    // Same restaurant or empty cart, add item directly
    addItem(item, restaurantId).catch(() => {
      // Error is handled by the context
    });
    onAddClick?.(item);
  };

  const handleConfirmClearCart = () => {
    setShowConfirm(false);
    if (pendingItem) {
      clearCart()
        .then(() => addItem(pendingItem, restaurantId))
        .then(() => onAddClick?.(pendingItem))
        .catch(() => {
          // Errors are handled by the context
        })
        .finally(() => {
          setPendingItem(null);
        });
    }
  };

  const handleCancelClearCart = () => {
    setShowConfirm(false);
    setPendingItem(null);
  };

  const handleQuantityIncrement = async () => {
    try {
      setIsUpdating(true);
      const newQuantity = cartQuantity + 1;
      await updateQuantity(item.id, newQuantity);
    } catch (err) {
      // Error is handled by the context
    } finally {
      setIsUpdating(false);
    }
  };

  const handleQuantityDecrement = async () => {
    try {
      setIsUpdating(true);
      const newQuantity = cartQuantity - 1;
      await updateQuantity(item.id, newQuantity);
    } catch (err) {
      // Error is handled by the context
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <>
      <article className="mic-card">
        {/* Image container */}
        <div className="mic-card__thumb">
          {item.imageUrl && item.imageUrl.trim() ? (
            <img
              src={item.imageUrl}
              alt={item.name}
              className="mic-card__img"
              loading="lazy"
              onError={(e) => {
                (e.currentTarget as HTMLImageElement).src =
                  'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PHJlY3Qgd2lkdGg9IjEwMCIgaGVpZ2h0PSIxMDAiIGZpbGw9IiNGMEYyRjgiLz48dGV4dCB4PSI1MCIgeT0iNTAiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIiBmaWxsPSIjOUVBMUIxIiBmb250LXNpemU9IjEyIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=';
              }}
            />
          ) : (
            <div className="mic-card__img-fallback" aria-hidden="true" />
          )}

          {/* Add/Counter button overlay */}
          {cartQuantity === 0 ? (
            <button
              className="mic-card__add-btn"
              onClick={handleAddClick}
              aria-label={`Add ${item.name} to cart`}
              title="Add to cart"
            >
              <PlusIcon />
            </button>
          ) : (
            <QuantityCounter
              quantity={cartQuantity}
              onIncrement={handleQuantityIncrement}
              onDecrement={handleQuantityDecrement}
              isLoading={isUpdating}
            />
          )}
        </div>

        {/* Content */}
        <div className="mic-card__content">
          <h3 className="mic-card__name">{item.name}</h3>
          
          {item.description && (
            <p className="mic-card__desc">{item.description}</p>
          )}

          <div className="mic-card__footer">
            {/* Price */}
            <span className="mic-card__price">{formatINR(item.price)}</span>

            {/* Rating */}
            {typeof item.rating === 'number' && item.rating > 0 && (
              <span className="mic-card__rating">
                <StarIcon />
                {item.rating}
                {item.totalRatings && item.totalRatings > 0 && (
                  <span className="mic-card__rating-count">({item.totalRatings})</span>
                )}
              </span>
            )}
          </div>
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

export default MenuItemCard;
