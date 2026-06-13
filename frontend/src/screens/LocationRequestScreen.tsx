import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getDestinationByRole } from '../services/authService';
import './LocationRequestScreen.css';

// ── Inline SVG pieces ─────────────────────────────────────────────────────────

/** Stylised street-map illustration inside the circle */
const MapIllustration: React.FC = () => (
  <svg
    className="loc-map__svg"
    viewBox="0 0 320 320"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    {/* ── Road grid ── */}
    {/* Horizontal roads */}
    <rect x="0"   y="148" width="320" height="24" rx="0" fill="#E8EAF0"/>
    <rect x="0"   y="84"  width="320" height="14" rx="0" fill="#E8EAF0"/>
    <rect x="0"   y="222" width="320" height="14" rx="0" fill="#E8EAF0"/>
    {/* Vertical roads */}
    <rect x="148" y="0"   width="24"  height="320" rx="0" fill="#E8EAF0"/>
    <rect x="82"  y="0"   width="14"  height="320" rx="0" fill="#E8EAF0"/>
    <rect x="224" y="0"   width="14"  height="320" rx="0" fill="#E8EAF0"/>

    {/* ── Road centre dashes (main horizontal) ── */}
    {[20,60,100,190,230,270].map((x) => (
      <rect key={x} x={x} y="158" width="28" height="4" rx="2" fill="#D0D3DC"/>
    ))}
    {/* ── Road centre dashes (main vertical) ── */}
    {[20,60,100,190,230,270].map((y) => (
      <rect key={y} x="158" y={y} width="4" height="28" rx="2" fill="#D0D3DC"/>
    ))}

    {/* ── City blocks ── */}
    {/* Top-left quadrant */}
    <rect x="4"   y="4"   width="72"  height="74"  rx="8" fill="#DDE1EC"/>
    <rect x="4"   y="4"   width="34"  height="34"  rx="6" fill="#C8CCDB"/>
    <rect x="42"  y="4"   width="34"  height="34"  rx="6" fill="#C8CCDB"/>
    <rect x="4"   y="42"  width="72"  height="36"  rx="6" fill="#C8CCDB" opacity="0.6"/>

    {/* Top-right quadrant */}
    <rect x="174" y="4"   width="60"  height="74"  rx="8" fill="#DDE1EC"/>
    <rect x="240" y="4"   width="76"  height="34"  rx="6" fill="#C8CCDB"/>
    <rect x="240" y="42"  width="76"  height="36"  rx="6" fill="#C8CCDB" opacity="0.6"/>
    <rect x="174" y="4"   width="60"  height="34"  rx="6" fill="#C8CCDB" opacity="0.8"/>

    {/* Bottom-left quadrant */}
    <rect x="4"   y="174" width="72"  height="60"  rx="8" fill="#DDE1EC"/>
    <rect x="4"   y="238" width="72"  height="78"  rx="6" fill="#C8CCDB" opacity="0.7"/>
    <rect x="4"   y="174" width="34"  height="28"  rx="6" fill="#C8CCDB"/>
    <rect x="42"  y="174" width="34"  height="28"  rx="6" fill="#C8CCDB"/>

    {/* Bottom-right quadrant */}
    <rect x="174" y="174" width="60"  height="60"  rx="8" fill="#DDE1EC"/>
    <rect x="240" y="174" width="76"  height="60"  rx="6" fill="#C8CCDB" opacity="0.8"/>
    <rect x="174" y="238" width="142" height="78"  rx="6" fill="#C8CCDB" opacity="0.6"/>

    {/* ── Green park patches ── */}
    <rect x="6"   y="6"   width="28"  height="28"  rx="5" fill="#A5D6A7" opacity="0.55"/>
    <rect x="244" y="6"   width="36"  height="22"  rx="5" fill="#A5D6A7" opacity="0.45"/>
    <rect x="176" y="176" width="30"  height="30"  rx="5" fill="#A5D6A7" opacity="0.5"/>

    {/* ── Tiny building details ── */}
    <rect x="10"  y="48"  width="18"  height="22"  rx="3" fill="#B0B5C8"/>
    <rect x="32"  y="52"  width="14"  height="18"  rx="3" fill="#B0B5C8"/>
    <rect x="50"  y="46"  width="20"  height="24"  rx="3" fill="#B0B5C8"/>
    <rect x="178" y="8"   width="16"  height="20"  rx="3" fill="#B0B5C8"/>
    <rect x="198" y="12"  width="12"  height="16"  rx="3" fill="#B0B5C8"/>
    <rect x="244" y="48"  width="22"  height="26"  rx="3" fill="#B0B5C8"/>
    <rect x="270" y="44"  width="16"  height="22"  rx="3" fill="#B0B5C8"/>

    {/* ── Roundabout at main intersection ── */}
    <circle cx="160" cy="160" r="28" fill="#E8EAF0"/>
    <circle cx="160" cy="160" r="20" fill="#F4F5F9"/>
    <circle cx="160" cy="160" r="12" fill="#DDE1EC"/>

    {/* ── Location pin shadow (ellipse on ground) ── */}
    <ellipse cx="160" cy="218" rx="22" ry="7" fill="rgba(0,0,0,0.10)"/>
  </svg>
);

