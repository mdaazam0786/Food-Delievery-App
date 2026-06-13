import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FormInput from '../components/auth/FormInput';
import SaveAddressDrawer from '../components/address/SaveAddressDrawer';
import { fetchCurrentUser, updateUserProfile, getUserAddresses, type UserProfile, type UserAddress } from '../services/userService';
import { fetchUserOrders, type RestaurantOrder, type Page } from '../services/orderService';
import { getRestaurantDetails, type RestaurantResponse } from '../services/restaurantService';
import { logout } from '../services/authService';
import './ProfileScreen.css';

type ActiveTab = 'orders' | 'addresses' | 'edit';

interface MenuItem {
  id: ActiveTab;
  label: string;
  icon: (className: string) => React.ReactNode;
}

interface OrderWithRestaurant extends RestaurantOrder {
  restaurant?: RestaurantResponse;
}

const RESTAURANT_FALLBACK = 'https://via.placeholder.com/120x80?text=Restaurant';

const ProfileScreen: React.FC = () => {
  const navigate = useNavigate();
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<ActiveTab>('orders');

  // Form state
  const [formData, setFormData] = useState({ fullName: '', email: '', phoneNumber: '' });
  const [isSaving, setIsSaving] = useState(false);
  const [formSuccess, setFormSuccess] = useState(false);

  // Orders state
  const [orders, setOrders] = useState<OrderWithRestaurant[]>([]);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersPage, setOrdersPage] = useState(0);
  const [ordersTotalPages, setOrdersTotalPages] = useState(0);
  const [ordersPageSize] = useState(10);

  // Addresses state
  const [addresses, setAddresses] = useState<UserAddress[]>([]);
  const [addressesLoading, setAddressesLoading] = useState(false);
  const [showAddressDrawer, setShowAddressDrawer] = useState(false);

  // Initial profile load
  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true);
        const profile = await fetchCurrentUser();
        setUser(profile);
        setFormData({
          fullName: profile.fullName || '',
          email: profile.email || '',
          phoneNumber: profile.phoneNumber || '',
        });
        setError(null);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to load profile';
        setError(message);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  // Load orders when tab changes or page changes
  useEffect(() => {
    if (activeTab === 'orders') {
      loadOrders(ordersPage);
    }
  }, [activeTab, ordersPage]);

  // Load addresses when tab changes
  useEffect(() => {
    if (activeTab === 'addresses') {
      loadAddresses();
    }
  }, [activeTab]);

  const loadOrders = async (page: number) => {
    try {
      setOrdersLoading(true);
      const historyData = await fetchUserOrders(page, ordersPageSize);
      
      const ordersWithRestaurants = await Promise.all(
        historyData.content.map(async (order) => {
          try {
            const restaurant = await getRestaurantDetails(order.restaurantId);
            return { ...order, restaurant };
          } catch {
            return order;
          }
        })
      );

      setOrders(ordersWithRestaurants);
      setOrdersTotalPages(historyData.totalPages);
    } catch (err) {
      console.error('Failed to load orders:', err);
      setOrders([]);
    } finally {
      setOrdersLoading(false);
    }
  };

  const loadAddresses = async () => {
    try {
      setAddressesLoading(true);
      const addressList = await getUserAddresses();
      setAddresses(addressList);
    } catch (err) {
      console.error('Failed to load addresses:', err);
    } finally {
      setAddressesLoading(false);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login', { replace: true });
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Logout failed';
      setError(message);
    }
  };

  const handleFormChange = (field: keyof typeof formData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setFormSuccess(false);
  };

  const handleSaveProfile = async () => {
    if (!formData.fullName.trim() || !formData.email.trim()) {
      setError('Full Name and Email are required');
      return;
    }

    try {
      setIsSaving(true);
      setError(null);
      const updated = await updateUserProfile({
        fullName: formData.fullName.trim(),
        email: formData.email.trim(),
        phoneNumber: formData.phoneNumber.trim() || undefined,
      });
      setUser(updated);
      setFormSuccess(true);
      setTimeout(() => setFormSuccess(false), 3000);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update profile';
      setError(message);
    } finally {
      setIsSaving(false);
    }
  };

  const getStatusBadgeClass = (status: string) => {
    const normalizedStatus = status.toUpperCase();
    if (normalizedStatus === 'COMPLETED' || normalizedStatus === 'DELIVERED') {
      return 'completed';
    }
    if (normalizedStatus === 'CANCELLED' || normalizedStatus === 'CANCELED') {
      return 'cancelled';
    }
    return 'default';
  };

  const getStatusLabel = (status: string) => {
    const normalizedStatus = status.toUpperCase();
    if (normalizedStatus === 'COMPLETED' || normalizedStatus === 'DELIVERED') {
      return 'Completed';
    }
    if (normalizedStatus === 'CANCELLED' || normalizedStatus === 'CANCELED') {
      return 'Cancelled';
    }
    return status;
  };

  const formatAmount = (amount: number) => {
    return `₹${amount.toFixed(2)}`;
  };

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-IN', {
        day: '2-digit',
        month: 'short',
        year: 'numeric',
      });
    } catch {
      return dateString;
    }
  };

  // Menu items
  const menuItems: MenuItem[] = [
    {
      id: 'orders',
      label: 'Orders',
      icon: (className: string) => (
        <svg className={className} width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="9" cy="21" r="1"/>
          <circle cx="20" cy="21" r="1"/>
          <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
        </svg>
      ),
    },
    {
      id: 'addresses',
      label: 'Addresses',
      icon: (className: string) => (
        <svg className={className} width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
          <circle cx="12" cy="10" r="3"/>
        </svg>
      ),
    },
    {
      id: 'edit',
      label: 'Personal Info',
      icon: (className: string) => (
        <svg className={className} width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
          <circle cx="12" cy="7" r="4"/>
        </svg>
      ),
    },
  ];

  if (loading) {
    return (
      <div className="profile-screen">
        <div className="profile-loading">
          <p>Loading profile…</p>
        </div>
      </div>
    );
  }

  if (error && !user) {
    return (
      <div className="profile-screen">
        <div className="profile-error">
          <p>{error}</p>
          <button 
            className="profile-error__retry"
            onClick={() => window.location.reload()}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="profile-screen">
        <div className="profile-error">
          <p>User data not available</p>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-screen">
      {/* ── Hero Banner ── */}
      <div className="profile-hero">
        <div className="profile-hero__content">
          <div className="profile-hero__left">
            <h1 className="profile-hero__name">{user.fullName || 'User'}</h1>
            <p className="profile-hero__phone">{user.phoneNumber || '(No phone number)'}</p>
          </div>
          <button 
            className="profile-hero__edit-btn" 
            onClick={() => setActiveTab('edit')}
          >
            EDIT PROFILE
          </button>
        </div>
      </div>

      {/* ── Main Container ── */}
      <div className="profile-container">
        {/* ── Left Sidebar ── */}
        <aside className="profile-sidebar">
          <nav className="profile-nav">
            {menuItems.map((item) => (
              <button
                key={item.id}
                className={`profile-nav__item ${activeTab === item.id ? 'profile-nav__item--active' : ''}`}
                onClick={() => setActiveTab(item.id)}
              >
                <div className="profile-nav__icon-wrapper">
                  {item.icon('profile-nav__icon')}
                </div>
                <span className="profile-nav__label">{item.label}</span>
              </button>
            ))}

            {/* Log Out Button */}
            <button
              className="profile-nav__item profile-nav__item--danger"
              onClick={handleLogout}
            >
              <div className="profile-nav__icon-wrapper profile-nav__icon-wrapper--danger">
                <svg 
                  className="profile-nav__icon" 
                  width="24" 
                  height="24" 
                  viewBox="0 0 24 24" 
                  fill="none" 
                  stroke="currentColor" 
                  strokeWidth="2"
                >
                  <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                  <polyline points="16 17 21 12 16 7"/>
                  <line x1="21" y1="12" x2="9" y2="12"/>
                </svg>
              </div>
              <span className="profile-nav__label">Log Out</span>
            </button>
          </nav>
        </aside>

        {/* ── Right Content Panel ── */}
        <main className="profile-content">
          {/* ── ORDERS TAB ── */}
          {activeTab === 'orders' && (
            <div className="profile-panel">
              {ordersLoading ? (
                <div className="profile-panel__loader">Loading orders…</div>
              ) : orders.length === 0 ? (
                <div className="profile-panel__empty">
                  <div className="profile-panel__empty-icon">🐱</div>
                  <h3 className="profile-panel__empty-title">No Orders</h3>
                  <p className="profile-panel__empty-text">You haven't placed any order yet.</p>
                </div>
              ) : (
                <div className="profile-panel__orders-list">
                  <h2 className="profile-panel__title">My Orders</h2>
                  {orders.map((order) => (
                    <div key={order.id} className="profile-order-card">
                      {/* Card Header */}
                      <div className="profile-order-card__header">
                        <div className="profile-order-card__thumb">
                          <img
                            src={order.restaurant?.imageUrl || RESTAURANT_FALLBACK}
                            alt={order.restaurant?.name || 'Restaurant'}
                            className="profile-order-card__image"
                            onError={(e) => {
                              (e.currentTarget as HTMLImageElement).src = RESTAURANT_FALLBACK;
                            }}
                          />
                        </div>
                        <div className="profile-order-card__info">
                          <h4 className="profile-order-card__name">{order.restaurant?.name || 'Restaurant'}</h4>
                          <p className="profile-order-card__meta">
                            <span>{formatAmount(order.totalAmount)}</span>
                            <span>•</span>
                            <span>{formatDate(order.createdAt)}</span>
                            <span>•</span>
                            <span>{order.items.length} Items</span>
                          </p>
                        </div>
                      </div>

                      {/* Status Badge */}
                      <div className={`profile-order-card__badge profile-order-card__badge--${getStatusBadgeClass(order.status)}`}>
                        {getStatusLabel(order.status)}
                      </div>

                      {/* Items List */}
                      <div className="profile-order-card__items">
                        {order.items.map((item, idx) => (
                          <div key={idx} className="profile-order-card__item">
                            <span className="profile-order-card__item-name">{item.itemName}</span>
                            <span className="profile-order-card__item-qty">x{item.quantity}</span>
                          </div>
                        ))}
                      </div>

                      {/* Action Buttons */}
                      <div className="profile-order-card__actions">
                        <button className="profile-order-card__btn profile-order-card__btn--outline">
                          Rate
                        </button>
                        <button className="profile-order-card__btn profile-order-card__btn--primary">
                          Re-Order
                        </button>
                      </div>
                    </div>
                  ))}

                  {/* Pagination */}
                  {ordersTotalPages > 1 && (
                    <div className="profile-pagination">
                      <button
                        className="profile-pagination__btn"
                        onClick={() => setOrdersPage((p) => Math.max(0, p - 1))}
                        disabled={ordersPage === 0}
                      >
                        ← Previous
                      </button>
                      <span className="profile-pagination__info">
                        Page {ordersPage + 1} of {ordersTotalPages}
                      </span>
                      <button
                        className="profile-pagination__btn"
                        onClick={() => setOrdersPage((p) => Math.min(ordersTotalPages - 1, p + 1))}
                        disabled={ordersPage === ordersTotalPages - 1}
                      >
                        Next →
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          {/* ── ADDRESSES TAB ── */}
          {activeTab === 'addresses' && (
            <div className="profile-panel">
              {addressesLoading ? (
                <div className="profile-panel__loader">Loading addresses…</div>
              ) : (
                <>
                  <h2 className="profile-panel__title">Saved Addresses</h2>
                  {addresses.length === 0 ? (
                    <div className="profile-panel__empty">
                      <div className="profile-panel__empty-icon">📍</div>
                      <h3 className="profile-panel__empty-title">No Addresses</h3>
                      <p className="profile-panel__empty-text">Add your first address to get started.</p>
                    </div>
                  ) : (
                    <div className="profile-addresses-list">
                      {addresses.map((address) => (
                        <div key={address.id} className="profile-address-card">
                          <div className="profile-address-card__icon">📍</div>
                          <div className="profile-address-card__content">
                            <h4 className="profile-address-card__label">{address.label}</h4>
                            <p className="profile-address-card__text">{address.formattedAddress}</p>
                          </div>
                          <div className="profile-address-card__actions">
                            <button className="profile-address-card__btn">Edit</button>
                            <button className="profile-address-card__btn profile-address-card__btn--danger">Delete</button>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* Add New Address Button */}
                  <button 
                    className="profile-panel__add-address-btn"
                    onClick={() => setShowAddressDrawer(true)}
                  >
                    + ADD NEW ADDRESS
                  </button>

                  {/* Address Drawer */}
                  <SaveAddressDrawer 
                    isOpen={showAddressDrawer}
                    onClose={() => setShowAddressDrawer(false)}
                    onAddressSaved={() => {
                      setShowAddressDrawer(false);
                      loadAddresses();
                    }}
                  />
                </>
              )}
            </div>
          )}

          {/* ── EDIT PROFILE TAB ── */}
          {activeTab === 'edit' && (
            <div className="profile-panel">
              <h2 className="profile-panel__title">Edit Profile</h2>
              
              {error && (
                <div className="profile-panel__message profile-panel__message--error">
                  {error}
                </div>
              )}

              {formSuccess && (
                <div className="profile-panel__message profile-panel__message--success">
                  Profile updated successfully!
                </div>
              )}

              <div className="profile-form">
                <FormInput
                  label="Full Name"
                  placeholder="Enter your full name"
                  value={formData.fullName}
                  onChange={(value) => handleFormChange('fullName', value)}
                  type="text"
                  autoComplete="name"
                  className="profile-form__field"
                />

                <FormInput
                  label="Email"
                  placeholder="Enter your email address"
                  value={formData.email}
                  onChange={(value) => handleFormChange('email', value)}
                  type="email"
                  autoComplete="email"
                  className="profile-form__field"
                />

                <FormInput
                  label="Phone Number"
                  placeholder="Enter your phone number"
                  value={formData.phoneNumber}
                  onChange={(value) => handleFormChange('phoneNumber', value)}
                  type="tel"
                  autoComplete="tel"
                  className="profile-form__field"
                />

                <button
                  className="profile-form__submit"
                  onClick={handleSaveProfile}
                  disabled={isSaving}
                  aria-busy={isSaving}
                >
                  {isSaving ? 'Saving…' : 'SAVE'}
                </button>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default ProfileScreen;
