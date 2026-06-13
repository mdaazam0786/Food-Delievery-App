import React, { useState, useCallback, useEffect, useRef } from 'react';
import AdminLayout from './AdminLayout';
import { useRestaurant } from '../../context/useRestaurant';
import { fetchRestaurantOrders, updateOrderStatus, STATUS_MAP, type OrderStatusPayload } from '../../services/orderService';
import {
  acceptRestaurantOrder,
  declineRestaurantOrder,
  openRestaurantOrderStream,
  type PendingOrderPayload,
} from '../../services/restaurantAcceptanceService';
import {
  splitRestaurantOrders,
  toIncomingOrder,
  toActiveOrder,
  type ActiveStatus,
  type ActiveOrder,
  type IncomingOrder,
} from '../../utils/adminOrderMappers';
import './AdminOrdersScreen.css';

// ─── Types ────────────────────────────────────────────────────────────────────

interface Toast {
  id:      number;
  message: string;
  type:    'success' | 'error' | 'warning';
}

// ─── Status config ────────────────────────────────────────────────────────────

const STATUS_STEPS: { value: ActiveStatus; label: string; color: string }[] = [
  { value: 'preparing', label: 'Preparing',        color: '#FF7622' },
  { value: 'ready',     label: 'Ready to Pick',    color: '#F59E0B' },
  { value: 'delivery',  label: 'Out for Delivery', color: '#6366F1' },
];

const ORDERS_FETCH_SIZE = 50;

// ─── Icons ────────────────────────────────────────────────────────────────────

const ClockIcon = () => (
  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" stroke="#9EA1B1" strokeWidth="2"/>
    <path d="M12 6v6l4 2" stroke="#9EA1B1" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const CheckIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="20 6 9 17 4 12" stroke="currentColor" strokeWidth="2.5"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const SyncIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="23 4 23 10 17 10" stroke="currentColor" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round"/>
    <polyline points="1 20 1 14 7 14" stroke="currentColor" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M3.51 9a9 9 0 0114.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0020.49 15"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const SpinnerIcon = () => (
  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" aria-hidden="true"
    className="adm-spin">
    <circle cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="2.5"
      strokeLinecap="round" strokeDasharray="32" strokeDashoffset="12"/>
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

const ToastWarningIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#F59E0B" opacity="0.15"/>
    <line x1="12" y1="8" x2="12" y2="13" stroke="#F59E0B" strokeWidth="2.2" strokeLinecap="round"/>
    <circle cx="12" cy="16.5" r="1.2" fill="#F59E0B"/>
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
        {t.type === 'warning' && <ToastWarningIcon />}
        <span className="adm-toast__msg">{t.message}</span>
      </div>
    ))}
  </div>
);

// ─── Incoming order card ──────────────────────────────────────────────────────

interface IncomingCardProps {
  order:      IncomingOrder;
  exiting:    boolean;
  processing: boolean;
  onDecline:  (id: string) => void;
  onAccept:   (id: string) => void;
}

const IncomingCard: React.FC<IncomingCardProps> = ({
  order, exiting, processing, onDecline, onAccept,
}) => (
  <article
    className={`adm-inc-card${exiting ? ' adm-inc-card--exiting' : ''}`}
    aria-label={`Incoming order ${order.orderId}`}
  >
    <div className="adm-inc-card__header">
      <span className="adm-inc-card__id">{order.orderId}</span>
      <span className="adm-inc-card__time"><ClockIcon />{order.elapsed}</span>
    </div>
    <div className="adm-inc-card__customer">{order.customer}</div>
    <p className="adm-inc-card__items">{order.items}</p>
    <div className="adm-inc-card__footer">
      <span className="adm-inc-card__total">{order.total}</span>
      <div className="adm-inc-card__actions">
        <button
          className="adm-inc-card__btn adm-inc-card__btn--decline"
          onClick={() => onDecline(order.id)}
          disabled={processing || exiting}
          aria-label={`Decline ${order.orderId}`}
        >
          {processing ? 'Declining…' : 'Decline'}
        </button>
        <button
          className="adm-inc-card__btn adm-inc-card__btn--accept"
          onClick={() => onAccept(order.id)}
          disabled={processing || exiting}
          aria-label={`Accept ${order.orderId}`}
        >
          {processing ? 'Accepting…' : 'Accept'}
        </button>
      </div>
    </div>
  </article>
);

