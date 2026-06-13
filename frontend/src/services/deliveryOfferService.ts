import { getAccessToken } from './authService';

export interface DeliveryOffer {
  orderId: string;
  restaurantId: string;
  restaurantName: string;
  pickupLocation: string;
  deliveryLocation: string;
  estimatedPayout: number;
  expiresIn: number;
}

export interface OfferEvent {
  type: string;
  offer: DeliveryOffer;
}

type OfferListener = (offer: DeliveryOffer) => void;
type ConnectionListener = (connected: boolean) => void;

class DeliveryOfferService {
  private eventSource: EventSource | null = null;
  private driverId: string | null = null;
  private offerListeners: Set<OfferListener> = new Set();
  private connectionListeners: Set<ConnectionListener> = new Set();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000; // ms

  subscribe(listener: OfferListener): () => void {
    this.offerListeners.add(listener);
    return () => this.offerListeners.delete(listener);
  }

  onConnectionChange(listener: ConnectionListener): () => void {
    this.connectionListeners.add(listener);
    return () => this.connectionListeners.delete(listener);
  }

  connect(driverId: string): void {
    if (this.eventSource) {
      console.log('[DeliveryOfferService] Already connected, skipping');
      return;
    }

    this.driverId = driverId;
    this.reconnectAttempts = 0;
    this.attemptConnection();
  }

  private attemptConnection(): void {
    if (!this.driverId) return;

    try {
      const url = `/api/delivery/drivers/${this.driverId}/offers/stream`;

      console.log('[DeliveryOfferService] Attempting SSE connection to', url);

      this.eventSource = new EventSource(url);

      this.eventSource.addEventListener('open', () => {
        console.log('[DeliveryOfferService] SSE connection established');
        this.reconnectAttempts = 0;
        this.notifyConnectionChange(true);
      });

      this.eventSource.addEventListener('DELIVERY_OFFER', (event: any) => {
        console.log('[DeliveryOfferService] Received DELIVERY_OFFER:', event.data);
        try {
          const offer = JSON.parse(event.data);
          this.notifyOfferListeners(offer);
        } catch (err) {
          console.error('[DeliveryOfferService] Failed to parse offer:', err);
        }
      });

      this.eventSource.addEventListener('error', (event: any) => {
        console.error('[DeliveryOfferService] SSE error:', event);
        this.handleConnectionError();
      });

      this.eventSource.onerror = () => {
        console.error('[DeliveryOfferService] SSE connection error');
        this.handleConnectionError();
      };
    } catch (err) {
      console.error('[DeliveryOfferService] Failed to create EventSource:', err);
      this.handleConnectionError();
    }
  }

  private handleConnectionError(): void {
    this.notifyConnectionChange(false);
    this.eventSource?.close();
    this.eventSource = null;

    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      console.log(`[DeliveryOfferService] Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);
      setTimeout(() => this.attemptConnection(), delay);
    } else {
      console.error('[DeliveryOfferService] Max reconnection attempts reached');
    }
  }

  async acceptOffer(orderId: string, driverId: string): Promise<void> {
    const token = getAccessToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`/api/delivery/offers/${orderId}/accept`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ driverId }),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || `HTTP ${response.status}`);
    }

    console.log(`[DeliveryOfferService] Successfully accepted offer: ${orderId}`);
  }

  async declineOffer(orderId: string, driverId: string): Promise<void> {
    const token = getAccessToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`/api/delivery/offers/${orderId}/decline?driverId=${encodeURIComponent(driverId)}`, {
      method: 'POST',
      headers,
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      throw new Error(error.message || `HTTP ${response.status}`);
    }

    console.log(`[DeliveryOfferService] Successfully declined offer: ${orderId}`);
  }

  async sendLocationPing(driverId: string, latitude: number, longitude: number, cityZone: string): Promise<void> {
    const token = getAccessToken();
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch('/api/drivers/location/ping', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          driverId,
          latitude,
          longitude,
          cityZone,
        }),
      });

      if (!response.ok) {
        console.error(`[DeliveryOfferService] Location ping failed: HTTP ${response.status}`);
      }
    } catch (err) {
      console.error('[DeliveryOfferService] Failed to send location ping:', err);
    }
  }

  disconnect(): void {
    if (this.eventSource) {
      console.log('[DeliveryOfferService] Closing SSE connection');
      this.eventSource.close();
      this.eventSource = null;
      this.notifyConnectionChange(false);
    }
  }

  private notifyOfferListeners(offer: DeliveryOffer): void {
    this.offerListeners.forEach(listener => {
      try {
        listener(offer);
      } catch (err) {
        console.error('[DeliveryOfferService] Error in offer listener:', err);
      }
    });
  }

  private notifyConnectionChange(connected: boolean): void {
    this.connectionListeners.forEach(listener => {
      try {
        listener(connected);
      } catch (err) {
        console.error('[DeliveryOfferService] Error in connection listener:', err);
      }
    });
  }
}

export const deliveryOfferService = new DeliveryOfferService();
