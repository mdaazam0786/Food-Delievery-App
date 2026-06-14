/**
 * searchService.ts — public discovery endpoints via API Gateway.
 *
 * GET /api/search/nearby?lat=&lon=&radius=
 * GET /api/search?q=&lat=&lon=
 */

import type { SearchRestaurant } from '../utils/geoUtils';

const BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '';

export async function fetchNearbyRestaurants(
  lat: number,
  lon: number,
  radiusKm = 5,
  size = 50,
): Promise<SearchRestaurant[]> {
  const params = new URLSearchParams({
    lat:    String(lat),
    lon:    String(lon),
    radius: String(radiusKm),
    size:   String(size),
  });

  try {
    const res = await fetch(`${BASE_URL}/api/search/nearby?${params}`);
    const json = await res.json();
    
    console.log('[fetchNearbyRestaurants] Response:', json);
    
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    
    // Handle both direct results array and wrapped response
    if (json.success === false) {
      throw new Error(json.message ?? 'Failed to load nearby restaurants');
    }
    
    // Extract results from data object or use results directly
    const results = json.data?.results || json.results || [];
    console.log('[fetchNearbyRestaurants] Extracted results:', results);
    
    return results;
  } catch (err) {
    console.error('[fetchNearbyRestaurants] Error:', err);
    throw err;
  }
}

export async function searchRestaurants(
  query: string,
  lat: number,
  lon: number,
  size = 50,
): Promise<SearchRestaurant[]> {
  const params = new URLSearchParams({
    q:    query,
    lat:  String(lat),
    lon:  String(lon),
    size: String(size),
  });

  try {
    const res = await fetch(`${BASE_URL}/api/search?${params}`);
    const json = await res.json();
    
    console.log('[searchRestaurants] Response:', json);
    console.log('[searchRestaurants] Query:', query);
    
    if (!res.ok) {
      throw new Error(`HTTP ${res.status}`);
    }
    
    if (json.success === false) {
      throw new Error(json.message ?? 'Failed to search restaurants');
    }
    
    // Extract results from data object or use results directly
    const results = json.data?.results || json.results || [];
    console.log('[searchRestaurants] Extracted results count:', results.length);
    console.log('[searchRestaurants] Results:', results);
    
    return results;
  } catch (err) {
    console.error('[searchRestaurants] Error:', err);
    throw err;
  }
}
