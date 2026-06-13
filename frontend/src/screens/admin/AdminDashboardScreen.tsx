import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import AdminLayout from './AdminLayout';
import { useRestaurant } from '../../context/useRestaurant';
import { fetchRestaurantOrders, type RestaurantOrder, type Page } from '../../services/orderService';
import { updateRestaurantStatus } from '../../services/restaurantService';
import { formatItemsSummary, formatOrderLabel } from '../../utils/adminOrderMappers';
import './AdminDashboardScreen.css';

// ─── Types ────────────────────────────────────────────────────────────────────

interface Toast {
  id:      number;
  message: string;
  type:    'success' | 'error' | 'warning';
}

// ─── Icons ────────────────────────────────────────────────────────────────────

const OrdersStatIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"
      stroke="#FF7622" strokeWidth="2" strokeLinecap="round"/>
    <rect x="9" y="3" width="6" height="4" rx="1" stroke="#FF7622" strokeWidth="2"/>
    <line x1="9" y1="12" x2="15" y2="12" stroke="#FF7622" strokeWidth="2" strokeLinecap="round"/>
    <line x1="9" y1="16" x2="13" y2="16" stroke="#FF7622" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

const RevenueStatIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="9" stroke="#22C55E" strokeWidth="2"/>
    <path d="M12 7v10M9.5 9.5C9.5 8.4 10.6 7.5 12 7.5s2.5.9 2.5 2-.9 1.8-2.5 2-2.5.9-2.5 2 1.1 2 2.5 2 2.5-.9 2.5-2"
      stroke="#22C55E" strokeWidth="1.8" strokeLinecap="round"/>
  </svg>
);

const ChevronLeftIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="15 18 9 12 15 6" stroke="currentColor" strokeWidth="2.2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const ChevronRightIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="9 18 15 12 9 6" stroke="currentColor" strokeWidth="2.2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const ToastSuccessIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#22C55E" opacity="0.15"/>
    <polyline points="8 12 11 15 16 9" stroke="#22C55E" strokeWidth="2.2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const ToastErrorIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#EF4444" opacity="0.15"/>
    <line x1="15" y1="9" x2="9" y2="15" stroke="#EF4444" strokeWidth="2.2" strokeLinecap="round"/>
    <line x1="9" y1="9" x2="15" y2="15" stroke="#EF4444" strokeWidth="2.2" strokeLinecap="round"/>
  </svg>
);

// ─── Empty state illustration ─────────────────────────────────────────────────