// ─── Active order card ────────────────────────────────────────────────────────

interface ActiveCardProps {
  order:          ActiveOrder;
  onStatusChange: (id: string, status: ActiveStatus) => void;
}

const ActiveCard: React.FC<ActiveCardProps> = ({ order, onStatusChange }) => {
  const currentIdx = STATUS_STEPS.findIndex((s) => s.value === order.status);

  return (
    <article
      className={`adm-active-card${order.updating ? ' adm-active-card--updating' : ''}`}
      aria-label={`Order ${order.orderId} — ${order.status}`}
      aria-busy={order.updating}
    >
      <div className="adm-active-card__header">
        <span className="adm-active-card__id">{order.orderId}</span>
        <span className="adm-active-card__time"><ClockIcon />{order.elapsed}</span>
      </div>
      <div className="adm-active-card__customer">{order.customer}</div>
      <p className="adm-active-card__items">{order.items}</p>

      <div className="adm-stepper" role="group" aria-label="Order status steps">
        {STATUS_STEPS.map((step, idx) => {
          const isDone    = idx < currentIdx;
          const isCurrent = idx === currentIdx;
          return (
            <React.Fragment key={step.value}>
              <button
                className={`adm-stepper__step${isDone ? ' adm-stepper__step--done' : ''}${isCurrent ? ' adm-stepper__step--current' : ''}`}
                style={isCurrent ? { borderColor: step.color, color: step.color } : undefined}
                onClick={() => !order.updating && onStatusChange(order.id, step.value)}
                aria-pressed={isCurrent}
                aria-label={`Set status to ${step.label}`}
                disabled={isDone || order.updating}
              >
                {isDone ? (
                  <span className="adm-stepper__check"><CheckIcon /></span>
                ) : isCurrent && order.updating ? (
                  <span className="adm-stepper__check"><SpinnerIcon /></span>
                ) : (
                  <span className="adm-stepper__dot"
                    style={isCurrent ? { background: step.color } : undefined}/>
                )}
                <span className="adm-stepper__label">{step.label}</span>
              </button>
              {idx < STATUS_STEPS.length - 1 && (
                <span
                  className={`adm-stepper__line${idx < currentIdx ? ' adm-stepper__line--done' : ''}`}
                  aria-hidden="true"
                />
              )}
            </React.Fragment>
          );
        })}
      </div>

      <div
        className={`adm-active-card__sync${order.synced ? ' adm-active-card__sync--visible' : ''}`}
        aria-live="polite"
      >
        <SyncIcon />
        Status synced to customer
      </div>

      <div className="adm-active-card__footer">
        <span className="adm-active-card__total">{order.total}</span>
        <span
          className="adm-active-card__badge"
          style={{
            background: `${STATUS_STEPS[currentIdx]?.color}18`,
            color:       STATUS_STEPS[currentIdx]?.color,
          }}
        >
          {STATUS_STEPS[currentIdx]?.label}
        </span>
      </div>
    </article>
  );
};

// ─── Main screen ──────────────────────────────────────────────────────────────

let toastCounter = 0;
const FADE_MS = 320;

