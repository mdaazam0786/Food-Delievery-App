/**
 * RestaurantContext.tsx — context definition + provider component only.
 * Hook lives in useRestaurant.ts so Vite Fast Refresh stays happy
 * (a file must export only components OR only non-components, not both).
 */

import React, {
  createContext,
  useEffect,
  useState,
  useCallback,
  type ReactNode,
} from 'react';
import { fetchMyRestaurant } from '../services/restaurantService';
import type { UserProfile } from '../services/userService';

export interface RestaurantContextValue {
  activeRestaurantId:   string | null;
  restaurantStatus:     'OPEN' | 'CLOSED' | null;
  userProfile:          UserProfile | null;
  profileLoading:       boolean;
  noRestaurant:         boolean;
  profileError:         string | null;
  refetch:              () => Promise<void>;
  completeProvisioning: (restaurantId: string, restaurantName?: string) => void;
  setRestaurantStatus:  (status: 'OPEN' | 'CLOSED') => void;
}

export const RestaurantContext = createContext<RestaurantContextValue>({
  activeRestaurantId:   null,
  restaurantStatus:     null,
  userProfile:          null,
  profileLoading:       true,
  noRestaurant:         false,
  profileError:         null,
  refetch:              async () => {},
  completeProvisioning: () => {},
  setRestaurantStatus:  () => {},
});

export const RestaurantProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [activeRestaurantId, setActiveRestaurantId] = useState<string | null>(null);
  const [restaurantStatus,   setRestaurantStatusState] = useState<'OPEN' | 'CLOSED' | null>(null);
  const [userProfile,        setUserProfile]        = useState<UserProfile | null>(null);
  const [profileLoading,     setProfileLoading]     = useState(true);
  const [noRestaurant,       setNoRestaurant]       = useState(false);
  const [profileError,       setProfileError]       = useState<string | null>(null);

  const applyRestaurantState = useCallback((
    profile: UserProfile | null,
    mine: Awaited<ReturnType<typeof fetchMyRestaurant>>,
  ) => {
    // Simple logic: if restaurant exists via API, show dashboard; otherwise show provisioning
    if (mine) {
      setActiveRestaurantId(mine.id);
      setRestaurantStatusState((mine.status as 'OPEN' | 'CLOSED') ?? 'OPEN');
      setNoRestaurant(false);
      setUserProfile(profile || null);
      return;
    }

    // No restaurant found
    setActiveRestaurantId(null);
    setRestaurantStatusState(null);
    setNoRestaurant(true);
  }, []);

  const loadProfile = useCallback(async () => {
    setProfileLoading(true);
    setProfileError(null);

    try {
      // Call API to get restaurant by owner email (admin email from JWT)
      const mine = await fetchMyRestaurant().catch(() => null);
      applyRestaurantState(null, mine);
    } catch (err: unknown) {
      setProfileError(
        err instanceof Error ? err.message : 'Failed to load restaurant profile.',
      );
    } finally {
      setProfileLoading(false);
    }
  }, [applyRestaurantState]);

  const completeProvisioning = useCallback(() => {
    // After provisioning, refetch to verify the restaurant was created
    loadProfile();
  }, [loadProfile]);

  const setRestaurantStatus = useCallback((status: 'OPEN' | 'CLOSED') => {
    setRestaurantStatusState(status);
  }, []);

  useEffect(() => {
    loadProfile();
  }, []);

  return (
    <RestaurantContext.Provider
      value={{
        activeRestaurantId,
        restaurantStatus,
        userProfile,
        profileLoading,
        noRestaurant,
        profileError,
        refetch: loadProfile,
        completeProvisioning,
        setRestaurantStatus,
      }}
    >
      {children}
    </RestaurantContext.Provider>
  );
};
