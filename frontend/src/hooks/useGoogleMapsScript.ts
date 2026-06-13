/**
 * Loads the Google Maps JavaScript API via the official callback pattern.
 * Set VITE_GOOGLE_MAPS_API_KEY in frontend/.env.local
 */

import { useEffect, useState } from 'react';

const SCRIPT_ID = 'google-maps-js';
const CALLBACK  = '__foodzieMapsInit';

let loadPromise: Promise<void> | null = null;

export function getGoogleMapsApiKey(): string | undefined {
  const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined;
  return key?.trim() || undefined;
}

function mapsReady(): boolean {
  return typeof window !== 'undefined' && typeof window.google?.maps?.Map === 'function';
}

function loadGoogleMapsScript(apiKey: string): Promise<void> {
  if (mapsReady()) return Promise.resolve();
  if (loadPromise) return loadPromise;

  loadPromise = new Promise((resolve, reject) => {
    const finish = () => {
      if (mapsReady()) resolve();
      else reject(new Error('Google Maps loaded but Map constructor is unavailable.'));
    };

    const timeout = window.setTimeout(() => {
      reject(new Error('Google Maps timed out. Check API key, billing, and Maps JavaScript API enablement.'));
    }, 20_000);

    const done = () => {
      window.clearTimeout(timeout);
      finish();
    };

    const fail = (message: string) => {
      window.clearTimeout(timeout);
      loadPromise = null;
      reject(new Error(message));
    };

    if (mapsReady()) {
      window.clearTimeout(timeout);
      resolve();
      return;
    }

    window[CALLBACK] = () => {
      delete window[CALLBACK];
      done();
    };

    const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null;
    if (existing) {
      if (mapsReady()) {
        done();
        return;
      }
      existing.addEventListener('load', done, { once: true });
      existing.addEventListener('error', () => fail('Google Maps script failed to load.'), { once: true });
      return;
    }

    const script = document.createElement('script');
    script.id = SCRIPT_ID;
    script.src =
      `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&callback=${CALLBACK}&libraries=places`;
    script.async = true;
    script.defer = true;
    script.onerror = () => fail('Google Maps script failed to load.');
    document.head.appendChild(script);
  });

  return loadPromise;
}

export function useGoogleMapsScript(): { ready: boolean; error: string | null; missingKey: boolean } {
  const apiKey = getGoogleMapsApiKey();
  const [ready, setReady] = useState(() => mapsReady());
  const [error, setError] = useState<string | null>(null);
  const missingKey = !apiKey;

  useEffect(() => {
    if (!apiKey) {
      setReady(false);
      setError(null);
      return;
    }

    if (mapsReady()) {
      setReady(true);
      return;
    }

    loadGoogleMapsScript(apiKey)
      .then(() => {
        setReady(true);
        setError(null);
      })
      .catch((err: Error) => {
        setReady(false);
        setError(err.message);
      });
  }, [apiKey]);

  return { ready, error, missingKey };
}

/** Default map centre (New Delhi) when geolocation is denied. */
export const DEFAULT_MAP_CENTER = { lat: 28.6139, lng: 77.2090 };

export async function resolveMapStartCenter(): Promise<{ lat: number; lng: number }> {
  if (!('geolocation' in navigator)) return DEFAULT_MAP_CENTER;

  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (pos) => resolve({ lat: pos.coords.latitude, lng: pos.coords.longitude }),
      () => resolve(DEFAULT_MAP_CENTER),
      { enableHighAccuracy: false, timeout: 5_000, maximumAge: 120_000 },
    );
  });
}

declare global {
  interface Window {
    __foodzieMapsInit?: () => void;
  }
}

export {};
