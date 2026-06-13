import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { useLocation } from '../context/LocationContext';
import { getUserAddresses, type UserAddress } from '../services/userService';
import { getRestaurantDetails, reverseGeocodeBackend, type RestaurantResponse } from '../services/restaurantService';
import { createOrder, type CreateOrderRequest, type CreateOrderResponse } from '../services/orderService';
import { initiatePayment, type InitiatePaymentResponse } from '../services/paymentService';
import SaveAddressDrawer from '../components/address/SaveAddressDrawer';
import './CheckoutScreen.css';

// ─── Helper: Load Razorpay Script ─────────────────────────────────────────────

/**
 * Dynamically loads the Razorpay Checkout script from CDN.
 * Returns a Promise that resolves when the script is ready or rejects on error.
 */
const loadRazorpayScript = (): Promise<void> => {
  return new Promise((resolve, reject) => {
    // Check if script already exists in document
    if ((window as any).Razorpay) {
      resolve();
      return;
    }

    const script = document.createElement('script');
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('Failed to load Razorpay script'));

    document.head.appendChild(script);
  });
};

// ─── Icons ────────────────────────────────────────────────────────────────────

const CheckIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="20 6 9 17 4 12" stroke="#22C55E" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const VegBadgeIcon = () => (
  <svg width="8" height="8" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="4" y="4" width="16" height="16" rx="2" stroke="#22C55E" strokeWidth="2" fill="#22C55E"/>
  </svg>
);

const NonVegBadgeIcon = () => (
  <svg width="8" height="8" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polygon points="12,4 20,16 4,16" stroke="#DC2626" strokeWidth="2" fill="#DC2626"/>
  </svg>
);

const MapPinIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5A2.5 2.5 0 1112 6a2.5 2.5 0 010 5.5z" fill="#9EA1B1"/>
  </svg>
);

// ─── Component ────────────────────────────────────────────────────────────────