const EmptyOrdersIllustration = () => (
  <svg width="64" height="64" viewBox="0 0 64 64" fill="none" aria-hidden="true">
    <rect x="12" y="8" width="40" height="48" rx="6" fill="#F0F2F8"/>
    <rect x="12" y="8" width="40" height="48" rx="6" stroke="#ECEEF4" strokeWidth="1.5"/>
    <rect x="20" y="20" width="24" height="3" rx="1.5" fill="#D0D5DD"/>
    <rect x="20" y="27" width="18" height="3" rx="1.5" fill="#D0D5DD"/>
    <rect x="20" y="34" width="21" height="3" rx="1.5" fill="#D0D5DD"/>
    <circle cx="32" cy="50" r="10" fill="#ECEEF4"/>
    <path d="M28 50h8M32 46v8" stroke="#C0C4D0" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

// ─── Toast stack ──────────────────────────────────────────────────────────────

const ToastStack: React.FC<{ toasts: Toast[] }> = ({ toasts }) => (
  <div className="adm-toast-stack" aria-live="polite" aria-atomic="false">
    {toasts.map((t) => (
      <div
        key={t.id}
        className={`adm-toast adm-toast--${t.type}`}
        role="status"
      >
        {t.type === 'success' && <ToastSuccessIcon />}
        {t.type === 'error'   && <ToastErrorIcon />}
        <span className="adm-toast__msg">{t.message}</span>
      </div>
    ))}
  </div>
);

// ─── Skeleton row ─────────────────────────────────────────────────────────────

const SkeletonRow = () => (
  <tr className="adm-dash__skeleton-row" aria-hidden="true">
    {[80, 110, 200, 70].map((w, i) => (
      <td key={i}>
        <span className="adm-dash__skeleton-cell" style={{ width: w }}/>
      </td>
    ))}
  </tr>
);

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('en-US', {
      month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  } catch {
    return iso;
  }
}

function formatAmount(amount: number): string {
  return `$${amount.toFixed(2)}`;
}

// ─── Full-page loading spinner ────────────────────────────────────────────────

const PageSpinner: React.FC = () => (
  <div className="adm-dash__page-spinner" aria-label="Loading dashboard" role="status">
    <svg className="adm-dash__spinner-svg" viewBox="0 0 50 50" aria-hidden="true">
      <circle cx="25" cy="25" r="20" fill="none"
        stroke="#FF7622" strokeWidth="4"
        strokeLinecap="round" strokeDasharray="100" strokeDashoffset="60"/>
    </svg>
    <span className="adm-dash__spinner-label">Loading your dashboard…</span>
  </div>
);

// ─── No-restaurant warning banner ─────────────────────────────────────────────

const NoRestaurantBanner: React.FC = () => (
  <div className="adm-dash__no-restaurant" role="alert" aria-live="assertive">
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" stroke="#F59E0B" strokeWidth="2"/>
      <line x1="12" y1="8" x2="12" y2="12" stroke="#F59E0B" strokeWidth="2.2" strokeLinecap="round"/>
      <circle cx="12" cy="16" r="1.2" fill="#F59E0B"/>
    </svg>
    <div className="adm-dash__no-restaurant-text">
      <strong>Error: No restaurant profiles associated with this merchant account.</strong>
      <span>Please contact support or ensure your account has a linked restaurant.</span>
    </div>
  </div>
);

// ─── Component ────────────────────────────────────────────────────────────────

const PAGE_SIZE = 5;
let toastCounter = 0;
const TOAST_FADE_MS = 3500;

const AdminDashboardScreen: React.FC = () => {
  const navigate = useNavigate();

  // ── Pull restaurantId and status from global context ──────────────────────
  const { activeRestaurantId, restaurantStatus, profileLoading, noRestaurant, profileError, setRestaurantStatus } = useRestaurant();

  // ── Paginated orders state ─────────────────────────────────────────────
  const [page,       setPage]       = useState(0);
  const [pageData,   setPageData]   = useState<Page<RestaurantOrder> | null>(null);
  const [loading,    setLoading]    = useState(false);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // ── Toast state ────────────────────────────────────────────────────────
  const [toasts, setToasts] = useState<Toast[]>([]);
  const [statusToggling, setStatusToggling] = useState(false);

  // ── Push toast helper ──────────────────────────────────────────────────
  const pushToast = useCallback((message: string, type: Toast['type']) => {
    const id = ++toastCounter;
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, TOAST_FADE_MS);
  }, []);

  // ── Derived stat counters ──────────────────────────────────────────────
  const totalOrders  = pageData?.totalElements ?? 0;
  const totalRevenue = pageData?.content.reduce((sum, o) => sum + o.totalAmount, 0) ?? 0;

  // ── Fetch orders once activeRestaurantId is available ─────────────────
  const loadOrders = useCallback(async (rid: string, p: number) => {
    setLoading(true);
    setFetchError(null);
    try {
      const data = await fetchRestaurantOrders(rid, p, PAGE_SIZE);
      setPageData(data);
    } catch (err) {
      setFetchError(err instanceof Error ? err.message : 'Failed to load orders.');
    } finally {
      setLoading(false);
    }
  }, []);

  // ── Status toggle handler ──────────────────────────────────────────────
  const handleStatusToggle = useCallback(async () => {
    if (!activeRestaurantId || !restaurantStatus) return;

    setStatusToggling(true);
    try {
      const newStatus = restaurantStatus === 'OPEN' ? 'CLOSED' : 'OPEN';
      await updateRestaurantStatus(activeRestaurantId, newStatus);
      setRestaurantStatus(newStatus);
      pushToast(`Restaurant is now ${newStatus}.`, 'success');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to update status.';
      pushToast(msg, 'error');
    } finally {
      setStatusToggling(false);
    }
  }, [activeRestaurantId, restaurantStatus, setRestaurantStatus, pushToast]);

  useEffect(() => {
    if (activeRestaurantId) {
      loadOrders(activeRestaurantId, page);
    }
  }, [activeRestaurantId, page, loadOrders]);

  // ── Pagination ─────────────────────────────────────────────────────────
  const totalPages  = pageData?.totalPages ?? 1;
  const canPrevious = page > 0;
  const canNext     = page < totalPages - 1;
  const goToPrev    = () => { if (canPrevious) setPage((p) => p - 1); };
  const goToNext    = () => { if (canNext)     setPage((p) => p + 1); };

  const orders  = pageData?.content ?? [];
  const isEmpty = !loading && !fetchError && orders.length === 0;
  const hasData = !loading && !fetchError && orders.length > 0;

  return (
    <AdminLayout>
      <div className="adm-dash">

        {/* Page header */}
        <div className="adm-dash__header">
          <div>
            <h1 className="adm-dash__title">Dashboard</h1>
            <p className="adm-dash__subtitle">Here's your restaurant overview for today.</p>
            
            {/* Status toggle — shown when restaurant is loaded */}
            {!profileLoading && activeRestaurantId && restaurantStatus && (
              <div className="adm-dash__status-toggle">
                <span className={`adm-dash__status-badge adm-dash__status-badge--${restaurantStatus.toLowerCase()}`}>
                  {restaurantStatus === 'OPEN' ? '🟢' : '🔴'} {restaurantStatus}
                </span>
                <button
                  className="adm-dash__toggle-btn"
                  onClick={handleStatusToggle}
                  disabled={statusToggling}
                  aria-label={`Toggle restaurant status (currently ${restaurantStatus})`}
                  title={`Click to change status from ${restaurantStatus}`}
                >
                  {statusToggling ? 'Updating...' : 'Change'}
                </button>
              </div>
            )}
          </div>
          <div className="adm-dash__date">
            {new Date().toLocaleDateString('en-US', {
              weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
            })}
          </div>
        </div>

        {/* ── Profile loading spinner — covers widgets while context resolves ── */}
        {profileLoading && <PageSpinner />}

        {/* ── No restaurant warning banner ── */}
        {!profileLoading && (noRestaurant || profileError) && (
          <NoRestaurantBanner />
        )}

        {/* ── Main content — only shown once restaurantId is confirmed ── */}
        {!profileLoading && activeRestaurantId && (
          <>
            {/* 2-card stat row */}
            <div className="adm-dash__stats adm-dash__stats--two">

              <div className="adm-stat-card">
                <div className="adm-stat-card__top">
                  <div className="adm-stat-card__icon-wrap adm-stat-card__icon-wrap--orange">
                    <OrdersStatIcon />
                  </div>
                </div>
                <div className="adm-stat-card__value">
                  {loading ? <span className="adm-dash__stat-skeleton"/> : totalOrders}
                </div>
                <div className="adm-stat-card__label">Total Orders Today</div>
                <div className="adm-stat-card__hint">
                  Orders will appear here once customers start placing them.
                </div>
              </div>

              <div className="adm-stat-card">
                <div className="adm-stat-card__top">
                  <div className="adm-stat-card__icon-wrap adm-stat-card__icon-wrap--green">
                    <RevenueStatIcon />
                  </div>
                </div>
                <div className="adm-stat-card__value">
                  {loading ? <span className="adm-dash__stat-skeleton"/> : formatAmount(totalRevenue)}
                </div>
                <div className="adm-stat-card__label">Total Revenue Today</div>
                <div className="adm-stat-card__hint">
                  Revenue updates automatically as orders are completed.
                </div>
              </div>

            </div>

            {/* Recent orders section */}
            <div className="adm-dash__section">
              <div className="adm-dash__section-header">
                <h2 className="adm-dash__section-title">Recent Orders</h2>
                <button className="adm-dash__see-all" onClick={() => navigate('/admin/orders')}>
                  View All Orders →
                </button>
              </div>

              {/* Fetch error */}
              {fetchError && (
                <div className="adm-dash__error" role="alert">
                  {fetchError}
                  <button
                    className="adm-dash__retry"
                    onClick={() => loadOrders(activeRestaurantId, page)}
                  >
                    Retry
                  </button>
                </div>
              )}

              {/* Empty state */}
              {isEmpty && (
                <div className="adm-dash__empty" role="status" aria-live="polite">
                  <EmptyOrdersIllustration />
                  <p className="adm-dash__empty-text">No orders placed today yet.</p>
                  <p className="adm-dash__empty-sub">New orders will show up here in real time.</p>
                </div>
              )}

              {/* Table */}
              {(loading || hasData) && (
                <div className="adm-dash__table-wrap">
                  <table className="adm-dash__table" aria-label="Recent orders" aria-busy={loading}>
                    <thead>
                      <tr>
                        <th>Order ID</th>
                        <th>Timestamp</th>
                        <th>Items</th>
                        <th>Amount</th>
                      </tr>
                    </thead>
                    <tbody>
                      {loading
                        ? Array.from({ length: PAGE_SIZE }).map((_, i) => <SkeletonRow key={i}/>)
                        : orders.map((order) => (
                            <tr key={order.id}>
                              <td><span className="adm-dash__order-id">{formatOrderLabel(order.id)}</span></td>
                              <td><span className="adm-dash__time">{formatDate(order.createdAt)}</span></td>
                              <td><span className="adm-dash__items">{formatItemsSummary(order.items)}</span></td>
                              <td><span className="adm-dash__total">{formatAmount(order.totalAmount)}</span></td>
                            </tr>
                          ))
                      }
                    </tbody>
                  </table>
                </div>
              )}

              {/* Pagination */}
              {!loading && !fetchError && totalPages > 1 && (
                <div className="adm-dash__pagination" role="navigation" aria-label="Order pages">
                  <button className="adm-dash__page-btn" onClick={goToPrev}
                    disabled={!canPrevious} aria-label="Previous page">
                    <ChevronLeftIcon />
                  </button>
                  <span className="adm-dash__page-info">
                    Page <strong>{page + 1}</strong> of <strong>{totalPages}</strong>
                  </span>
                  <button className="adm-dash__page-btn" onClick={goToNext}
                    disabled={!canNext} aria-label="Next page">
                    <ChevronRightIcon />
                  </button>
                </div>
              )}

            </div>
          </>
        )}

        <ToastStack toasts={toasts} />

      </div>
    </AdminLayout>
  );
};

export default AdminDashboardScreen;
