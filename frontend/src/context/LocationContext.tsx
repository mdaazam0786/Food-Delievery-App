import React, {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { reverseGeocode, type GeoCoords } from '../utils/geoUtils';

interface LocationContextValue {
  coords:    GeoCoords | null;
  address:   string;
  loading:   boolean;
  error:     string | null;
  refresh:   () => void;
}

const LocationContext = createContext<LocationContextValue>({
  coords:  null,
  address: 'Locating…',
  loading: true,
  error:   null,
  refresh: () => {},
});

export const LocationProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [coords,  setCoords]  = useState<GeoCoords | null>(null);
  const [address, setAddress] = useState('Locating…');
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState<string | null>(null);
  const [tick,    setTick]    = useState(0);

  const FALLBACK_ADDRESS = 'Delhi, India';

  useEffect(() => {
    if (!('geolocation' in navigator)) {
      setError('Geolocation is not supported by this browser.');
      setAddress(FALLBACK_ADDRESS);
      setLoading(false);
      return;
    }

    setLoading(true);
    setError(null);

    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        setCoords({ lat, lng });
        try {
          const label = await reverseGeocode(lat, lng);
          setAddress(label || FALLBACK_ADDRESS);
        } catch {
          // If backend geocoding fails, use coordinate display, then fallback to city
          try {
            setAddress(`${lat.toFixed(4)}, ${lng.toFixed(4)}`);
          } catch {
            setAddress(FALLBACK_ADDRESS);
          }
        } finally {
          setLoading(false);
        }
      },
      (err) => {
        // User denied location or error occurred
        setError(err.message);
        setAddress(FALLBACK_ADDRESS);
        setLoading(false);
      },
      { enableHighAccuracy: true, timeout: 12_000, maximumAge: 60_000 },
    );
  }, [tick]);

  const refresh = () => setTick((t) => t + 1);

  return (
    <LocationContext.Provider value={{ coords, address, loading, error, refresh }}>
      {children}
    </LocationContext.Provider>
  );
};

export function useLocation(): LocationContextValue {
  return useContext(LocationContext);
}