const CheckoutScreen: React.FC = () => {
  const navigate = useNavigate();
  const { items, getTotalPrice, updateQuantity, removeItem, restaurantId, clearCart } = useCart();
  const { showToast } = useToast();
  const { coords } = useLocation();

  const [addresses, setAddresses] = useState<UserAddress[]>([]);
  const [selectedAddress, setSelectedAddress] = useState<string | null>(null);
  const [showAddressOptions, setShowAddressOptions] = useState(true);
  const [showAddressDrawer, setShowAddressDrawer] = useState(false);
  const [addressLoading, setAddressLoading] = useState(true);
  const [addressError, setAddressError] = useState<string | null>(null);
  const [notes, setNotes] = useState('');
  const [processing, setProcessing] = useState(false);
  const [processingMessage, setProcessingMessage] = useState('');
  const [restaurant, setRestaurant] = useState<RestaurantResponse | null>(null);
  const [currentLocationAddress, setCurrentLocationAddress] = useState<string>('');
  const [isGlobalLocked, setIsGlobalLocked] = useState(false);

  // Load addresses on mount
  useEffect(() => {
    const loadAddresses = async () => {
      try {
        setAddressLoading(true);
        setAddressError(null);
        const data = await getUserAddresses();
        setAddresses(data);
        // Auto-select default address if available
        const defaultAddr = data.find(a => a.isDefault);
        if (defaultAddr) {
          setSelectedAddress(defaultAddr.id);
          setShowAddressOptions(false);
        }
      } catch (err) {
        const errorMsg = err instanceof Error ? err.message : 'Failed to load addresses';
        // Don't show error if it's 404 - just show empty state
        if (!errorMsg.includes('404')) {
          setAddressError(errorMsg);
        }
        console.error('Load addresses error:', err);
      } finally {
        setAddressLoading(false);
      }
    };

    loadAddresses();
  }, []);

  // Geocode current location
  useEffect(() => {
    const geocodeCurrentLocation = async () => {
      if (!coords) return;
      try {
        const response = await reverseGeocodeBackend(coords.lat, coords.lng);
        setCurrentLocationAddress(response.displayName || 'Current Location');
      } catch (err) {
        console.error('Geocode error:', err);
        setCurrentLocationAddress('Current Location');
      }
    };

    geocodeCurrentLocation();
  }, [coords]);

  // Load restaurant details
  useEffect(() => {
    const loadRestaurant = async () => {
      if (!restaurantId) {
        return;
      }

      try {
        const data = await getRestaurantDetails(restaurantId);
        setRestaurant(data);
      } catch (err) {
        console.error('Failed to load restaurant:', err);
      }
    };

    loadRestaurant();
  }, [restaurantId]);

  const handleSelectAddress = (addressId: string) => {
    setSelectedAddress(addressId);
    setShowAddressOptions(false);
  };

  const handleChangeAddress = () => {
    setShowAddressOptions(true);
  };

  const handleAddNewAddress = () => {
    setShowAddressDrawer(true);
  };

  const handleAddressSelected = async (newAddress: { label: string; formattedAddress: string; latitude: number; longitude: number }) => {
    // TODO: Call backend to persist address, then reload list
    // For now, just add to local list
    const tempId = `temp-${Date.now()}`;
    const addressToAdd: UserAddress = {
      id: tempId,
      label: newAddress.label,
      formattedAddress: newAddress.formattedAddress,
      latitude: newAddress.latitude,
      longitude: newAddress.longitude,
      isDefault: false,
    };
    
    setAddresses((prev) => [...prev, addressToAdd]);
    setSelectedAddress(tempId);
    setShowAddressOptions(false);
    showToast('Address added successfully', 'success');
  };

  const handleAddressSaved = async () => {
    // Refresh the address list from backend
    try {
      const updatedAddresses = await getUserAddresses();
      setAddresses(updatedAddresses);
      
      // Auto-select the newly added address (should be the last one)
      if (updatedAddresses.length > 0) {
        const newAddress = updatedAddresses[updatedAddresses.length - 1];
        setSelectedAddress(newAddress.id);
        setShowAddressOptions(false);
      }
    } catch (err) {
      console.error('Failed to refresh addresses:', err);
    }
  };

  const handleProceedToPay = async () => {
    if (!selectedAddress) {
      showToast('Please select a delivery address', 'error');
      return;
    }

    if (!restaurantId) {
      showToast('Restaurant information not available', 'error');
      return;
    }

    // Lock the entire window to prevent duplicate clicks during pipeline execution
    setIsGlobalLocked(true);
    setProcessing(true);
    setProcessingMessage('Creating order...');

    try {
      // ────────────────────────────────────────────────────────────────
      // STEP 1: Create Order
      // ────────────────────────────────────────────────────────────────
      const orderPayload: CreateOrderRequest = {
        restaurantId,
        deliveryAddressId: selectedAddress,
        items: items.map(item => ({
          menuItemId: item.itemId,
          itemName: item.itemName,
          quantity: item.quantity,
          price: item.unitPrice,
        })),
        notes: notes.trim() || undefined,
      };

      const orderResponse: CreateOrderResponse = await createOrder(orderPayload);
      const orderId = orderResponse.id || orderResponse.orderId;

      if (!orderId) {
        throw new Error('Order created but no order ID returned');
      }

      // ────────────────────────────────────────────────────────────────
      // STEP 1.5: Allow backend Kafka event propagation
      // ────────────────────────────────────────────────────────────────
      // Give distributed backend services time to complete async database
      // synchronization (Kafka event processing, payment record creation, etc.)
      setProcessingMessage('Preparing payment gateway...');
      await new Promise(resolve => setTimeout(() => resolve(undefined), 1000));

      // ────────────────────────────────────────────────────────────────
      // STEP 2: Initiate Payment Session
      // ────────────────────────────────────────────────────────────────
      setProcessingMessage('Initializing payment...');

      const paymentResponse: InitiatePaymentResponse = await initiatePayment(orderId);

      if (!paymentResponse.razorpayOrderId) {
        throw new Error('Payment initialization failed - no Razorpay Order ID');
      }

      // ────────────────────────────────────────────────────────────────
      // STEP 3: Load Razorpay Checkout Script
      // ────────────────────────────────────────────────────────────────
      setProcessingMessage('Loading payment gateway...');
      await loadRazorpayScript();

      // ────────────────────────────────────────────────────────────────
      // STEP 4: Initialize and Open Razorpay Checkout Modal
      // ────────────────────────────────────────────────────────────────
      const razorpayKey = import.meta.env.VITE_RAZORPAY_KEY_ID;
      console.log('🔑 Initializing Razorpay with Key:', razorpayKey);

      // Validate Razorpay key before attempting checkout
      if (!razorpayKey || razorpayKey.trim() === '') {
        console.error('❌ Razorpay key is missing or empty. Check VITE_RAZORPAY_KEY_ID env var.');
        setProcessing(false);
        setProcessingMessage('');
        setIsGlobalLocked(false);
        showToast('Checkout configuration error: Missing frontend gateway API Key.', 'error');
        return;
      }

      const razorpayOptions = {
        key: razorpayKey,
        amount: Math.round(paymentResponse.amount * 100), // Convert to paise
        currency: paymentResponse.currency,
        order_id: paymentResponse.razorpayOrderId,
        name: 'Foodzie App',
        description: 'Order Payment Checkout',
        handler: async () => {
          try {
            setProcessingMessage('Payment successful! Setting up your order...');
            
            // Payment was captured by Razorpay SDK
            // The webhook handler on backend will verify the signature and update payment status
            // We just need to clear cart and navigate to home
            
            // Wait a moment for webhook to process
            await new Promise(resolve => setTimeout(() => resolve(undefined), 1500));

            // Clear the cart using pre-initialized hook reference
            await clearCart();
            
            // Show success notification
            showToast('🎉 Payment Successful! Your order has been placed.', 'success');
            
            // Redirect to home using pre-initialized hook reference
            navigate('/home');
          } catch (err) {
            const errorMsg = err instanceof Error ? err.message : 'Failed to complete order';
            showToast(errorMsg, 'error');
            console.error('Order completion error:', err);
            setProcessing(false);
            setIsGlobalLocked(false);
          }
        },
      };

      const rzp = new (window as any).Razorpay(razorpayOptions);
      
      // Handle modal close without payment
      rzp.on('dismiss', () => {
        console.warn('User dismissed Razorpay checkout modal');
        setProcessing(false);
        setProcessingMessage('');
        setIsGlobalLocked(false);
      });

      // Open the checkout modal
      setProcessingMessage('Opening payment gateway...');
      rzp.open();

    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to place order';
      showToast(errorMsg, 'error');
      console.error('Order/Payment pipeline error:', err);
    } finally {
      setProcessing(false);
      setProcessingMessage('');
      setIsGlobalLocked(false);
    }
  };

  const totalPrice = getTotalPrice();
  const selectedAddressData = addresses.find(a => a.id === selectedAddress);

  if (items.length === 0) {
    return (
      <div className="checkout-empty">
        <p>Your cart is empty</p>
        <button onClick={() => navigate(-1)} className="checkout-empty__btn">Go Back</button>
      </div>
    );
  }

  return (
    <div className="checkout">
      <div className="checkout__container">
        {/* Left Column (2 cols) */}
        <div className="checkout__left">
          {/* Address Section */}
          <div className="checkout__section">
            <h2 className="checkout__section-title">Delivery Address</h2>

            {addressLoading ? (
              <div className="checkout__address-loading">
                <p>Loading addresses...</p>
              </div>
            ) : addresses.length === 0 ? (
              <div className="checkout__no-addresses">
                <div className="checkout__no-addresses-icon">
                  <MapPinIcon />
                </div>
                <p className="checkout__no-addresses-heading">Add New Address</p>
                <p className="checkout__no-addresses-location">{currentLocationAddress || 'Current Location'}</p>
                <button
                  className="checkout__add-address-btn"
                  onClick={handleAddNewAddress}
                >
                  ADD NEW
                </button>
              </div>
            ) : addressError ? (
              <div className="checkout__address-error">
                <p>{addressError}</p>
              </div>
            ) : !showAddressOptions && selectedAddressData ? (
              <div className="checkout__selected-address">
                <div className="checkout__selected-address-content">
                  <CheckIcon />
                  <div className="checkout__selected-address-text">
                    <p className="checkout__selected-address-name">{selectedAddressData.label}</p>
                    <p className="checkout__selected-address-detail">{selectedAddressData.formattedAddress}</p>
                  </div>
                </div>
                <button
                  className="checkout__change-btn"
                  onClick={handleChangeAddress}
                >
                  CHANGE
                </button>
              </div>
            ) : (
              <div className="checkout__address-options">
                {addresses.map((addr) => (
                  <button
                    key={addr.id}
                    className={`checkout__address-option ${selectedAddress === addr.id ? 'checkout__address-option--selected' : ''}`}
                    onClick={() => handleSelectAddress(addr.id)}
                  >
                    <div className="checkout__address-option-header">
                      <p className="checkout__address-option-label">{addr.label}</p>
                      <span className="checkout__address-delivery-time">40 MINS</span>
                    </div>
                    <p className="checkout__address-option-text">{addr.formattedAddress}</p>
                    <div className="checkout__address-option-action">
                      <span className="checkout__deliver-here-btn">DELIVER HERE</span>
                    </div>
                  </button>
                ))}
                
                {/* Add New Address Button */}
                <button
                  className="checkout__address-option checkout__address-option--add-new"
                  onClick={handleAddNewAddress}
                >
                  <div className="checkout__address-option-icon">
                    <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <circle cx="12" cy="12" r="10" stroke="#22C55E" strokeWidth="1.5"/>
                      <line x1="12" y1="8" x2="12" y2="16" stroke="#22C55E" strokeWidth="2" strokeLinecap="round"/>
                      <line x1="8" y1="12" x2="16" y2="12" stroke="#22C55E" strokeWidth="2" strokeLinecap="round"/>
                    </svg>
                  </div>
                  <p className="checkout__address-option-label">Add New Address</p>
                  <p className="checkout__address-option-text">{currentLocationAddress || 'Current Location'}</p>
                </button>
              </div>
            )}
          </div>

          {/* Proceed to Pay Button */}
          <button
            className={`checkout__proceed-btn ${!selectedAddress ? 'checkout__proceed-btn--disabled' : ''}`}
            onClick={handleProceedToPay}
            disabled={!selectedAddress || processing}
            aria-busy={processing}
          >
            {processing ? processingMessage || 'Processing...' : `PROCEED TO PAY ₹${totalPrice.toFixed(2)}`}
          </button>
        </div>

        {/* Right Sidebar (1 col, sticky) */}
        <div className="checkout__right">
          {/* Restaurant Info */}
          <div className="checkout__restaurant">
            <div className="checkout__restaurant-header">
              <p className="checkout__restaurant-name">{restaurant?.name || 'Restaurant Name'}</p>
              <p className="checkout__restaurant-location">{restaurant?.addressText || 'Outlet Location'}</p>
            </div>
          </div>

          {/* Cart Items */}
          <div className="checkout__items">
            <h3 className="checkout__items-title">Order Summary</h3>
            <div className="checkout__items-list">
              {items.map((item) => (
                <div key={item.itemId} className="checkout__item">
                  <div className="checkout__item-info">
                    <div className="checkout__item-badge">
                      {item.restaurantId ? <VegBadgeIcon /> : <NonVegBadgeIcon />}
                    </div>
                    <div className="checkout__item-details">
                      <p className="checkout__item-name">{item.itemName}</p>
                      <p className="checkout__item-price">₹{item.unitPrice}</p>
                    </div>
                  </div>
                  <div className="checkout__item-controls">
                    <button
                      className="checkout__item-qty-btn"
                      onClick={() => {
                        const newQty = item.quantity - 1;
                        if (newQty > 0) {
                          updateQuantity(item.itemId, newQty);
                        } else {
                          removeItem(item.itemId);
                        }
                      }}
                      aria-label="Decrease quantity"
                    >
                      −
                    </button>
                    <span className="checkout__item-qty-display">{item.quantity}</span>
                    <button
                      className="checkout__item-qty-btn"
                      onClick={() => updateQuantity(item.itemId, item.quantity + 1)}
                      aria-label="Increase quantity"
                    >
                      +
                    </button>
                    <span className="checkout__item-subtotal">₹{item.subtotal}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Notes */}
          <div className="checkout__notes">
            <textarea
              className="checkout__notes-input"
              placeholder="Add special instructions or notes for your order..."
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              rows={3}
            />
          </div>

          {/* Bill Summary */}
          <div className="checkout__summary">
            <div className="checkout__summary-row">
              <span>Subtotal</span>
              <span>₹{totalPrice.toFixed(2)}</span>
            </div>
            <div className="checkout__summary-row">
              <span>Delivery Fee</span>
              <span>₹0</span>
            </div>
            <div className="checkout__summary-row">
              <span>Tax</span>
              <span>₹0</span>
            </div>
            <div className="checkout__summary-row checkout__summary-row--total">
              <span>TO PAY</span>
              <span>₹{totalPrice.toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* SaveAddressDrawer */}
        <SaveAddressDrawer
          isOpen={showAddressDrawer}
          onClose={() => setShowAddressDrawer(false)}
          onAddressSelected={handleAddressSelected}
          onAddressSaved={handleAddressSaved}
        />

        {/* Global Loading Overlay - Prevents interaction during sequential API calls */}
        {isGlobalLocked && (
          <div className="checkout__global-overlay" aria-busy="true" role="status">
            <div className="checkout__global-overlay-content">
              <div className="checkout__spinner"></div>
              <p className="checkout__overlay-message">{processingMessage || 'Processing your order...'}</p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CheckoutScreen;
