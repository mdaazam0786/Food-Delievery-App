/**
 * useRestaurant.ts — hook-only file so Vite Fast Refresh doesn't complain
 * about mixing component and non-component exports in one file.
 */

import { useContext } from 'react';
import { RestaurantContext, type RestaurantContextValue } from './RestaurantContext';

export function useRestaurant(): RestaurantContextValue {
  return useContext(RestaurantContext);
}
