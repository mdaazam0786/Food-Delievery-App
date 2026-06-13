import React, { createContext, useState, useCallback, useEffect, ReactNode } from 'react';
import { DeliveryOffer, deliveryOfferService } from '../services/deliveryOfferService';

export interface OfferWithTimer extends DeliveryOffer {
  timeLeft: number; // seconds remaining
}

export interface OfferContextValue {
  offers: OfferWithTimer[];
  isConnected: boolean;
  isConnecting: boolean;
  addOffer: (offer: DeliveryOffer) => void;
  removeOffer: (orderId: string) => void;
  acceptOffer: (orderId: string, driverId: string) => Promise<void>;
  declineOffer: (orderId: string, driverId: string) => Promise<void>;
  connectToOffers: (driverId: string) => void;
  disconnectFromOffers: () => void;
}

const OfferContext = createContext<OfferContextValue | undefined>(undefined);

interface OfferProviderProps {
  children: ReactNode;
}

export const OfferProvider: React.FC<OfferProviderProps> = ({ children }) => {
  const [offers, setOffers] = useState<OfferWithTimer[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [driverId, setDriverId] = useState<string | null>(null);

  // Handle incoming offers from service
  const handleNewOffer = useCallback((offer: DeliveryOffer) => {
    console.log('[OfferContext] New offer received:', offer.orderId);
    setOffers(prev => [
      ...prev,
      { ...offer, timeLeft: offer.expiresIn }
    ]);
  }, []);

  // Handle connection state change
  const handleConnectionChange = useCallback((connected: boolean) => {
    console.log('[OfferContext] Connection state changed:', connected);
    setIsConnected(connected);
    setIsConnecting(false);
  }, []);

  // Subscribe to offer service on mount
  useEffect(() => {
    const unsubscribeOffer = deliveryOfferService.subscribe(handleNewOffer);
    const unsubscribeConnection = deliveryOfferService.onConnectionChange(handleConnectionChange);

    return () => {
      unsubscribeOffer();
      unsubscribeConnection();
    };
  }, [handleNewOffer, handleConnectionChange]);

  // Timer for countdown
  useEffect(() => {
    if (offers.length === 0) return;

    const interval = setInterval(() => {
      setOffers(prev => {
        const updated = prev
          .map(offer => ({
            ...offer,
            timeLeft: Math.max(0, offer.timeLeft - 1)
          }))
          .filter(offer => offer.timeLeft > 0);

        return updated;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [offers.length]);

  const addOffer = useCallback((offer: DeliveryOffer) => {
    setOffers(prev => [...prev, { ...offer, timeLeft: offer.expiresIn }]);
  }, []);

  const removeOffer = useCallback((orderId: string) => {
    setOffers(prev => prev.filter(o => o.orderId !== orderId));
  }, []);

  const acceptOffer = useCallback(async (orderId: string, driverId: string) => {
    try {
      await deliveryOfferService.acceptOffer(orderId, driverId);
      removeOffer(orderId);
    } catch (err) {
      console.error('[OfferContext] Failed to accept offer:', err);
      throw err;
    }
  }, [removeOffer]);

  const declineOffer = useCallback(async (orderId: string, driverId: string) => {
    try {
      await deliveryOfferService.declineOffer(orderId, driverId);
      removeOffer(orderId);
    } catch (err) {
      console.error('[OfferContext] Failed to decline offer:', err);
      throw err;
    }
  }, [removeOffer]);

  const connectToOffers = useCallback((id: string) => {
    if (driverId === id && isConnected) {
      console.log('[OfferContext] Already connected for driver:', id);
      return;
    }

    console.log('[OfferContext] Connecting to offers for driver:', id);
    setDriverId(id);
    setIsConnecting(true);
    deliveryOfferService.connect(id);
  }, [driverId, isConnected]);

  const disconnectFromOffers = useCallback(() => {
    console.log('[OfferContext] Disconnecting from offers');
    deliveryOfferService.disconnect();
    setOffers([]);
    setIsConnected(false);
    setDriverId(null);
  }, []);

  const value: OfferContextValue = {
    offers,
    isConnected,
    isConnecting,
    addOffer,
    removeOffer,
    acceptOffer,
    declineOffer,
    connectToOffers,
    disconnectFromOffers,
  };

  return (
    <OfferContext.Provider value={value}>
      {children}
    </OfferContext.Provider>
  );
};

export const useOffers = (): OfferContextValue => {
  const context = React.useContext(OfferContext);
  if (!context) {
    throw new Error('useOffers must be used within an OfferProvider');
  }
  return context;
};