const AdminOrdersScreen: React.FC = () => {
  const { activeRestaurantId, profileLoading } = useRestaurant();

  const [incoming,      setIncoming]      = useState<IncomingOrder[]>([]);
  const [active,        setActive]        = useState<ActiveOrder[]>([]);
  const [toasts,        setToasts]        = useState<Toast[]>([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [fetchError,    setFetchError]    = useState<string | null>(null);
  const [processingIds, setProcessingIds] = useState<Set<string>>(new Set());
  const [exitingIds,    setExitingIds]    = useState<Set<string>>(new Set());

  const enrichPendingRef = useRef<(pending: PendingOrderPayload) => Promise<void>>();

  // ── Toast helpers ──────────────────────────────────────────────────────────
  const pushToast = useCallback((message: string, type: Toast['type']) => {
    const id = ++toastCounter;
    setToasts((prev) => [...prev, { id, message, type }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 3500);
  }, []);

  // ── Load orders from order-service ─────────────────────────────────────────
  const loadOrders = useCallback(async (restaurantId: string) => {
    setFetchError(null);
    try {
      const page = await fetchRestaurantOrders(restaurantId, 0, ORDERS_FETCH_SIZE);
      const { incoming: inc, active: act } = splitRestaurantOrders(page.content);
      setIncoming(inc);
      setActive(act);
    } catch (err) {
      setFetchError(err instanceof Error ? err.message : 'Failed to load orders.');
    } finally {
      setInitialLoading(false);
    }
  }, []);

  // Cold-start fetch once restaurantId is available
  useEffect(() => {
    if (!activeRestaurantId) {
      if (!profileLoading) setInitialLoading(false);
      return;
    }
    loadOrders(activeRestaurantId);
  }, [activeRestaurantId, profileLoading, loadOrders]);

  // Enrich a pending SSE payload with full order details from order-service
  const enrichPendingOrder = useCallback(async (pending: PendingOrderPayload) => {
    if (!activeRestaurantId) return;

    try {
      const page = await fetchRestaurantOrders(activeRestaurantId, 0, ORDERS_FETCH_SIZE);
      const match = page.content.find((o) => o.id === pending.orderId);
      if (match?.status === 'PREPARING') {
        setIncoming((prev) => {
          if (prev.some((o) => o.rawId === pending.orderId)) return prev;
          return [toIncomingOrder(match), ...prev];
        });
      }
    } catch {
      /* SSE will retry on next poll cycle if enrichment fails */
    }
  }, [activeRestaurantId]);

  enrichPendingRef.current = enrichPendingOrder;

  // SSE live stream for new incoming orders
  useEffect(() => {
    if (!activeRestaurantId) return;

    const es = openRestaurantOrderStream(activeRestaurantId, {
      onNewOrder: (pending) => {
        enrichPendingRef.current?.(pending);
      },
      onOrderExpired: (orderId) => {
        setIncoming((prev) => prev.filter((o) => o.rawId !== orderId));
      },
    });

    return () => es?.close();
  }, [activeRestaurantId]);

  // Fallback poll every 30 s in case SSE misses an event
  useEffect(() => {
    if (!activeRestaurantId) return;
    const interval = setInterval(() => loadOrders(activeRestaurantId), 30_000);
    return () => clearInterval(interval);
  }, [activeRestaurantId, loadOrders]);

  // ── Decline → POST /api/restaurants/orders/{orderId}/decline ───────────────
  const handleDecline = useCallback(async (id: string) => {
    const order = incoming.find((o) => o.id === id);
    if (!order || processingIds.has(id)) return;

    setProcessingIds((prev) => new Set(prev).add(id));

    try {
      await declineRestaurantOrder(order.rawId);
      setExitingIds((prev) => new Set(prev).add(id));

      setTimeout(() => {
        setIncoming((prev) => prev.filter((o) => o.id !== id));
        setExitingIds((prev) => {
          const next = new Set(prev);
          next.delete(id);
          return next;
        });
        pushToast(`Order ${order.orderId} was declined.`, 'warning');
      }, FADE_MS);
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to decline order.';
      pushToast(msg, 'error');
    } finally {
      setProcessingIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  }, [incoming, processingIds, pushToast]);

  // ── Accept → POST /api/restaurants/orders/{orderId}/accept ───────────────
  const handleAccept = useCallback(async (id: string) => {
    const order = incoming.find((o) => o.id === id);
    if (!order || processingIds.has(id)) return;

    setProcessingIds((prev) => new Set(prev).add(id));

    try {
      await acceptRestaurantOrder(order.rawId);

      setIncoming((prev) => prev.filter((o) => o.id !== id));
      
      // Convert IncomingOrder to ActiveOrder
      const activeOrder: ActiveOrder = {
        ...order,
        status: 'preparing',
        synced: false,
        updating: false,
      };
      
      setActive((prev) => [activeOrder, ...prev]);
      pushToast(`${order.orderId} accepted — now preparing.`, 'success');
    } catch (err) {
      const msg = err instanceof Error ? err.message : 'Failed to accept order.';
      pushToast(msg, 'error');
    } finally {
      setProcessingIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
    }
  }, [incoming, processingIds, pushToast]);

  // ── Status change → PATCH /api/orders/{orderId}/status ─────────────────────
  const handleStatusChange = useCallback(async (id: string, status: ActiveStatus) => {
    const order = active.find((o) => o.id === id);
    if (!order || order.updating) return;

    const payload = STATUS_MAP[status] as OrderStatusPayload;

    setActive((prev) =>
      prev.map((o) => (o.id === id ? { ...o, updating: true, synced: false } : o))
    );

    try {
      await updateOrderStatus(order.rawId, payload);

      setActive((prev) =>
        prev.map((o) =>
          o.id === id ? { ...o, status, updating: false, synced: true } : o
        )
      );

      pushToast('Order status synchronized successfully.', 'success');

      setTimeout(() => {
        setActive((prev) =>
          prev.map((o) => (o.id === id ? { ...o, synced: false } : o))
        );
      }, 2500);
    } catch (err) {
      setActive((prev) =>
        prev.map((o) => (o.id === id ? { ...o, updating: false } : o))
      );
      const msg = err instanceof Error ? err.message : 'Failed to update order status.';
      pushToast(msg, 'error');
    }
  }, [active, pushToast]);

  const incomingCount = incoming.length;
  const activeCount   = active.length;

  return (
    <AdminLayout>
      <div className="adm-orders">

        <div className="adm-orders__header">
          <div>
            <h1 className="adm-orders__title">Order Management</h1>
            <p className="adm-orders__subtitle">
              Accept incoming orders and track them through to delivery.
            </p>
          </div>
          <div className="adm-orders__counts">
            <span className="adm-orders__count-badge adm-orders__count-badge--blue">
              {incomingCount} incoming
            </span>
            <span className="adm-orders__count-badge adm-orders__count-badge--orange">
              {activeCount} active
            </span>
          </div>
        </div>

        {fetchError && (
          <div className="adm-orders__error" role="alert">
            {fetchError}
            {activeRestaurantId && (
              <button
                className="adm-orders__retry"
                onClick={() => loadOrders(activeRestaurantId)}
              >
                Retry
              </button>
            )}
          </div>
        )}

        {/* ── Incoming orders ── */}
        <section className="adm-orders__section" aria-label="Incoming orders">
          <div className="adm-orders__section-header">
            <span className="adm-orders__section-dot adm-orders__section-dot--blue" aria-hidden="true"/>
            <h2 className="adm-orders__section-title">Incoming Orders</h2>
            <span className="adm-orders__section-count adm-orders__section-count--blue">
              {incomingCount}
            </span>
          </div>
          {initialLoading ? (
            <div className="adm-orders__empty" aria-live="polite">Loading incoming orders…</div>
          ) : incomingCount === 0 ? (
            <div className="adm-orders__empty" aria-live="polite">
              No new incoming orders right now.
            </div>
          ) : (
            <div className="adm-orders__incoming-grid">
              {incoming.map((order) => (
                <IncomingCard
                  key={order.id}
                  order={order}
                  exiting={exitingIds.has(order.id)}
                  processing={processingIds.has(order.id)}
                  onDecline={handleDecline}
                  onAccept={handleAccept}
                />
              ))}
            </div>
          )}
        </section>

        {/* ── Active orders ── */}
        <section className="adm-orders__section" aria-label="Active orders">
          <div className="adm-orders__section-header">
            <span className="adm-orders__section-dot adm-orders__section-dot--orange" aria-hidden="true"/>
            <h2 className="adm-orders__section-title">Active Orders</h2>
            <span className="adm-orders__section-count adm-orders__section-count--orange">
              {activeCount}
            </span>
          </div>
          {initialLoading ? (
            <div className="adm-orders__empty" aria-live="polite">Loading active orders…</div>
          ) : activeCount === 0 ? (
            <div className="adm-orders__empty" aria-live="polite">
              No active orders. Accept an incoming order to get started.
            </div>
          ) : (
            <div className="adm-orders__active-grid">
              {active.map((order) => (
                <ActiveCard
                  key={order.id}
                  order={order}
                  onStatusChange={handleStatusChange}
                />
              ))}
            </div>
          )}
        </section>

      </div>

      <ToastStack toasts={toasts} />
    </AdminLayout>
  );
};

export default AdminOrdersScreen;
