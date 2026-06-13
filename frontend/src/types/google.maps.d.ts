/** Google Maps typings for the draggable pin location picker. */
declare namespace google.maps {
  class LatLng {
    constructor(lat: number, lng: number);
    lat(): number;
    lng(): number;
  }

  interface LatLngLiteral {
    lat: number;
    lng: number;
  }

  interface MapOptions {
    center?: LatLng | LatLngLiteral;
    zoom?:   number;
    mapTypeControl?: boolean;
    streetViewControl?: boolean;
    fullscreenControl?: boolean;
  }

  interface MapMouseEvent {
    latLng: LatLng | null;
    placeId?: string;
    stop?: () => void;
  }

  interface MarkerOptions {
    map?:       Map;
    position?:  LatLng | LatLngLiteral;
    draggable?: boolean;
    title?:     string;
  }

  class Map {
    constructor(el: HTMLElement, opts?: MapOptions);
    setCenter(latlng: LatLng | LatLngLiteral): void;
    panTo(latlng: LatLng | LatLngLiteral): void;
    addListener(event: string, handler: (e: MapMouseEvent) => void): unknown;
  }

  class Marker {
    constructor(opts?: MarkerOptions);
    setPosition(latlng: LatLng | LatLngLiteral): void;
    getPosition(): LatLng | null | undefined;
    setTitle(title: string): void;
    setDraggable(draggable: boolean): void;
    addListener(event: string, handler: () => void): unknown;
  }

  interface InfoWindowOptions {
    content?: string;
    disableAutoPan?: boolean;
  }

  interface InfoWindowOpenOptions {
    map: Map;
    anchor?: Marker;
  }

  class InfoWindow {
    constructor(opts?: InfoWindowOptions);
    setContent(content: string): void;
    setPosition(latlng: LatLng | LatLngLiteral): void;
    open(opts: InfoWindowOpenOptions): void;
    close(): void;
  }

  interface GeocoderRequest {
    location: LatLngLiteral;
    placeId?: string;
  }

  interface GeocoderResult {
    formatted_address: string;
  }

  type GeocoderStatus = 'OK' | string;

  class Geocoder {
    geocode(request: GeocoderRequest): Promise<GeocoderResult[]>;
    geocode(
      request: GeocoderRequest,
      callback: (results: GeocoderResult[] | null, status: GeocoderStatus) => void
    ): void;
  }

  namespace places {
    type PlacesServiceStatus = 'OK' | string;

    interface PlaceGeometry {
      location: LatLng;
    }

    interface PlaceResult {
      place_id?: string;
      name?: string;
      formatted_address?: string;
      geometry?: PlaceGeometry;
    }

    interface PlaceDetailsRequest {
      placeId: string;
      fields: Array<'name' | 'formatted_address' | 'geometry'>;
    }

    interface NearbySearchRequest {
      location: LatLngLiteral;
      radius?: number;
    }

    type NearbySearchCallback = (results: PlaceResult[] | null, status: PlacesServiceStatus) => void;

    class PlacesService {
      constructor(attrContainer: Map | HTMLElement);
      getDetails(
        request: PlaceDetailsRequest,
        callback: (place: PlaceResult | null, status: PlacesServiceStatus) => void
      ): void;

      nearbySearch(
        request: NearbySearchRequest,
        callback: NearbySearchCallback
      ): void;
    }
  }

  namespace event {
    function removeListener(listener: unknown): void;
  }
}

declare const google: { maps: typeof google.maps };

declare interface Window {
  google?: typeof google;
}