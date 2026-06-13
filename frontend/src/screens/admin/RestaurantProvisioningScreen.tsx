import React, { useCallback, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MapLocationPicker from '../../components/admin/MapLocationPicker';
import { useRestaurant } from '../../context/useRestaurant';
import {
  createRestaurant,
  uploadRestaurantImageImmediate,
  reverseGeocodeBackend,
} from '../../services/restaurantService';
import {
  formatFssaiInput,
  formatGstInput,
  validateFssai,
  validateGst,
} from '../../utils/complianceValidation';
import './RestaurantProvisioningScreen.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const LogoIcon = () => (
  <svg width="32" height="32" viewBox="0 0 36 36" fill="none" aria-hidden="true">
    <path d="M4 22 Q4 10 18 10 Q32 10 32 22 Z" fill="white"/>
    <rect x="2" y="22" width="32" height="4" rx="2" fill="white"/>
    <circle cx="18" cy="10" r="3" fill="white"/>
    <circle cx="18" cy="10" r="1.5" fill="#FF7622"/>
  </svg>
);

const StoreIcon = () => (
  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"
      stroke="#FF7622" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <polyline points="9 22 9 12 15 12 15 22"
      stroke="#FF7622" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const GpsIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="2"/>
    <path d="M12 2v3M12 19v3M2 12h3M19 12h3" stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

const MapIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M9 3L3 6v15l6-3 6 3 6-3V3l-6 3-6-3z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round"/>
    <path d="M9 3v15M15 6v15" stroke="currentColor" strokeWidth="2"/>
  </svg>
);

const UploadIcon = () => (
  <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"
      stroke="#9EA1B1" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    <polyline points="17 8 12 3 7 8"
      stroke="#9EA1B1" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    <line x1="12" y1="3" x2="12" y2="15" stroke="#9EA1B1" strokeWidth="1.8" strokeLinecap="round"/>
  </svg>
);

const SpinnerIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none"
    className="prov-spin" aria-hidden="true">
    <circle cx="12" cy="12" r="10" stroke="white" strokeWidth="2.5"
      strokeLinecap="round" strokeDasharray="32" strokeDashoffset="12"/>
  </svg>
);

// ─── Form state ───────────────────────────────────────────────────────────────

type LocationMode = 'gps' | 'map';

interface FormState {
  name:        string;
  description: string;
  gstNo:       string;
  fssaiNo:     string;
  addressText: string;
  latitude:    number;
  longitude:   number;
  imageUrl:    string;
}

type FieldKey = keyof FormState;
type FieldErrors = Partial<Record<FieldKey | 'location', string>>;

const EMPTY: FormState = {
  name: '', description: '', gstNo: '', fssaiNo: '',
  addressText: '', imageUrl: '', latitude: 0, longitude: 0,
};

const WIZARD_STEPS = [
  'Business profile',
  'Compliance & licenses',
  'Location & branding',
];

// ─── Component ────────────────────────────────────────────────────────────────

