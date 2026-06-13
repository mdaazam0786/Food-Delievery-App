import type { RestaurantOrder } from '../services/orderService';

export type ActiveStatus = 'preparing' | 'ready' | 'delivery';

export interface IncomingOrder {
  id:       string;
  orderId:  string;
  rawId:    string;
  customer: string;
  items:    string;
  total:    string;
  elapsed:  string;
}

export interface ActiveOrder extends IncomingOrder {
  status:   ActiveStatus;
  synced:   boolean;
  updating: boolean;
}

export function formatOrderLabel(id: string): string {
  const short = id.length > 4 ? id.slice(-4) : id;
  return `#ORD-${short.toUpperCase()}`;
}

export function formatItemsSummary(items: RestaurantOrder['items']): string {
  if (!items?.length) return '—';
  return items.map((i) => `${i.quantity}x ${i.itemName}`).join(', ');
}

export function formatTotal(amount: number, currency = 'USD'): string {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(amount);
}

export function formatElapsed(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return 'Just now';
  if (mins === 1) return '1 min ago';
  return `${mins} mins ago`;
}

function emailToCustomer(email: string): string {
  const local = email.split('@')[0] ?? email;
  return local
    .replace(/[._]/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase());
}

export function toIncomingOrder(o: RestaurantOrder): IncomingOrder {
  return {
    id:       o.id,
    orderId:  formatOrderLabel(o.id),
    rawId:    o.id,
    customer: emailToCustomer(o.userEmail),
    items:    formatItemsSummary(o.items),
    total:    formatTotal(Number(o.totalAmount), o.currency ?? 'USD'),
    elapsed:  formatElapsed(o.createdAt),
  };
}

export function backendStatusToActive(status: string): ActiveStatus | null {
  switch (status) {
    case 'ACCEPTED':           return 'preparing';
    case 'READY_FOR_PICKUP':   return 'ready';
    case 'OUT_FOR_DELIVERY':   return 'delivery';
    default:                   return null;
  }
}

export function activeStatusToBackend(status: ActiveStatus): string {
  const map: Record<ActiveStatus, string> = {
    preparing: 'ACCEPTED',
    ready:     'READY_FOR_PICKUP',
    delivery:  'OUT_FOR_DELIVERY',
  };
  return map[status];
}

export function toActiveOrder(o: RestaurantOrder): ActiveOrder | null {
  const status = backendStatusToActive(o.status);
  if (!status) return null;
  return { ...toIncomingOrder(o), status, synced: false, updating: false };
}

export function splitRestaurantOrders(orders: RestaurantOrder[]): {
  incoming: IncomingOrder[];
  active:   ActiveOrder[];
} {
  const incoming: IncomingOrder[] = [];
  const active: ActiveOrder[] = [];

  for (const o of orders) {
    if (o.status === 'PREPARING') {
      incoming.push(toIncomingOrder(o));
    } else {
      const activeOrder = toActiveOrder(o);
      if (activeOrder) active.push(activeOrder);
    }
  }

  return { incoming, active };
}
