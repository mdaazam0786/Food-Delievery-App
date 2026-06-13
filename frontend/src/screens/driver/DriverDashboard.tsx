import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDriverAuth } from '../../context/DriverAuthContext';
import { useToast } from '../../context/ToastContext';
import { useOffers, OfferWithTimer } from '../../context/OfferContext';
import { getAccessToken, getStoredUser } from '../../services/authService';
import { deliveryOfferService } from '../../services/deliveryOfferService';
import DriverRegistrationForm from '../../components/driver/DriverRegistrationForm';
import './DriverDashboard.css';

// ─── Types ────────────────────────────────────────────────────────────────────

interface ActiveDelivery {
  orderId: string;
  restaurantName: string;
  pickupArea: string;
  deliveryArea: string;
  customerName: string;
}

interface DeliveryRecord {
  id: number;
  orderId: string;
  restaurantName: string;
  pickupAddress: string;
  deliveryAddress: string;
  payoutAmount: number;
  deliveryStatus: string;
  customerRating: number | null;
  customerFeedback: string | null;
  completedAt: string;
}

interface EarningsSummary {
  driverId: string;
  totalEarnings: number;
  totalDeliveries: number;
  activeDeliveries: number;
  todaysEarnings: number;
}

interface PaginatedResponse {
  content: DeliveryRecord[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
}

type DriverStatus = 'AVAILABLE' | 'OFFLINE';

// ─── Icons ────────────────────────────────────────────────────────────────────

const FoodzieIcon = () => (
  <svg width="32" height="32" viewBox="0 0 100 100" fill="none" aria-hidden="true">
    <rect width="100" height="100" rx="12" fill="#FF7622"/>
    <text x="50" y="65" fontSize="48" fontWeight="bold" fill="white" textAnchor="middle">F</text>
  </svg>
);

const DashboardIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" aria-hidden="true">
    <rect x="3" y="3" width="7" height="7" strokeWidth="1.5"/>
    <rect x="14" y="3" width="7" height="7" strokeWidth="1.5"/>
    <rect x="3" y="14" width="7" height="7" strokeWidth="1.5"/>
    <rect x="14" y="14" width="7" height="7" strokeWidth="1.5"/>
  </svg>
);

const DeliveriesIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" aria-hidden="true">
    <path d="M5 9h14v10c0 1.1-.9 2-2 2H7c-1.1 0-2-.9-2-2V9z" strokeWidth="1.5"/>
    <path d="M12 5v4M9 5c0-.6.4-1 1-1h4c.6 0 1 .4 1 1" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

const ProfileIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" aria-hidden="true">
    <circle cx="12" cy="8" r="4" strokeWidth="1.5"/>
    <path d="M4 20c0-3.3 3.6-6 8-6s8 2.7 8 6" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

const EarningsIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill={active ? 'currentColor' : 'none'} stroke="currentColor" aria-hidden="true">
    <circle cx="12" cy="12" r="9" strokeWidth="1.5"/>
    <path d="M8 12h8M12 8v8" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

const LogoutIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
    <path d="M10 7H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h4M17 17l4-4m0 0l-4-4m4 4H7" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const BikeIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
    <circle cx="7" cy="17" r="3" strokeWidth="1.5"/>
    <circle cx="17" cy="17" r="3" strokeWidth="1.5"/>
    <path d="M9 5h4l4 7m-8 0h6" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

const CurrencyIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <circle cx="12" cy="12" r="9" opacity="0.1"/>
    <path d="M12 8v8M9 10h6v4H9z" fill="currentColor"/>
  </svg>
);

const CheckIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
    <polyline points="20 6 9 17 4 12" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const DeclineIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
    <line x1="18" y1="6" x2="6" y2="18" strokeWidth="2.2" strokeLinecap="round"/>
    <line x1="6" y1="6" x2="18" y2="18" strokeWidth="2.2" strokeLinecap="round"/>
  </svg>
);

const LocationIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 1118 0z" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
    <circle cx="12" cy="10" r="2.5" fill="currentColor"/>
  </svg>
);

const MoneyIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" aria-hidden="true">
    <circle cx="12" cy="12" r="9" strokeWidth="1.5"/>
    <path d="M8 12h8M12 8v8" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

const OnlineIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
    <circle cx="12" cy="12" r="10"/>
  </svg>
);

// ─── Sidebar Component ─────────────────────────────────────────────────────────

interface SidebarProps {
  activeTab: string;
  onTabChange: (tab: string) => void;
  onLogout: () => void;
}

const DriverSidebar: React.FC<SidebarProps> = ({ activeTab, onTabChange, onLogout }) => {
  const navItems = [
    { id: 'dashboard', label: 'Dashboard', icon: DashboardIcon },
    { id: 'deliveries', label: 'My Deliveries', icon: DeliveriesIcon },
    { id: 'profile', label: 'Profile Status', icon: ProfileIcon },
    { id: 'earnings', label: 'Earnings History', icon: EarningsIcon },
  ];

  return (
    <aside className="drv-sidebar">
      <div className="drv-sidebar__logo">
        <FoodzieIcon />
        <span className="drv-sidebar__logo-text">Foodzie</span>
      </div>

      <nav className="drv-sidebar__nav">
        {navItems.map((item) => {
          const IconComponent = item.icon;
          const isActive = activeTab === item.id;
          return (
            <button
              key={item.id}
              className={`drv-sidebar__nav-item ${isActive ? 'drv-sidebar__nav-item--active' : ''}`}
              onClick={() => onTabChange(item.id)}
              aria-current={isActive ? 'page' : undefined}
            >
              <IconComponent active={isActive} />
              <span>{item.label}</span>
            </button>
          );
        })}
      </nav>

      <button className="drv-sidebar__logout" onClick={onLogout} title="Sign out">
        <LogoutIcon />
        <span>Sign Out</span>
      </button>
    </aside>
  );
};

// ─── Stat Card Component ───────────────────────────────────────────────────────

interface StatCardProps {
  icon: React.ReactNode;
  value: string | number;
  label: string;
  hint: string;
}

const StatCard: React.FC<StatCardProps> = ({ icon, value, label, hint }) => (
  <div className="drv-stat-card">
    <div className="drv-stat-card__icon">{icon}</div>
    <div className="drv-stat-card__value">{value}</div>
    <div className="drv-stat-card__label">{label}</div>
    <div className="drv-stat-card__hint">{hint}</div>
  </div>
);

// ─── Offer Card Component ──────────────────────────────────────────────────────

interface OfferCardProps {
  offer: OfferWithTimer;
  onAccept: (orderId: string) => Promise<void>;
  onDecline: (orderId: string) => Promise<void>;
  isProcessing: boolean;
}

const OfferCard: React.FC<OfferCardProps> = ({ offer, onAccept, onDecline, isProcessing }) => {
  const isExpired = offer.timeLeft === 0;

  return (
    <div className="drv-offer-card">
      <div className="drv-offer-card__header">
        <h4 className="drv-offer-card__restaurant">{offer.restaurantName}</h4>
        <span className={`drv-offer-card__timer ${isExpired ? 'drv-offer-card__timer--expired' : ''}`}>
          ⏱️ {offer.timeLeft}s
        </span>
      </div>

      <div className="drv-offer-card__routes">
        <div className="drv-offer-card__route">
          <LocationIcon />
          <div>
            <div className="drv-offer-card__route-label">Pickup</div>
            <div className="drv-offer-card__route-value">{offer.pickupLocation}</div>
          </div>
        </div>
        <div className="drv-offer-card__arrow">→</div>
        <div className="drv-offer-card__route">
          <LocationIcon />
          <div>
            <div className="drv-offer-card__route-label">Delivery</div>
            <div className="drv-offer-card__route-value">{offer.deliveryLocation}</div>
          </div>
        </div>
      </div>

      <div className="drv-offer-card__footer">
        <div className="drv-offer-card__payout">
          <MoneyIcon />
          <span className="drv-offer-card__amount">₹{offer.estimatedPayout.toFixed(2)}</span>
        </div>
        <div className="drv-offer-card__actions">
          <button
            className="drv-offer-card__btn drv-offer-card__btn--decline"
            onClick={() => onDecline(offer.orderId)}
            disabled={isProcessing || isExpired}
          >
            <DeclineIcon />
            Decline
          </button>
          <button
            className="drv-offer-card__btn drv-offer-card__btn--accept"
            onClick={() => onAccept(offer.orderId)}
            disabled={isProcessing || isExpired}
          >
            <CheckIcon />
            {isProcessing ? 'Accepting...' : 'Accept'}
          </button>
        </div>
      </div>
    </div>
  );
};

