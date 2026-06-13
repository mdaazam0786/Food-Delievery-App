/**
 * CartContext.tsx — Shopping cart state management with multi-restaurant validation.
 * Syncs with backend repository APIs for persistent cart management.
 *
 * Features:
 * - Initial cart state sync on mount (GET /api/cart)
 * - Async quantity updates with backend calls
 * - Toast notifications on state changes
 * - Multi-restaurant validation
 */

import React, {
  createContext,
  useContext,
  useState,
  useCallback,
  useEffect,
  type ReactNode,
} from 'react';
import type { MenuItemResponse } from '../services/restaurantService';
import { useToast } from './ToastContext';

export interface CartItem {
  itemId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  restaurantId: string;
}

export interface CartContextValue {
  items: CartItem[];
  restaurantId: string | null;
  totalAmount: number;
  totalItems: number;
  loading: boolean;
  error: string | null;
  addItem: (menuItem: MenuItemResponse, restaurantId: string) => Promise<void>;
  removeItem: (itemId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  updateQuantity: (itemId: string, quantity: number) => Promise<void>;
  getTotalItems: () => number;
  getTotalPrice: () => number;
  initializeCart: () => Promise<void>;
}

const CartContext = createContext<CartContextValue>({
  items: [],
  restaurantId: null,
  totalAmount: 0,
  totalItems: 0,
  loading: false,
  error: null,
  addItem: async () => {},
  removeItem: async () => {},
  clearCart: async () => {},
  updateQuantity: async () => {},
  getTotalItems: () => 0,
  getTotalPrice: () => 0,
  initializeCart: async () => {},
});

export const CartProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [items, setItems] = useState<CartItem[]>([]);
  const [restaurantId, setRestaurantId] = useState<string | null>(null);
  const [totalAmount, setTotalAmount] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { showToast } = useToast();

