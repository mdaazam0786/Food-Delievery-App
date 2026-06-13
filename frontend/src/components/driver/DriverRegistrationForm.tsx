import React, { useState } from 'react';
import './DriverRegistrationForm.css';
import { useDriverAuth, DriverProfile } from '../../context/DriverAuthContext';
import { useToast } from '../../context/ToastContext';
import { getAccessToken, getStoredUser } from '../../services/authService';

// ─── Types ────────────────────────────────────────────────────────────────────

interface FormData {
  fullName: string;
  phoneNumber: string;
  vehicleType: 'BIKE' | 'CYCLE' | 'CAR';
  cityZone: string;
}

interface DriverRegistrationFormProps {
  onRegistrationComplete?: (profile: DriverProfile) => void;
}

// ─── Icons ────────────────────────────────────────────────────────────────────

const BikeIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="7" cy="17" r="4" stroke="currentColor" strokeWidth="2"/>
    <circle cx="17" cy="17" r="4" stroke="currentColor" strokeWidth="2"/>
    <path d="M17 9l-3-3m0 0l-4 4m4-4h-6M9 6l2 7m0 0l6 4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const UserIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="1.5"/>
    <path d="M6 20c0-3.3 2.7-6 6-6s6 2.7 6 6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

const PhoneIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="5" y="2" width="14" height="20" rx="2" stroke="currentColor" strokeWidth="1.5"/>
    <path d="M9 21h6" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
  </svg>
);

const LocationIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 1118 0z" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
    <circle cx="12" cy="10" r="3" fill="currentColor"/>
  </svg>
);

const CheckIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="20 6 9 17 4 12" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

// ─── Main Component ───────────────────────────────────────────────────────────