// ─── Error Boundary ───────────────────────────────────────────────────────────

class DriverDashboardErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { error: Error | null }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error) {
    return { error };
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          padding: '32px',
          gap: '12px',
          background: '#f0f2f8',
          textAlign: 'center',
          fontFamily: 'system-ui, -apple-system, sans-serif',
        }}>
          <p style={{ fontWeight: 700, fontSize: '16px', color: '#1E2229', margin: '0 0 12px 0' }}>
            ⚠️ Driver Dashboard Error
          </p>
          <pre style={{
            fontSize: '13px',
            color: '#e53e3e',
            background: '#fff',
            padding: '16px',
            borderRadius: '8px',
            maxWidth: '600px',
            overflow: 'auto',
            textAlign: 'left',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
          }}>
            {this.state.error.message}
          </pre>
          <button
            onClick={() => window.location.reload()}
            style={{
              padding: '10px 24px',
              background: '#FF7622',
              color: '#fff',
              border: 'none',
              borderRadius: '8px',
              fontWeight: 700,
              fontSize: '14px',
              cursor: 'pointer',
            }}
          >
            Reload Page
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

// ─── Main Dashboard Component ──────────────────────────────────────────────────

interface DriverDashboardProps {
  // No props needed - driver ID comes from context
}

const DriverDashboard: React.FC<DriverDashboardProps> = () => {
  const navigate = useNavigate();
  const { isRegistered, isLoading, fetchDriverProfile, setIsRegistered, driverProfile } = useDriverAuth();
  const { showToast } = useToast();

  const { offers, connectToOffers, disconnectFromOffers, acceptOffer: acceptOfferFromContext, declineOffer: declineOfferFromContext } = useOffers();

  const [activeTab, setActiveTab] = useState('dashboard');
  const [status, setStatus] = useState<DriverStatus>('OFFLINE');
  const [activeDelivery, setActiveDelivery] = useState<ActiveDelivery | null>(null);
  const [processingOrderId, setProcessingOrderId] = useState<string | null>(null);
  const [statusToggling, setStatusToggling] = useState(false);

  // Earnings and delivery data
  const [earningsSummary, setEarningsSummary] = useState<EarningsSummary | null>(null);
  const [deliveryHistory, setDeliveryHistory] = useState<PaginatedResponse | null>(null);
  const [earningsHistory, setEarningsHistory] = useState<PaginatedResponse | null>(null);
  const [loadingEarnings, setLoadingEarnings] = useState(false);
  const [loadingDeliveries, setLoadingDeliveries] = useState(false);
  const [loadingEarningsHistory, setLoadingEarningsHistory] = useState(false);
  const [deliveryPage, setDeliveryPage] = useState(0);
  const [earningsPage, setEarningsPage] = useState(0);

  const locationIntervalRef = useRef<NodeJS.Timeout | null>(null);

  // ── Onboarding Guard & Load Earnings Data ─────────────────────────────────

  useEffect(() => {
    const initializeDriver = async () => {
      const storedUser = getStoredUser();
      const email = storedUser?.email;

      if (!email) {
        console.log('[DriverDashboard] No email found in stored user');
        navigate('/login');
        return;
      }

      console.log('[DriverDashboard] Initializing driver with email:', email);
      await fetchDriverProfile(email);
    };

    initializeDriver();
  }, [fetchDriverProfile, navigate]);

  // ── Load earnings summary when driver profile is loaded ───────────────────

  useEffect(() => {
    if (driverProfile && activeTab === 'dashboard') {
      fetchEarningsSummary();
    }
  }, [driverProfile, activeTab]);

  // ── Load delivery history when switching to tab ──────────────────────────

  useEffect(() => {
    if (driverProfile && activeTab === 'deliveries') {
      fetchDeliveryHistory();
    }
  }, [driverProfile, activeTab, deliveryPage]);

  // ── Load earnings history when switching to tab ─────────────────────────

  useEffect(() => {
    if (driverProfile && activeTab === 'earnings') {
      fetchEarningsHistory();
    }
  }, [driverProfile, activeTab, earningsPage]);

  // ─── API Handlers ──────────────────────────────────────────────────────────

  const updateDriverStatus = async (newStatus: DriverStatus) => {
    setStatusToggling(true);
    try {
      const token = getAccessToken();

      if (!driverProfile) {
        showToast('Driver profile not found', 'error');
        setStatusToggling(false);
        return;
      }

      const driverId = driverProfile.id;

      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      // Map AVAILABLE to IDLE for backend
      const backendStatus = newStatus === 'AVAILABLE' ? 'IDLE' : 'OFFLINE';

      console.log('[DriverDashboard] Updating status:', {
        driverId,
        currentStatus: newStatus,
        backendStatus,
      });

      const response = await fetch(`/api/drivers/${driverId}/status`, {
        method: 'PATCH',
        headers,
        body: JSON.stringify({ status: backendStatus }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `HTTP ${response.status}`;
        throw new Error(errorMessage);
      }

      // Update local state only after successful API call
      setStatus(newStatus);

      if (newStatus === 'OFFLINE') {
        disconnectFromOffers();
        if (locationIntervalRef.current) {
          clearInterval(locationIntervalRef.current);
          locationIntervalRef.current = null;
        }
        showToast('You are now offline', 'success');
      } else {
        connectToOffers(driverId);
        startLocationPings(driverId, driverProfile.cityZone);
        showToast('You are now available for deliveries', 'success');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      console.error('[DriverDashboard] Failed to update status:', errorMessage);
      showToast(`Failed to update status: ${errorMessage}`, 'error');
    } finally {
      setStatusToggling(false);
    }
  };

  // ── Earnings & Delivery API Calls ──────────────────────────────────────────

  const fetchEarningsSummary = async () => {
    if (!driverProfile) return;
    
    setLoadingEarnings(true);
    try {
      const token = getAccessToken();
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(`/api/drivers/${driverProfile.id}/earnings`, {
        method: 'GET',
        headers,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      console.log('[DriverDashboard] Earnings summary received:', data);
      setEarningsSummary(data.data || data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      console.error('[DriverDashboard] Failed to fetch earnings:', errorMessage);
      showToast('Failed to load earnings', 'error');
    } finally {
      setLoadingEarnings(false);
    }
  };

  const fetchDeliveryHistory = async () => {
    if (!driverProfile) return;

    setLoadingDeliveries(true);
    try {
      const token = getAccessToken();
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(
        `/api/drivers/${driverProfile.id}/deliveries?page=${deliveryPage}&size=10`,
        {
          method: 'GET',
          headers,
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      console.log('[DriverDashboard] Delivery history received:', data);
      setDeliveryHistory(data.data || data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      console.error('[DriverDashboard] Failed to fetch deliveries:', errorMessage);
      showToast('Failed to load delivery history', 'error');
    } finally {
      setLoadingDeliveries(false);
    }
  };

  const fetchEarningsHistory = async () => {
    if (!driverProfile) return;

    setLoadingEarningsHistory(true);
    try {
      const token = getAccessToken();
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch(
        `/api/drivers/${driverProfile.id}/earnings-history?page=${earningsPage}&size=10`,
        {
          method: 'GET',
          headers,
        }
      );

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const data = await response.json();
      console.log('[DriverDashboard] Earnings history received:', data);
      setEarningsHistory(data.data || data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      console.error('[DriverDashboard] Failed to fetch earnings history:', errorMessage);
      showToast('Failed to load earnings history', 'error');
    } finally {
      setLoadingEarningsHistory(false);
    }
  };

  const acceptOffer = async (orderId: string) => {
    if (!driverProfile) {
      showToast('Driver profile not found', 'error');
      return;
    }

    setProcessingOrderId(orderId);
    try {
      await acceptOfferFromContext(orderId, driverProfile.id);
      
      // Set as active delivery
      const offer = offers.find(o => o.orderId === orderId);
      if (offer) {
        setActiveDelivery({
          orderId: offer.orderId,
          restaurantName: offer.restaurantName,
          pickupArea: offer.pickupLocation,
          deliveryArea: offer.deliveryLocation,
          customerName: 'Customer',
        });
      }
      
      showToast('Order accepted! 🎉', 'success');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      console.error('[DriverDashboard] Failed to accept offer:', errorMessage);
      showToast(`Failed to accept order: ${errorMessage}`, 'error');
    } finally {
      setProcessingOrderId(null);
    }
  };

  const declineOffer = async (orderId: string) => {
    if (!driverProfile) {
      showToast('Driver profile not found', 'error');
      return;
    }

    try {
      await declineOfferFromContext(orderId, driverProfile.id);
      showToast('Order declined', 'success');
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      console.error('[DriverDashboard] Failed to decline offer:', errorMessage);
      showToast(`Failed to decline order: ${errorMessage}`, 'error');
    }
  };

  const startLocationPings = (driverId: string, cityZone: string) => {
    if (locationIntervalRef.current) {
      clearInterval(locationIntervalRef.current);
    }

    console.log('[DriverDashboard] Starting location pings');

    locationIntervalRef.current = setInterval(() => {
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (position) => {
            const { latitude, longitude } = position.coords;
            deliveryOfferService.sendLocationPing(driverId, latitude, longitude, cityZone)
              .catch(err => console.error('[DriverDashboard] Location ping failed:', err));
          },
          (error) => {
            console.error('[DriverDashboard] Geolocation error:', error.message);
          }
        );
      }
    }, 5000); // Every 5 seconds
  };

  // ── Render Guards ──────────────────────────────────────────────────────────

  console.log('[DriverDashboard] Render state - isLoading:', isLoading, 'isRegistered:', isRegistered);

  if (isLoading) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      }}>
        <div style={{ textAlign: 'center', color: 'white' }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>⏳</div>
          <p>Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  if (!isRegistered) {
    console.log('[DriverDashboard] Driver not registered - showing form');
    return <DriverRegistrationForm onRegistrationComplete={() => setIsRegistered(true)} />;
  }

  console.log('[DriverDashboard] Driver registered - rendering dashboard');

  return (
    <div className="drv-dashboard">
      {/* Sidebar */}
      <DriverSidebar
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onLogout={() => navigate('/login')}
      />

      {/* Main Content */}
      <main className="drv-main">
        {/* Top Header */}
        <header className="drv-header">
          <div className="drv-header__left">
            <h2 className="drv-header__greeting">Welcome Back, Driver! 👋</h2>
          </div>
          <div className="drv-header__right">
            <button
              className={`drv-status-toggle ${status === 'AVAILABLE' ? 'drv-status-toggle--active' : ''}`}
              onClick={() => updateDriverStatus(status === 'AVAILABLE' ? 'OFFLINE' : 'AVAILABLE')}
              disabled={statusToggling}
            >
              <OnlineIcon />
              <span>{status === 'AVAILABLE' ? '🟢 AVAILABLE' : '🔴 OFFLINE'}</span>
            </button>
          </div>
        </header>

        {/* Content */}
        <div className="drv-content">
            {/* Stats Row */}
          <div className="drv-stats">
            <StatCard
              icon={<BikeIcon />}
              value={loadingEarnings ? '...' : (earningsSummary?.activeDeliveries || 0)}
              label="Active Deliveries"
              hint="Your current active orders"
            />
            <StatCard
              icon={<CurrencyIcon />}
              value={loadingEarnings ? '...' : `₹${(earningsSummary?.todaysEarnings || 0).toFixed(2)}`}
              label="Today's Earnings"
              hint="Total settlement revenue"
            />
          </div>

          {/* Offers Section - Dashboard Tab */}
          {activeTab === 'dashboard' && (
            <section className="drv-section">
              <div className="drv-section__header">
                <h3 className="drv-section__title">Recent Offers & Active Runs</h3>
              </div>

              {status === 'OFFLINE' ? (
                <div className="drv-empty-state">
                  <div style={{ fontSize: '48px', marginBottom: '12px' }}>📴</div>
                  <p className="drv-empty-state__text">You're currently offline</p>
                  <p className="drv-empty-state__subtext">Go online to see delivery offers</p>
                </div>
              ) : offers.length === 0 && !activeDelivery ? (
                <div className="drv-empty-state">
                  <div style={{ fontSize: '48px', marginBottom: '12px' }}>🔍</div>
                  <p className="drv-empty-state__text">No offers available right now</p>
                  <p className="drv-empty-state__subtext">Check back soon for new deliveries</p>
                </div>
              ) : (
                <div className="drv-offers-list">
                  {activeDelivery && (
                    <div className="drv-offer-card drv-offer-card--active">
                      <div className="drv-offer-card__header">
                        <h4 className="drv-offer-card__restaurant">{activeDelivery.restaurantName}</h4>
                        <span className="drv-offer-card__badge">🚗 In Progress</span>
                      </div>
                      <div className="drv-offer-card__routes">
                        <div className="drv-offer-card__route">
                          <LocationIcon />
                          <div>
                            <div className="drv-offer-card__route-label">Pickup</div>
                            <div className="drv-offer-card__route-value">{activeDelivery.pickupArea}</div>
                          </div>
                        </div>
                        <div className="drv-offer-card__arrow">→</div>
                        <div className="drv-offer-card__route">
                          <LocationIcon />
                          <div>
                            <div className="drv-offer-card__route-label">Delivery</div>
                            <div className="drv-offer-card__route-value">{activeDelivery.deliveryArea}</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  )}

                  {offers.map((offer) => (
                    <OfferCard
                      key={offer.orderId}
                      offer={offer}
                      onAccept={acceptOffer}
                      onDecline={declineOffer}
                      isProcessing={processingOrderId === offer.orderId}
                    />
                  ))}
                </div>
              )}
            </section>
          )}

          {/* My Deliveries Tab */}
          {activeTab === 'deliveries' && (
            <section className="drv-section">
              <div className="drv-section__header">
                <h3 className="drv-section__title">My Deliveries</h3>
              </div>

              {loadingDeliveries ? (
                <div className="drv-empty-state">
                  <div style={{ fontSize: '48px', marginBottom: '12px' }}>⏳</div>
                  <p className="drv-empty-state__text">Loading deliveries...</p>
                </div>
              ) : deliveryHistory?.content && deliveryHistory.content.length > 0 ? (
                <>
                  <div className="drv-delivery-list">
                    {deliveryHistory.content.map((delivery) => (
                      <div key={delivery.id} className="drv-delivery-item">
                        <div className="drv-delivery-header">
                          <h4 className="drv-delivery-restaurant">{delivery.restaurantName}</h4>
                          <span className={`drv-delivery-status drv-delivery-status--${delivery.deliveryStatus.toLowerCase()}`}>
                            {delivery.deliveryStatus}
                          </span>
                        </div>
                        <div className="drv-delivery-details">
                          <div className="drv-delivery-route">
                            <span className="drv-delivery-label">📍 Pickup:</span>
                            <span className="drv-delivery-value">{delivery.pickupAddress}</span>
                          </div>
                          <div className="drv-delivery-route">
                            <span className="drv-delivery-label">📍 Delivery:</span>
                            <span className="drv-delivery-value">{delivery.deliveryAddress}</span>
                          </div>
                          <div className="drv-delivery-footer">
                            <div className="drv-delivery-payout">
                              <MoneyIcon />
                              <span>₹{delivery.payoutAmount.toFixed(2)}</span>
                            </div>
                            {delivery.customerRating && (
                              <div className="drv-delivery-rating">
                                ⭐ {delivery.customerRating}/5
                              </div>
                            )}
                            <div className="drv-delivery-date">
                              {new Date(delivery.completedAt).toLocaleDateString()}
                            </div>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Pagination */}
                  {deliveryHistory && deliveryHistory.totalPages > 1 && (
                    <div className="drv-pagination">
                      <button
                        onClick={() => setDeliveryPage(Math.max(0, deliveryPage - 1))}
                        disabled={deliveryHistory.isFirst}
                        className="drv-pagination__btn"
                      >
                        ← Previous
                      </button>
                      <span className="drv-pagination__info">
                        Page {deliveryHistory.pageNumber + 1} of {deliveryHistory.totalPages}
                      </span>
                      <button
                        onClick={() => setDeliveryPage(deliveryPage + 1)}
                        disabled={deliveryHistory.isLast}
                        className="drv-pagination__btn"
                      >
                        Next →
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <div className="drv-empty-state">
                  <div style={{ fontSize: '48px', marginBottom: '12px' }}>📦</div>
                  <p className="drv-empty-state__text">No deliveries yet</p>
                  <p className="drv-empty-state__subtext">Your completed deliveries will appear here</p>
                </div>
              )}
            </section>
          )}

          {/* Earnings History Tab */}
          {activeTab === 'earnings' && (
            <section className="drv-section">
              <div className="drv-section__header">
                <h3 className="drv-section__title">Earnings History</h3>
                {earningsHistory && (
                  <div className="drv-section__summary">
                    <strong>Total Earnings: </strong>₹{(earningsHistory.content.reduce((sum, d) => sum + d.payoutAmount, 0)).toFixed(2)}
                  </div>
                )}
              </div>

              {loadingEarningsHistory ? (
                <div className="drv-empty-state">
                  <div style={{ fontSize: '48px', marginBottom: '12px' }}>⏳</div>
                  <p className="drv-empty-state__text">Loading earnings...</p>
                </div>
              ) : earningsHistory?.content && earningsHistory.content.length > 0 ? (
                <>
                  <div className="drv-earnings-list">
                    {earningsHistory.content.map((earning) => (
                      <div key={earning.id} className="drv-earnings-item">
                        <div className="drv-earnings-left">
                          <h4 className="drv-earnings-restaurant">{earning.restaurantName}</h4>
                          <span className="drv-earnings-order">Order: {earning.orderId}</span>
                        </div>
                        <div className="drv-earnings-center">
                          <div className="drv-earnings-route-short">
                            {earning.pickupAddress} → {earning.deliveryAddress}
                          </div>
                        </div>
                        <div className="drv-earnings-right">
                          <div className="drv-earnings-amount">₹{earning.payoutAmount.toFixed(2)}</div>
                          <div className="drv-earnings-date">
                            {new Date(earning.completedAt).toLocaleDateString()}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>

                  {/* Pagination */}
                  {earningsHistory && earningsHistory.totalPages > 1 && (
                    <div className="drv-pagination">
                      <button
                        onClick={() => setEarningsPage(Math.max(0, earningsPage - 1))}
                        disabled={earningsHistory.isFirst}
                        className="drv-pagination__btn"
                      >
                        ← Previous
                      </button>
                      <span className="drv-pagination__info">
                        Page {earningsHistory.pageNumber + 1} of {earningsHistory.totalPages}
                      </span>
                      <button
                        onClick={() => setEarningsPage(earningsPage + 1)}
                        disabled={earningsHistory.isLast}
                        className="drv-pagination__btn"
                      >
                        Next →
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <div className="drv-empty-state">
                  <div style={{ fontSize: '48px', marginBottom: '12px' }}>💰</div>
                  <p className="drv-empty-state__text">No earnings yet</p>
                  <p className="drv-empty-state__subtext">Your earnings will appear here once you complete deliveries</p>
                </div>
              )}
            </section>
          )}
        </div>
      </main>
    </div>
  );
};

const DriverDashboardWithErrorBoundary: React.FC<DriverDashboardProps> = (props) => (
  <DriverDashboardErrorBoundary>
    <DriverDashboard {...props} />
  </DriverDashboardErrorBoundary>
);

export default DriverDashboardWithErrorBoundary;
