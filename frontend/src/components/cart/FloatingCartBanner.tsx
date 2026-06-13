import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { formatINR } from '../../utils/currencyFormatter';
import './FloatingCartBanner.css';

/**
 * FloatingCartBanner — Sticky floating cart summary banner at the bottom of the screen.
 * 
 * Displays:
 * - Total item count and subtotal (left side)
 * - "View Cart" button (right side)
 * 
 * Only visible when cart has items.
 */
const FloatingCartBanner: React.FC = () => {
  const navigate = useNavigate();
  const { items, totalAmount } = useCart();

  // Don't render if cart is empty
  if (items.length === 0) {
    return null;
  }

  // Calculate total items count
  const totalItems = items.reduce((sum, item) => sum + item.quantity, 0);

  const handleViewCart = () => {
    navigate('/checkout');
  };

  return (
    <div className="fcb-banner">
      {/* Left side: Item count and subtotal */}
      <div className="fcb-banner__summary">
        <span className="fcb-banner__count">
          {totalItems} {totalItems === 1 ? 'item' : 'items'}
        </span>
        <span className="fcb-banner__divider">|</span>
        <span className="fcb-banner__total">
          {formatINR(totalAmount)}
        </span>
      </div>

      {/* Right side: View Cart button */}
      <button
        className="fcb-banner__button"
        onClick={handleViewCart}
        aria-label="View cart and checkout"
        title="View cart and checkout"
      >
        View Cart 🛒
      </button>
    </div>
  );
};

export default FloatingCartBanner;