export const DriverRegistrationForm: React.FC<DriverRegistrationFormProps> = ({
  onRegistrationComplete,
}) => {
  const { setIsRegistered, setDriverProfile } = useDriverAuth();
  const { showToast } = useToast();

  // Form state
  const [formData, setFormData] = useState<FormData>({
    fullName: '',
    phoneNumber: '',
    vehicleType: 'BIKE',
    cityZone: '',
  });

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  // Form validation - phone must be 10-15 digits
  const isPhoneValid = /^\d{10,15}$/.test(formData.phoneNumber.trim());
  const isFormValid =
    formData.fullName.trim().length > 0 &&
    isPhoneValid &&
    formData.vehicleType &&
    formData.cityZone.trim().length > 0;

  // Handle input changes
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
    setSubmitError(null);
  };

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!isFormValid) {
      setSubmitError('Please fill in all fields correctly. Phone must be 10-15 digits.');
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const token = getAccessToken();
      const headers: Record<string, string> = {
        'Content-Type': 'application/json',
      };

      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }

      const response = await fetch('/api/drivers', {
        method: 'POST',
        headers,
        body: JSON.stringify({
          fullName: formData.fullName.trim(),
          phoneNumber: formData.phoneNumber.trim(),
          email: getStoredUser()?.email || '',
          vehicleType: formData.vehicleType,
          cityZone: formData.cityZone.trim(),
        }),
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || errorData.error || `HTTP ${response.status}`;
        console.error('[DriverRegistrationForm] API error:', errorMessage, errorData);
        throw new Error(errorMessage);
      }

      const data = await response.json();
      console.log('[DriverRegistrationForm] Registration successful:', data);
      const registeredDriver = data.data || data;

      if (registeredDriver && registeredDriver.id) {
        // Update context
        setDriverProfile(registeredDriver);
        setIsRegistered(true);

        // Show success toast
        showToast('Registration successful! Welcome to the fleet.', 'success');

        // Call completion callback if provided
        if (onRegistrationComplete) {
          setTimeout(() => {
            onRegistrationComplete(registeredDriver);
          }, 1000);
        }
      } else {
        throw new Error('Invalid response from server');
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'An error occurred';
      setSubmitError(errorMessage);
      showToast(errorMessage, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="drv-reg-form">
      <div className="drv-reg-form__wrapper">
        <div className="drv-reg-form__card">
          {/* Header */}
          <div className="drv-reg-form__header">
            <div className="drv-reg-form__icon-badge">
              <BikeIcon />
            </div>
            <h1 className="drv-reg-form__title">Complete Your Profile</h1>
            <p className="drv-reg-form__subtitle">
              Join thousands of delivery partners earning on their terms
            </p>
          </div>

          {/* Form */}
          <form onSubmit={handleSubmit} className="drv-reg-form__form">
            {/* Error message */}
            {submitError && (
              <div className="drv-reg-form__error-banner" role="alert">
                <span className="drv-reg-form__error-icon">⚠️</span>
                <span>{submitError}</span>
              </div>
            )}

            {/* Full Name Field */}
            <div className="drv-reg-form__field-group">
              <label htmlFor="fullName" className="drv-reg-form__label">
                <UserIcon />
                Full Name
              </label>
              <input
                id="fullName"
                type="text"
                name="fullName"
                placeholder="Enter your full name"
                value={formData.fullName}
                onChange={handleInputChange}
                disabled={isSubmitting}
                className="drv-reg-form__input"
                required
              />
            </div>

            {/* Phone Number Field */}
            <div className="drv-reg-form__field-group">
              <label htmlFor="phoneNumber" className="drv-reg-form__label">
                <PhoneIcon />
                Phone Number
              </label>
              <input
                id="phoneNumber"
                type="tel"
                name="phoneNumber"
                placeholder="10-15 digit phone number"
                value={formData.phoneNumber}
                onChange={handleInputChange}
                disabled={isSubmitting}
                className={`drv-reg-form__input ${!isPhoneValid && formData.phoneNumber ? 'drv-reg-form__input--error' : ''}`}
                required
              />
              {formData.phoneNumber && !isPhoneValid && (
                <span className="drv-reg-form__field-error">Phone must be 10-15 digits</span>
              )}
            </div>

            {/* Vehicle Type Dropdown */}
            <div className="drv-reg-form__field-group">
              <label htmlFor="vehicleType" className="drv-reg-form__label">
                <BikeIcon />
                Vehicle Type
              </label>
              <select
                id="vehicleType"
                name="vehicleType"
                value={formData.vehicleType}
                onChange={handleInputChange}
                disabled={isSubmitting}
                className="drv-reg-form__select"
                required
              >
                <option value="BIKE">🏍️ Bike</option>
                <option value="CYCLE">🚴 Cycle</option>
                <option value="CAR">🚗 Car</option>
              </select>
            </div>

            {/* City Zone Field */}
            <div className="drv-reg-form__field-group">
              <label htmlFor="cityZone" className="drv-reg-form__label">
                <LocationIcon />
                City / Operational Zone
              </label>
              <input
                id="cityZone"
                type="text"
                name="cityZone"
                placeholder="e.g., Delhi, Mumbai, Bangalore"
                value={formData.cityZone}
                onChange={handleInputChange}
                disabled={isSubmitting}
                className="drv-reg-form__input"
                required
              />
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={!isFormValid || isSubmitting}
              className="drv-reg-form__submit"
              aria-busy={isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <svg
                    width="16"
                    height="16"
                    viewBox="0 0 24 24"
                    fill="none"
                    className="drv-reg-form__spinner"
                    aria-hidden="true"
                  >
                    <circle
                      cx="12"
                      cy="12"
                      r="10"
                      stroke="currentColor"
                      strokeWidth="2.5"
                      strokeLinecap="round"
                      strokeDasharray="32"
                      strokeDashoffset="12"
                    />
                  </svg>
                  <span>Registering...</span>
                </>
              ) : (
                <>
                  <CheckIcon />
                  <span>Register as Rider</span>
                </>
              )}
            </button>
          </form>

          {/* Benefits Section */}
          <div className="drv-reg-form__benefits">
            <h3 className="drv-reg-form__benefits-title">Why join us?</h3>
            <ul className="drv-reg-form__benefits-list">
              <li>💰 Flexible earnings & instant payouts</li>
              <li>📍 Work in your preferred zones</li>
              <li>🛡️ Insurance & support 24/7</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
};

export default DriverRegistrationForm;