  // ── Initialize cart on component mount ──
  const initializeCart = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await fetchCart();
      setItems(response.items || []);
      setRestaurantId(response.restaurantId || null);
      setTotalAmount(response.totalAmount || 0);
      setTotalItems(response.totalItems || 0);
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to load cart';
      setError(errorMsg);
      console.error('Cart initialization failed:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    initializeCart();
  }, [initializeCart]);

  // ── Add item to cart ──
  const addItem = useCallback(
    async (menuItem: MenuItemResponse, newRestaurantId: string) => {
      try {
        setError(null);
        const response = await addItemToCart({
          restaurantId: newRestaurantId,
          itemId: menuItem.id,
          itemName: menuItem.name,
          quantity: 1,
          unitPrice: menuItem.price,
        });
        
        setItems(response.items || []);
        setRestaurantId(response.restaurantId || null);
        setTotalAmount(response.totalAmount || 0);
        setTotalItems(response.totalItems || 0);
        
        showToast(`${menuItem.name} added to cart! 🛒`, 'success');
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to add item';
        setError(errorMsg);
        showToast(errorMsg, 'error');
        throw err;
      }
    },
    [showToast]
  );

  // ── Remove item from cart ──
  const removeItem = useCallback(
    async (itemId: string) => {
      try {
        setError(null);
        const response = await deleteItemFromCart(itemId);
        
        setItems(response.items || []);
        setRestaurantId(response.restaurantId || null);
        setTotalAmount(response.totalAmount || 0);
        setTotalItems(response.totalItems || 0);
        
        showToast('Item removed from cart', 'success');
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to remove item';
        setError(errorMsg);
        showToast(errorMsg, 'error');
        throw err;
      }
    },
    [showToast]
  );

  // ── Update quantity ──
  const updateQuantity = useCallback(
    async (itemId: string, quantity: number) => {
      try {
        setError(null);
        
        if (quantity <= 0) {
          await removeItem(itemId);
          return;
        }

        const response = await updateItemQuantity(itemId, quantity);
        
        setItems(response.items || []);
        setRestaurantId(response.restaurantId || null);
        setTotalAmount(response.totalAmount || 0);
        setTotalItems(response.totalItems || 0);
        
        showToast('Quantity updated', 'success');
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to update quantity';
        setError(errorMsg);
        showToast(errorMsg, 'error');
        throw err;
      }
    },
    [removeItem, showToast]
  );

  // ── Clear cart ──
  const clearCart = useCallback(async () => {
    try {
      setError(null);
      await clearCartBackend();
      
      setItems([]);
      setRestaurantId(null);
      setTotalAmount(0);
      setTotalItems(0);
      
      showToast('Cart cleared', 'success');
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to clear cart';
      setError(errorMsg);
      showToast(errorMsg, 'error');
      throw err;
    }
  }, [showToast]);

  // ── Calculate totals ──
  const getTotalItems = useCallback(() => {
    return items.reduce((sum, item) => sum + item.quantity, 0);
  }, [items]);

  const getTotalPrice = useCallback(() => {
    return items.reduce((sum, item) => sum + item.subtotal, 0);
  }, [items]);

  return (
    <CartContext.Provider
      value={{
        items,
        restaurantId,
        totalAmount,
        totalItems,
        loading,
        error,
        addItem,
        removeItem,
        clearCart,
        updateQuantity,
        getTotalItems,
        getTotalPrice,
        initializeCart,
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export function useCart(): CartContextValue {
  return useContext(CartContext);
}

// ── Backend API calls ──

interface CartResponse {
  id: string;
  userEmail: string;
  restaurantId: string | null;
  items: CartItem[];
  totalAmount: number;
  totalItems: number;
  createdAt: string;
  updatedAt: string;
}

interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp: string;
}

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

async function getAuthHeaders(): Promise<HeadersInit> {
  const { getAccessToken } = await import('../services/authService');
  const token = getAccessToken();
  if (!token) throw new Error('No access token — user is not authenticated.');
  
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  };
}

async function fetchCart(): Promise<CartResponse> {
  const headers = await getAuthHeaders();
  const res = await fetch(`${BASE_URL}/api/cart`, { headers });

  if (!res.ok) {
    throw new Error(`Failed to fetch cart with status ${res.status}`);
  }

  const json: ApiResponse<CartResponse> = await res.json();
  if (!json.success) {
    throw new Error(json.message || 'Failed to fetch cart');
  }

  return json.data;
}

async function addItemToCart(payload: {
  restaurantId: string;
  itemId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
}): Promise<CartResponse> {
  const headers = await getAuthHeaders();
  const res = await fetch(`${BASE_URL}/api/cart/items`, {
    method: 'POST',
    headers,
    body: JSON.stringify(payload),
  });

  if (!res.ok) {
    throw new Error(`Failed to add item with status ${res.status}`);
  }

  const json: ApiResponse<CartResponse> = await res.json();
  if (!json.success) {
    throw new Error(json.message || 'Failed to add item');
  }

  return json.data;
}

async function updateItemQuantity(
  itemId: string,
  quantity: number
): Promise<CartResponse> {
  const headers = await getAuthHeaders();
  const res = await fetch(`${BASE_URL}/api/cart/items/${itemId}`, {
    method: 'PATCH',
    headers,
    body: JSON.stringify({ quantity }),
  });

  if (!res.ok) {
    throw new Error(`Failed to update quantity with status ${res.status}`);
  }

  const json: ApiResponse<CartResponse> = await res.json();
  if (!json.success) {
    throw new Error(json.message || 'Failed to update quantity');
  }

  return json.data;
}

async function deleteItemFromCart(itemId: string): Promise<CartResponse> {
  const headers = await getAuthHeaders();
  const res = await fetch(`${BASE_URL}/api/cart/items/${itemId}`, {
    method: 'DELETE',
    headers,
  });

  if (!res.ok) {
    throw new Error(`Failed to remove item with status ${res.status}`);
  }

  const json: ApiResponse<CartResponse> = await res.json();
  if (!json.success) {
    throw new Error(json.message || 'Failed to remove item');
  }

  return json.data;
}

async function clearCartBackend(): Promise<void> {
  const headers = await getAuthHeaders();
  const res = await fetch(`${BASE_URL}/api/cart`, {
    method: 'DELETE',
    headers,
  });

  if (!res.ok) {
    throw new Error(`Failed to clear cart with status ${res.status}`);
  }

  const json: ApiResponse<null> = await res.json();
  if (!json.success) {
    throw new Error(json.message || 'Failed to clear cart');
  }
}
