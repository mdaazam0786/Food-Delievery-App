/**
 * useCartOperations.ts — Custom hook for cart CRUD operations
 *
 * Provides simplified async wrappers for adding, updating, and removing cart items.
 * Handles error states and provides feedback through toast notifications.
 *
 * Usage:
 *   const { addToCart, updateQty, removeFromCart } = useCartOperations();
 *   await addToCart(menuItem, restaurantId);
 *   await updateQty(itemId, newQuantity);
 *   await removeFromCart(itemId);
 */

import { useCallback, useState } from 'react';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import type { MenuItemResponse } from '../services/restaurantService';

export function useCartOperations() {
  const { addItem, removeItem, updateQuantity } = useCart();
  const { showToast } = useToast();
  const [operationLoading, setOperationLoading] = useState(false);

  // ── Add item to cart ──
  const addToCart = useCallback(
    async (menuItem: MenuItemResponse, restaurantId: string) => {
      setOperationLoading(true);
      try {
        await addItem(menuItem, restaurantId);
        // Toast is shown in CartContext
        return true;
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to add item';
        console.error('Add to cart error:', err);
        return false;
      } finally {
        setOperationLoading(false);
      }
    },
    [addItem]
  );

  // ── Update quantity ──
  const updateQty = useCallback(
    async (itemId: string, quantity: number) => {
      setOperationLoading(true);
      try {
        await updateQuantity(itemId, quantity);
        // Toast is shown in CartContext
        return true;
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to update quantity';
        console.error('Update quantity error:', err);
        return false;
      } finally {
        setOperationLoading(false);
      }
    },
    [updateQuantity]
  );

  // ── Remove from cart ──
  const removeFromCart = useCallback(
    async (itemId: string) => {
      setOperationLoading(true);
      try {
        await removeItem(itemId);
        // Toast is shown in CartContext
        return true;
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to remove item';
        console.error('Remove from cart error:', err);
        return false;
      } finally {
        setOperationLoading(false);
      }
    },
    [removeItem]
  );

  return {
    addToCart,
    updateQty,
    removeFromCart,
    operationLoading,
  };
}