const RestaurantProvisioningScreen: React.FC = () => {
  const { userProfile, completeProvisioning } = useRestaurant();
  const navigate = useNavigate();

  const [form,          setForm]          = useState<FormState>(EMPTY);
  const [errors,        setErrors]        = useState<FieldErrors>({});
  const [submitting,    setSubmitting]    = useState(false);
  const [submitError,   setSubmitError]   = useState<string | null>(null);
  const [success,       setSuccess]       = useState(false);
  const [imageDrag,     setImageDrag]     = useState(false);
  const [uploading,     setUploading]     = useState(false);
  const [uploadError,   setUploadError]   = useState<string | null>(null);
  const [locationMode,  setLocationMode]  = useState<LocationMode | null>(null);
  const [locating,      setLocating]      = useState(false);
  const [locationError, setLocationError] = useState<string | null>(null);
  const [locationLocked, setLocationLocked] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const set = (key: FieldKey, value: string | number) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const applyLocation = useCallback((addressText: string, latitude: number, longitude: number) => {
    setForm((prev) => ({ ...prev, addressText, latitude, longitude }));
    setLocationError(null);
    setErrors((prev) => ({ ...prev, addressText: undefined, location: undefined }));
  }, []);

  const handleMapChange = useCallback(({ addressText, latitude, longitude }: { addressText: string; latitude: number; longitude: number }) => {
    setLocationLocked(false);
    applyLocation(addressText, latitude, longitude);
  }, [applyLocation]);

  const handleUseCurrentLocation = () => {
    setLocationLocked(false);
    setLocationMode('gps');
    setLocationError(null);

    if (!('geolocation' in navigator)) {
      setLocationError('Geolocation is not supported in this browser.');
      return;
    }

    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        try {
          const result = await reverseGeocodeBackend(lat, lng);
          applyLocation(result.displayName, lat, lng);
        } catch {
          applyLocation(`${lat.toFixed(5)}, ${lng.toFixed(5)}`, lat, lng);
        } finally {
          setLocating(false);
        }
      },
      (err) => {
        setLocating(false);
        setLocationError(err.message || 'Could not access your location.');
      },
      { enableHighAccuracy: true, timeout: 12_000 },
    );
  };

  const handleImageFile = (file: File | null) => {
    if (!file) return;
    
    setUploadError(null);
    setUploading(true);

    // Immediately upload the file to the backend
    uploadRestaurantImageImmediate(file)
      .then((imageUrl) => {
        // Save the URL directly into the form
        setForm((prev) => ({ ...prev, imageUrl }));
        setUploading(false);
      })
      .catch((err: unknown) => {
        setUploading(false);
        setUploadError(
          err instanceof Error ? err.message : 'Failed to upload image'
        );
        // Reset preview on error
        setForm((prev) => ({ ...prev, imageUrl: '' }));
      });
  };

  const validate = (): boolean => {
    const e: FieldErrors = {};
    if (!form.name.trim()) e.name = 'Restaurant name is required.';
    const gstErr = validateGst(form.gstNo);
    if (gstErr) e.gstNo = gstErr;
    const fssaiErr = validateFssai(form.fssaiNo);
    if (fssaiErr) e.fssaiNo = fssaiErr;
    if (!form.addressText.trim()) e.addressText = 'Restaurant address is required.';
    if (!form.latitude || !form.longitude) {
      e.location = 'Select a location using GPS or map search.';
    }
    setErrors(e);
    return Object.keys(e).length === 0;
  };

  const handleSubmit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    if (!validate()) return;

    setSubmitting(true);
    setSubmitError(null);

    try {
      const created = await createRestaurant({
        name:        form.name.trim(),
        description: form.description.trim(),
        addressText: form.addressText.trim(),
        imageUrl:    form.imageUrl, // Already uploaded, just pass the URL
        latitude:    form.latitude,
        longitude:   form.longitude,
        gstNo:       form.gstNo.trim().toUpperCase(),
        fssaiNo:     form.fssaiNo.trim(),
      });

      completeProvisioning(created.id, created.name);
      setSuccess(true);

      // Short delay so the success label is visible, then navigate.
      // completeProvisioning() will refetch the restaurant data, then navigate.
      setTimeout(() => {
        navigate('/admin', { replace: true });
      }, 800);
    } catch (err: unknown) {
      setSubmitError(
        err instanceof Error ? err.message : 'Failed to provision restaurant. Please try again.',
      );
      setSubmitting(false);
    }
  };

  return (
    <div className="prov-page prov-page--overlay" role="main" aria-label="Restaurant provisioning wizard">

      <aside className="prov-left" aria-hidden="true">
        <div className="prov-left__inner">
          <div className="prov-left__logo">
            <div className="prov-left__logo-icon"><LogoIcon /></div>
            <span className="prov-left__logo-text">Foodzie</span>
          </div>

          <div className="prov-left__hero">
            <div className="prov-left__icon-wrap"><StoreIcon /></div>
            <h2 className="prov-left__headline">
              First-time setup &amp;<br/>compliance wizard
            </h2>
            <p className="prov-left__sub">
              Complete your restaurant profile, licenses, and location before accessing the admin dashboard.
            </p>
          </div>

          <div className="prov-left__steps">
            {WIZARD_STEPS.map((label, i) => (
              <div key={label} className="prov-left__step">
                <span className="prov-left__step-num">{i + 1}</span>
                <span className="prov-left__step-label">{label}</span>
              </div>
            ))}
          </div>
        </div>
      </aside>

      <main className="prov-right">
        <div className="prov-form-wrap">
          <div className="prov-form-header">
            <p className="prov-form-eyebrow">Admin onboarding</p>
            <h1 className="prov-form-title">Provision Your Restaurant</h1>
            <p className="prov-form-subtitle">
              {userProfile?.email
                ? `Signed in as ${userProfile.email}. All fields are required unless marked optional.`
                : 'Fill in your business details to activate the admin dashboard.'}
            </p>
          </div>

          <form className="prov-form" onSubmit={handleSubmit} noValidate>

            {/* ── Restaurant Name ── */}
            <div className="prov-field">
              <label className="prov-field__label" htmlFor="prov-name">
                Restaurant Name <span className="prov-field__req">*</span>
              </label>
              <input
                id="prov-name"
                className={`prov-field__input${errors.name ? ' prov-field__input--error' : ''}`}
                type="text"
                placeholder="e.g. Rose Garden Restaurant"
                value={form.name}
                onChange={(e) => set('name', e.target.value)}
                autoComplete="organization"
                disabled={submitting || success}
              />
              {errors.name && <span className="prov-field__error" role="alert">{errors.name}</span>}
            </div>

            {/* ── Cuisine / About ── */}
            <div className="prov-field">
              <label className="prov-field__label" htmlFor="prov-desc">
                Cuisine / About Description
                <span className="prov-field__optional"> — optional</span>
              </label>
              <textarea
                id="prov-desc"
                className="prov-field__textarea"
                placeholder="Describe your cuisine, specialties, or brand story…"
                rows={3}
                value={form.description}
                onChange={(e) => set('description', e.target.value)}
                disabled={submitting || success}
              />
            </div>

            {/* ── Compliance row ── */}
            <div className="prov-field-row">
              <div className="prov-field">
                <label className="prov-field__label" htmlFor="prov-gst">
                  GST Number <span className="prov-field__req">*</span>
                </label>
                <input
                  id="prov-gst"
                  className={`prov-field__input${errors.gstNo ? ' prov-field__input--error' : ''}`}
                  type="text"
                  placeholder="22AAAAA0000A1Z5"
                  value={form.gstNo}
                  onChange={(e) => set('gstNo', formatGstInput(e.target.value))}
                  maxLength={15}
                  disabled={submitting || success}
                  spellCheck={false}
                />
                {errors.gstNo && <span className="prov-field__error" role="alert">{errors.gstNo}</span>}
              </div>

              <div className="prov-field">
                <label className="prov-field__label" htmlFor="prov-fssai">
                  FSSAI License Number <span className="prov-field__req">*</span>
                </label>
                <input
                  id="prov-fssai"
                  className={`prov-field__input${errors.fssaiNo ? ' prov-field__input--error' : ''}`}
                  type="text"
                  inputMode="numeric"
                  placeholder="14-digit license number"
                  value={form.fssaiNo}
                  onChange={(e) => set('fssaiNo', formatFssaiInput(e.target.value))}
                  maxLength={14}
                  disabled={submitting || success}
                />
                {errors.fssaiNo && <span className="prov-field__error" role="alert">{errors.fssaiNo}</span>}
              </div>
            </div>

            {/* ── Location strategy ── */}
            <div className="prov-location">
              <p className="prov-location__title">Configure Restaurant Location</p>

              <div className="prov-location__actions">
                <button
                  type="button"
                  className={`prov-location__btn${locationMode === 'gps' ? ' prov-location__btn--active' : ''}`}
                  onClick={handleUseCurrentLocation}
                  disabled={submitting || success || locating}
                >
                  <GpsIcon />
                  {locating ? 'Locating…' : 'Use Current Location'}
                </button>

                <button
                  type="button"
                  className={`prov-location__btn${locationMode === 'map' ? ' prov-location__btn--active' : ''}`}
                  onClick={() => { setLocationLocked(false); setLocationMode('map'); }}
                  disabled={submitting || success}
                >
                  <MapIcon />
                  Pick on Map
                </button>
              </div>

              {locationMode === 'map' && (
                <MapLocationPicker
                  disabled={submitting || success}
                  onChange={handleMapChange}
                  onConfirm={({ addressText, latitude, longitude }) =>
                    (() => {
                      applyLocation(addressText, latitude, longitude);
                      setLocationLocked(true);
                    })()
                  }
                  onLockChange={(locked) => setLocationLocked(locked)}
                />
              )}

              <div className={`prov-location__resolved${locationLocked ? ' prov-location__resolved--confirmed' : ''}`}>
                <span className="prov-location__resolved-label">Address text</span>
                <p className={`prov-location__resolved-value${!form.addressText ? ' prov-location__resolved-value--empty' : ''}`}>
                  {form.addressText || 'No location selected yet'}
                </p>
              </div>

              {locationError && (
                <span className="prov-field__error" role="alert">{locationError}</span>
              )}
              {errors.addressText && (
                <span className="prov-field__error" role="alert">{errors.addressText}</span>
              )}
              {errors.location && (
                <span className="prov-field__error" role="alert">{errors.location}</span>
              )}
            </div>

            {/* ── Banner Image ── */}
            <div className="prov-field">
              <label className="prov-field__label">
                Cover Banner Image
                <span className="prov-field__optional"> — optional</span>
              </label>
              <div
                className={`prov-dropzone${imageDrag ? ' prov-dropzone--over' : ''}${form.imageUrl ? ' prov-dropzone--filled' : ''}`}
                onDragOver={(e) => { e.preventDefault(); setImageDrag(true); }}
                onDragLeave={() => setImageDrag(false)}
                onDrop={(e) => {
                  e.preventDefault();
                  setImageDrag(false);
                  handleImageFile(e.dataTransfer.files[0] ?? null);
                }}
                onClick={() => !submitting && !success && !uploading && fileInputRef.current?.click()}
                role="button"
                tabIndex={submitting || success || uploading ? -1 : 0}
                aria-label="Upload cover banner — click or drag and drop"
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  className="prov-dropzone__input"
                  onChange={(e) => handleImageFile(e.target.files?.[0] ?? null)}
                  aria-hidden="true"
                  tabIndex={-1}
                  disabled={submitting || success || uploading}
                />
                {uploading ? (
                  <div className="prov-dropzone__uploading">
                    <SpinnerIcon />
                    <span className="prov-dropzone__upload-text">Uploading image…</span>
                  </div>
                ) : form.imageUrl ? (
                  <div className="prov-dropzone__preview">
                    <img src={form.imageUrl} alt="Banner preview" className="prov-dropzone__img" />
                    <span className="prov-dropzone__change">Click to change</span>
                  </div>
                ) : (
                  <>
                    <UploadIcon />
                    <span className="prov-dropzone__primary">Drag &amp; drop your cover banner</span>
                    <span className="prov-dropzone__secondary">PNG, JPG, WEBP up to 10 MB</span>
                  </>
                )}
              </div>
              {uploadError && (
                <span className="prov-field__error" role="alert">{uploadError}</span>
              )}
            </div>

            {submitError && (
              <div className="prov-error-banner" role="alert">{submitError}</div>
            )}

            <button
              type="submit"
              className={`prov-cta${success ? ' prov-cta--success' : ''}`}
              disabled={submitting || success || uploading}
              aria-busy={submitting}
            >
              {success ? (
                'Restaurant provisioned — opening dashboard…'
              ) : submitting ? (
                <>
                  <SpinnerIcon />
                  Provisioning…
                </>
              ) : uploading ? (
                <>
                  <SpinnerIcon />
                  Uploading image…
                </>
              ) : (
                'PROVISION & REGISTER RESTAURANT'
              )}
            </button>
          </form>
        </div>
      </main>
    </div>
  );
};

export default RestaurantProvisioningScreen;
