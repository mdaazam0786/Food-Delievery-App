/** Earth radius in kilometres */
const R = 6371;

/** Haversine distance between two WGS-84 coordinates (km). */
export function haversineKm(
  lat1: number,
  lon1: number,
  lat2: number,
  lon2: number,
): number {
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

/** Rough delivery ETA from distance (3 min/km + 8 min base). */
export function estimateDeliveryMinutes(distanceKm: number): number {
  return Math.max(12, Math.round(8 + distanceKm * 3.2));
}

/** Stable pseudo-rating for display when the search index has no rating field yet. */
export function deriveRating(seed: string): number {
  let hash = 0;
  for (let i = 0; i < seed.length; i++) {
    hash = (hash << 5) - hash + seed.charCodeAt(i);
    hash |= 0;
  }
  return Math.round((3.5 + (Math.abs(hash) % 14) / 10) * 10) / 10;
}

export interface GeoCoords {
  lat: number;
  lng: number;
}

export interface ProcessedRestaurant {
  id:           string;
  name:         string;
  imageUrl?:    string;
  tags:         string;
  rating?:      number;   // Now populated from backend
  totalRatings?: number;  // Now populated from backend
  distanceKm:   number;
  deliveryMins: number;
  discount?:    string;   // Now populated from backend
}

export interface SearchRestaurant {
  restaurantId: string;
  name:         string;
  imageUrl?:    string;
  latitude:     number;
  longitude:    number;
  distanceKm?:  number | null;
  rating?:      number;
  totalRatings?: number;
  discount?:    string;
  status?:      string;
  menuItems?:   { name: string; category?: string; isVeg?: boolean; rating?: number; totalRatings?: number }[];
}

/** Filter ≤ maxKm, sort by distance then rating within ~300 m brackets. 
 * Also prioritizes restaurants that have matching menu items to the search query.
 */
export function filterAndSortRestaurants(
  items: SearchRestaurant[],
  userLat: number,
  userLng: number,
  maxKm = 5,
  searchQuery?: string,
): ProcessedRestaurant[] {
  console.log('[filterAndSortRestaurants] Input:', { itemsCount: items.length, maxKm, searchQuery });
  
  const withDistance = items.map((r) => {
    const distanceKm =
      r.distanceKm ??
      haversineKm(userLat, userLng, r.latitude, r.longitude);
    
    // Extract unique categories and item names from menuItems
    const itemNames = r.menuItems?.map((m) => m.name) || [];
    const categoryNames = Array.from(new Set(r.menuItems?.map((m) => m.category).filter(Boolean) || []));
    
    // Combine item names and categories for tags (show item names first, then categories)
    const tags = [
      ...itemNames.slice(0, 3),
      ...categoryNames.slice(0, 1),
    ]
      .slice(0, 4)
      .join(' · ') || 'Popular picks nearby';

    // Check if restaurant has the searched item
    let hasSearchedItem = false;
    if (searchQuery) {
      const query = searchQuery.toLowerCase();
      hasSearchedItem = 
        r.menuItems?.some((m) => 
          m.name.toLowerCase().includes(query) || 
          m.category?.toLowerCase().includes(query)
        ) ?? false;
    }

    return {
      id:           r.restaurantId,
      name:         r.name,
      imageUrl:     r.imageUrl,
      tags,
      rating:       r.rating && r.rating > 0 ? r.rating : undefined,
      totalRatings: r.totalRatings,
      distanceKm,
      deliveryMins: estimateDeliveryMinutes(distanceKm),
      discount:     r.discount,
      hasSearchedItem, // Add flag for sorting
    };
  });

  console.log('[filterAndSortRestaurants] withDistance:', withDistance);

  const filtered = withDistance.filter((r) => r.distanceKm <= maxKm);
  console.log('[filterAndSortRestaurants] Filtered by distance:', { count: filtered.length, maxKm });

  const sorted = filtered
    .sort((a, b) => {
      // If we have a search query, prioritize restaurants with matching items
      if (searchQuery) {
        if (a.hasSearchedItem !== b.hasSearchedItem) {
          return a.hasSearchedItem ? -1 : 1; // hasSearchedItem comes first
        }
      }
      
      // Then sort by distance and rating
      const bracketA = Math.floor(a.distanceKm / 0.3);
      const bracketB = Math.floor(b.distanceKm / 0.3);
      if (bracketA !== bracketB) return a.distanceKm - b.distanceKm;
      if (a.distanceKm !== b.distanceKm) return a.distanceKm - b.distanceKm;
      return (b.rating ?? 0) - (a.rating ?? 0);
    })
    .map(({ hasSearchedItem, ...r }) => r); // Remove the temporary flag before returning

  console.log('[filterAndSortRestaurants] Final sorted results:', sorted);

  return sorted;
}

/** Reverse geocode using backend proxy endpoint (via /api/geocode). */
export async function reverseGeocode(lat: number, lon: number): Promise<string> {
  const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';
  
  try {
    const res = await fetch(`${BASE_URL}/api/geocode?lat=${lat}&lng=${lon}`);
    if (!res.ok) throw new Error('Geocode failed');
    
    const data = (await res.json()) as {
      displayName?: string;
      address?: {
        city?: string;
        state?: string;
        country?: string;
      };
    };
    
    // Return the displayName if available, otherwise construct from address components
    if (data.displayName && data.displayName.trim()) {
      return data.displayName;
    }
    
    if (data.address) {
      const parts = [data.address.city, data.address.state].filter(Boolean);
      if (parts.length) return parts.join(', ');
    }
    
    return 'Your location';
  } catch (err) {
    // Graceful fallback to coordinates if backend fails
    return `${lat.toFixed(4)}, ${lon.toFixed(4)}`;
  }
}