/** Large map-pin icon that drops into the centre of the circle */
const MapPinLarge: React.FC = () => (
  <svg
    className="loc-map__pin"
    viewBox="0 0 64 80"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    {/* Pin body */}
    <path
      d="M32 4C18.745 4 8 14.745 8 28c0 18 24 48 24 48s24-30 24-48C56 14.745 45.255 4 32 4z"
      fill="#FF7622"
    />
    {/* Pin inner highlight */}
    <path
      d="M32 6C19.85 6 10 15.85 10 28c0 17 22 45 22 45s22-28 22-45C54 15.85 44.15 6 32 6z"
      fill="#FF8C42"
      opacity="0.4"
    />
    {/* White inner circle */}
    <circle cx="32" cy="28" r="10" fill="white"/>
    {/* Orange dot inside */}
    <circle cx="32" cy="28" r="5" fill="#FF7622"/>
  </svg>
);

/** Small pin icon inside the button badge */
const PinMini: React.FC = () => (
  <svg
    width="14" height="18"
    viewBox="0 0 14 18"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <path
      d="M7 0C3.134 0 0 3.134 0 7c0 4.5 7 11 7 11s7-6.5 7-11C14 3.134 10.866 0 7 0z"
      fill="white"
    />
    <circle cx="7" cy="7" r="2.5" fill="rgba(255,118,34,0.85)"/>
  </svg>
);

// ── Component ─────────────────────────────────────────────────────────────────

const LocationRequestScreen: React.FC = () => {
  const navigate = useNavigate();
  const [requesting, setRequesting] = useState(false);

  const handleAccessLocation = async () => {
    setRequesting(true);
    try {
      // Request the browser Geolocation API if available
      if ('geolocation' in navigator) {
        await new Promise<GeolocationPosition>((resolve, reject) =>
          navigator.geolocation.getCurrentPosition(resolve, reject, {
            timeout: 8000,
          })
        );
      }
    } catch {
      // Permission denied or unavailable — proceed anyway
    } finally {
      setRequesting(false);
      navigate(getDestinationByRole(), { replace: true });
    }
  };

  return (
    <div className="loc-page">
      <div className="loc-viewport">

        {/* ── Top spacer ── */}
        <div className="loc-spacer-top" />

        {/* ── Map circle ── */}
        <div className="loc-map">
          <MapIllustration />
          <div className="loc-map__pin-wrap">
            <MapPinLarge />
          </div>
        </div>

        {/* ── Text block ── */}
        <div className="loc-text">
          <h1 className="loc-text__title">Your Location</h1>
          <p className="loc-text__subtitle">
            We need your location to find restaurants and
            delivery options near you.
          </p>
        </div>

        {/* ── Spacer pushes CTA to bottom ── */}
        <div className="loc-spacer-bottom" />

        {/* ── CTA button ── */}
        <button
          className="loc-cta"
          onClick={handleAccessLocation}
          disabled={requesting}
          aria-busy={requesting}
          aria-label="Allow location access"
        >
          <span className="loc-cta__text">
            {requesting ? 'REQUESTING…' : 'ACCESS LOCATION'}
          </span>
          {/* Right-side translucent badge with mini pin */}
          <span className="loc-cta__badge" aria-hidden="true">
            <PinMini />
          </span>
        </button>

        {/* ── Legal disclaimer ── */}
        <p className="loc-disclaimer">
          DFOOD WILL ACCESS YOUR LOCATION ONLY WHILE USING THE APP
        </p>

      </div>
    </div>
  );
};

export default LocationRequestScreen;
