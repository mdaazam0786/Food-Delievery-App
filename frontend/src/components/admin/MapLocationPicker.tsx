import React, { useEffect, useRef, useState } from 'react';
import {
  DEFAULT_MAP_CENTER,
  resolveMapStartCenter,
  useGoogleMapsScript,
} from '../../hooks/useGoogleMapsScript';
import { reverseGeocodeBackend } from '../../services/restaurantService';
import './MapLocationPicker.css';

export interface MapLocationSelection {
  addressText: string;
  latitude:    number;
  longitude:   number;
}

interface MapLocationPickerProps {
  onConfirm: (selection: MapLocationSelection) => void;
  onChange?: (selection: MapLocationSelection) => void;
  onLockChange?: (locked: boolean) => void;
  disabled?: boolean;
}

const MapLocationPicker: React.FC<MapLocationPickerProps> = ({ onConfirm, onChange, onLockChange, disabled = false }) => {
  const mapContainerRef = useRef<HTMLDivElement>(null);
  const mapRef          = useRef<google.maps.Map | null>(null);
  const markerRef       = useRef<google.maps.Marker | null>(null);
  const infoWindowRef   = useRef<google.maps.InfoWindow | null>(null);
  const listenersRef    = useRef<any[]>([]); // Ref to safely clean up native maps event instances
  const onChangeRef       = useRef<MapLocationPickerProps['onChange']>(onChange);
  const mountedRef        = useRef(true);
  const geocodeSeqRef     = useRef(0);
  const skipNextGeocodeRef = useRef(false);
  const userMovedPinRef   = useRef(false);
  const userEditedAddressRef = useRef(false);
  const disabledRef       = useRef(disabled);
  const confirmedRef      = useRef(false);
  const didInitRef        = useRef(false);

  const { ready, error, missingKey } = useGoogleMapsScript();

  const [initializing, setInitializing] = useState(true);
  const [initError,    setInitError]    = useState<string | null>(null);
  const [confirming,   setConfirming]   = useState(false);
  const [pin,          setPin]          = useState<{ lat: number; lng: number } | null>(null);
  const [confirmError, setConfirmError] = useState<string | null>(null);
  const [resolvingAddress, setResolvingAddress] = useState(false);
  const [addressText, setAddressText] = useState<string>('');
  const [confirmed, setConfirmed] = useState(false);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  useEffect(() => {
    onChangeRef.current = onChange;
  }, [onChange]);

  useEffect(() => {
    disabledRef.current = disabled;
  }, [disabled]);

  useEffect(() => {
    confirmedRef.current = confirmed;
  }, [confirmed]);

  const applyPinLocation = (lat: number, lng: number, opts?: { label?: string }) => {
    userMovedPinRef.current = true;
    userEditedAddressRef.current = false;
    setPin({ lat, lng });
    markerRef.current?.setPosition({ lat, lng });
    mapRef.current?.panTo({ lat, lng });

    if (opts?.label) {
      skipNextGeocodeRef.current = true;
      geocodeSeqRef.current += 1;
      setAddressText(opts.label);
      onChangeRef.current?.({ addressText: opts.label, latitude: lat, longitude: lng });
      setResolvingAddress(false);
    } else {
      setAddressText('');
      setResolvingAddress(true);
      infoWindowRef.current?.setContent(
        '<div class="map-picker__infowindow" style="color:#111; font-weight:500; padding:4px;">Resolving address…</div>',
      );
      if (mapRef.current && markerRef.current) {
        infoWindowRef.current?.open({ map: mapRef.current, anchor: markerRef.current });
      }
    }
  };

  const reverseGeocodeWithBackend = async (lat: number, lng: number): Promise<string> => {
    const result = await reverseGeocodeBackend(lat, lng);
    if (!result.displayName?.trim()) throw new Error('Empty display_name from backend geocode.');
    return result.displayName.trim();
  };

  const resolveAddressLabel = async (lat: number, lng: number): Promise<string> => {
    return await reverseGeocodeWithBackend(lat, lng);
  };

  // High-precision label like the POI cards: use Places near the dropped pin.
  const resolvePlaceFormattedAddressFromCoords = async (lat: number, lng: number): Promise<string | null> => {
    if (!mapRef.current) throw new Error('Map is not ready.');
    if (!google.maps?.places?.PlacesService) throw new Error('PlacesService unavailable.');

    const svc = new google.maps.places.PlacesService(mapRef.current);
    const nearby = await new Promise<google.maps.places.PlaceResult[] | null>((resolve, reject) => {
      try {
        svc.nearbySearch(
          { location: { lat, lng }, radius: 75 },
          (results, status) => {
            if (status && status !== 'OK') {
              reject(new Error(status));
              return;
            }
            resolve(results ?? []);
          },
        );
      } catch (e) {
        reject(e);
      }
    });

    const placeId = nearby?.[0]?.place_id;
    if (!placeId) return null;

    const place = await new Promise<google.maps.places.PlaceResult>((resolve, reject) => {
      svc.getDetails(
        { placeId, fields: ['name', 'formatted_address', 'geometry'] },
        (p, status) => {
          if (status && status !== 'OK') {
            reject(new Error(status));
            return;
          }
          if (!p) {
            reject(new Error('No place details.'));
            return;
          }
          resolve(p);
        },
      );
    });

    const addr = place.formatted_address?.trim();
    if (addr) return addr;

    // Fallback: build a label from name+formatted_address if formatted_address alone isn't present.
    const name = place.name?.trim();
    if (name) return name;
    return null;
  };

  const placeResolvedSeqRef = useRef<number>(0);

  useEffect(() => {
    // Strictly instantiate the Google Map once, after the script is ready.
    if (!ready) return;
    if (!mapContainerRef.current) return;
    if (disabledRef.current) return;
    if (mapRef.current) return;
    if (didInitRef.current) return;

    didInitRef.current = true;

    let cancelled = false;
    setInitializing(true);
    setInitError(null);

    try {
      const map = new google.maps.Map(mapContainerRef.current, {
        center:            DEFAULT_MAP_CENTER,
        zoom:              16, // Zoomed in closer for precise city-pin drops
        mapTypeControl:    false,
        streetViewControl: false,
        fullscreenControl: true,
      });

      const marker = new google.maps.Marker({
        map,
        position:  DEFAULT_MAP_CENTER,
        draggable: !confirmed,
        title:     'Drag to set restaurant location',
      });

      const infoWindow = new google.maps.InfoWindow({
        content: '<div class="map-picker__infowindow" style="color:#111; font-weight:500; padding:4px;">Resolving Target Location...</div>',
        disableAutoPan: true,
      });

      mapRef.current = map;
      markerRef.current = marker;
      infoWindowRef.current = infoWindow;
      setPin(DEFAULT_MAP_CENTER);

      // Open initial infoWindow on anchor setup
      infoWindow.open({ map, anchor: marker });

      const listeners = [
        map.addListener('click', (e: google.maps.MapMouseEvent) => {
          if (disabledRef.current || confirmedRef.current) return;

          const lat = e.latLng?.lat();
          const lng = e.latLng?.lng();
          if (lat == null || lng == null) return;

          applyPinLocation(lat, lng);
        }),
        marker.addListener('dragend', () => {
          if (disabledRef.current || confirmedRef.current) return;
          const pos = marker.getPosition();
          if (!pos) return;
          applyPinLocation(pos.lat(), pos.lng());
        }),
        marker.addListener('click', () => {
          if (!mapRef.current || !markerRef.current) return;
          infoWindowRef.current?.open({ map: mapRef.current, anchor: markerRef.current });
        }),
      ];
      
      listenersRef.current = listeners;
      setInitializing(false);

      resolveMapStartCenter().then((start) => {
        if (cancelled || userMovedPinRef.current) return;
        const targetStart = { lat: start.lat, lng: start.lng };
        setPin(targetStart);
        markerRef.current?.setPosition(targetStart);
        mapRef.current?.panTo(targetStart);
      });

    } catch (err: unknown) {
      // Allow retry if init failed before refs were populated.
      didInitRef.current = false;
      setInitError(err instanceof Error ? err.message : 'Could not initialize the map.');
      setInitializing(false);
    }

    // COMPONENT CLEANUP: Destroys active event hooks safely on lifecycle unmount events
    return () => {
      cancelled = true;
      if (listenersRef.current.length > 0) {
        listenersRef.current.forEach((listener) => {
          if (typeof google !== 'undefined' && google.maps?.event) {
            google.maps.event.removeListener(listener);
          }
        });
        listenersRef.current = [];
      }
    };
  }, [ready]);

  // Sync marker configuration states when location lock changes
  useEffect(() => {
    if (markerRef.current) {
      markerRef.current.setDraggable(!disabled && !confirmed);
    }
  }, [confirmed, disabled]);

  // Debounced execution logic handling location mutations safely
  useEffect(() => {
    if (!ready || !pin || confirmed) return;

    if (skipNextGeocodeRef.current) {
      skipNextGeocodeRef.current = false;
      return;
    }

    const seq = ++geocodeSeqRef.current;
    const { lat, lng } = pin;

    const handle = window.setTimeout(() => {
      setResolvingAddress(true);
      setConfirmError(null);

      // Prefer Places formatted_address (high precision, POI card-like),
      // but also resolve a reverse-geocode fallback for responsiveness.
      placeResolvedSeqRef.current = 0;

      const stopLoading = () => {
        if (!mountedRef.current) return;
        if (seq !== geocodeSeqRef.current) return;
        setResolvingAddress(false);
      };

      resolveAddressLabel(lat, lng)
        .then((label) => {
          if (!mountedRef.current) return;
          if (seq !== geocodeSeqRef.current) return;
          // If the admin started typing, never overwrite their edits.
          if (userEditedAddressRef.current) {
            stopLoading();
            return;
          }
          // If Places already resolved for this seq, don't overwrite it.
          if (placeResolvedSeqRef.current === seq) {
            stopLoading();
            return;
          }
          setAddressText(label);
          onChangeRef.current?.({ addressText: label, latitude: lat, longitude: lng });
          stopLoading();
        })
        .catch(() => {
          if (!mountedRef.current) return;
          if (seq !== geocodeSeqRef.current) return;
          if (userEditedAddressRef.current) {
            stopLoading();
            return;
          }
          // If both Places and reverse-geocode fail, avoid leaving UI empty forever.
          const fallback = 'Unknown location';
          if (placeResolvedSeqRef.current === seq) {
            stopLoading();
            return;
          }
          setAddressText(fallback);
          onChangeRef.current?.({ addressText: fallback, latitude: lat, longitude: lng });
          stopLoading();
        });

      resolvePlaceFormattedAddressFromCoords(lat, lng)
        .then((addr) => {
          if (!mountedRef.current) return;
          if (seq !== geocodeSeqRef.current) return;
          if (!addr) return;
          if (userEditedAddressRef.current) {
            stopLoading();
            return;
          }
          placeResolvedSeqRef.current = seq;
          setAddressText(addr);
          onChangeRef.current?.({ addressText: addr, latitude: lat, longitude: lng });
          stopLoading();
        })
        .catch(() => {
          // Places can fail (no place nearby / quota / permissions). Fallback will handle UI.
        });
    }, 380); // < 400ms UI update target, still debounced to avoid hammering APIs

    return () => {
      window.clearTimeout(handle);
    };
  }, [ready, pin, confirmed]);

  // Update popup bubble text to strictly match parsed address label without showing coordinates
  useEffect(() => {
    const marker = markerRef.current;
    const map = mapRef.current;
    const infoWindow = infoWindowRef.current;
    if (!marker || !map || !infoWindow || !pin) return;

    const safe = addressText?.trim() ? addressText.trim() : 'Resolving Target Location...';
    const escaped = safe.replace(/</g, '&lt;').replace(/>/g, '&gt;');
    
    infoWindow.setContent(`<div class="map-picker__infowindow" style="color:#111; font-weight:500; padding:4px;">${escaped}</div>`);
    marker.setTitle(safe);
    
    // InfoWindow position sync logic isolated from jarring map pan jumping loops
    infoWindow.setPosition({ lat: pin.lat, lng: pin.lng });
  }, [addressText, pin]);

  const handleConfirm = async () => {
    if (!pin || !addressText.trim() || resolvingAddress) return;
    setConfirming(true);
    setConfirmError(null);

    try {
      onConfirm({ addressText: addressText.trim(), latitude: pin.lat, longitude: pin.lng });
      setConfirmed(true);
      onLockChange?.(true);
      infoWindowRef.current?.close(); // Clean interface viewport immediately on lock confirmation
    } catch (err: unknown) {
      setConfirmError(err instanceof Error ? err.message : 'Could not confirm location.');
    } finally {
      setConfirming(false);
    }
  };

  const handleUnlock = () => {
    setConfirmed(false);
    onLockChange?.(false);
    if (mapRef.current && markerRef.current) {
      infoWindowRef.current?.open({ map: mapRef.current, anchor: markerRef.current });
    }
  };

  if (missingKey) {
    return (
      <div className="map-picker map-picker--setup">
        <p className="map-picker__setup-title">Google Maps API key required</p>
        <p className="map-picker__setup-note">
          Add <code>VITE_GOOGLE_MAPS_API_KEY</code> to <code>frontend/.env.local</code> and restart the server.
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="map-picker map-picker--setup">
        <p className="map-picker__setup-title">Map failed to load</p>
        <p className="map-picker__setup-note">{error}</p>
      </div>
    );
  }

  return (
    <div className="map-picker">
      <p className="map-picker__hint">
        Drag the pin or tap the map to set your restaurant location, then confirm.
      </p>

      <div className="map-picker__frame">
        {initializing && (
          <div className="map-picker__loading">Loading map…</div>
        )}
        <div ref={mapContainerRef} className="map-picker__canvas" aria-label="Interactive location map" />
      </div>

      {initError && (
        <span className="map-picker__error" role="alert">{initError}</span>
      )}

      <div className={`map-picker__address${confirmed ? ' map-picker__address--confirmed' : ''}`}>
        <span className="map-picker__address-label">Address text</span>
        <input
          type="text"
          className={`map-picker__address-input${!addressText ? ' map-picker__address-input--empty' : ''}`}
          value={addressText}
          placeholder={resolvingAddress ? 'Resolving address…' : 'Type address (optional)'}
          onChange={(e) => {
            const next = e.target.value;
            userEditedAddressRef.current = true;
            setAddressText(next);
            if (pin) {
              onChangeRef.current?.({ addressText: next, latitude: pin.lat, longitude: pin.lng });
            }
          }}
          disabled={disabled || confirmed}
          aria-label="Address text"
        />
      </div>

      {confirmError && (
        <span className="map-picker__error" role="alert">{confirmError}</span>
      )}

      <div className="map-picker__actions">
        <button
          type="button"
          className={`map-picker__confirm${confirmed ? ' map-picker__confirm--confirmed' : ''}`}
          onClick={handleConfirm}
          disabled={disabled || !ready || initializing || confirming || !pin || resolvingAddress || confirmed}
        >
          {confirmed ? 'Location Confirmed ✓' : confirming ? 'Confirming…' : 'Confirm this location'}
        </button>

        {confirmed && (
          <button
            type="button"
            className="map-picker__unlock"
            onClick={handleUnlock}
            disabled={disabled}
          >
            Change location
          </button>
        )}
      </div>
    </div>
  );
};

export default MapLocationPicker;