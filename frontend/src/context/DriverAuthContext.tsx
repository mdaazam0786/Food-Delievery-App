import React, { createContext, useState, useCallback, ReactNode, useEffect } from 'react';
import { getAccessToken } from '../services/authService';

// ─── Types ────────────────────────────────────────────────────────────────────

export interface DriverProfile {
  id: string;
  fullName: string;
  phoneNumber: string;
  email: string;
  vehicleType: string;
  kycStatus: 'PENDING' | 'APPROVED' | 'REJECTED';
  currentStatus: 'OFFLINE' | 'IDLE' | 'DELIVERING';
  cityZone: string;
  createdAt: string;
}

export interface DriverAuthContextValue {
  driverId: string | null;
  driverProfile: DriverProfile | null;
  isRegistered: boolean;
  isLoading: boolean;
  error: string | null;
  fetchDriverProfile: (driverId: string) => Promise<void>;
  setDriverProfile: (profile: DriverProfile | null) => void;
  setIsRegistered: (registered: boolean) => void;
  clearError: () => void;
}

// ─── Context ──────────────────────────────────────────────────────────────────

const DriverAuthContext = createContext<DriverAuthContextValue | undefined>(undefined);

// ─── Provider ─────────────────────────────────────────────────────────────────

interface DriverAuthProviderProps {
  children: ReactNode;
}

export const DriverAuthProvider: React.FC<DriverAuthProviderProps> = ({ children }) => {
  const [driverId, setDriverId] = useState<string | null>(null);
  const [driverProfile, setDriverProfile] = useState<DriverProfile | null>(null);
  const [isRegistered, setIsRegistered] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Fetch driver profile from backend using email
  const fetchDriverProfile = useCallback(async (email: string) => {
    setIsLoading(true);
    setError(null);

    try {
      console.log(`[DriverAuth] Fetching profile for email: ${email}`);
      
      const token = getAccessToken();
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
        headers['X-User-Email'] = email;
      }

      const response = await fetch(`/api/drivers/profile`, {
        method: 'GET',
        headers,
      });

      console.log(`[DriverAuth] Response status: ${response.status}`);

      // Handle 404 - driver not found/not registered
      if (response.status === 404) {
        console.log('[DriverAuth] Driver not found - showing registration form');
        setIsRegistered(false);
        setDriverProfile(null);
        setError(null);
        return;
      }

      // Handle other errors
      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP ${response.status}`);
      }

      // Parse successful response
      const data = await response.json();
      console.log('[DriverAuth] Driver profile received:', data);
      
      // Extract driver data from the ApiResponse wrapper
      const driver = data.data || data;

      if (driver && driver.id) {
        console.log('[DriverAuth] Driver registered, updating context');
        setDriverProfile(driver);
        setIsRegistered(true);
        setDriverId(driver.id);
        setError(null);
      } else {
        // Empty or null response
        console.log('[DriverAuth] Empty response - showing registration form');
        setIsRegistered(false);
        setDriverProfile(null);
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      console.error('[DriverAuth] Error fetching profile:', errorMessage);
      setError(errorMessage);
      setIsRegistered(false);
      setDriverProfile(null);
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Clear error
  const clearError = useCallback(() => {
    setError(null);
  }, []);

  // Update profile
  const updateDriverProfile = useCallback((profile: DriverProfile | null) => {
    setDriverProfile(profile);
    if (profile) {
      setIsRegistered(true);
      setDriverId(profile.id);
    }
  }, []);

  // Update registration status
  const updateIsRegistered = useCallback((registered: boolean) => {
    setIsRegistered(registered);
  }, []);

  const value: DriverAuthContextValue = {
    driverId,
    driverProfile,
    isRegistered,
    isLoading,
    error,
    fetchDriverProfile,
    setDriverProfile: updateDriverProfile,
    setIsRegistered: updateIsRegistered,
    clearError,
  };

  return (
    <DriverAuthContext.Provider value={value}>
      {children}
    </DriverAuthContext.Provider>
  );
};

// ─── Hook ─────────────────────────────────────────────────────────────────────

export const useDriverAuth = (): DriverAuthContextValue => {
  const context = React.useContext(DriverAuthContext);
  if (!context) {
    throw new Error('useDriverAuth must be used within a DriverAuthProvider');
  }
  return context;
};
