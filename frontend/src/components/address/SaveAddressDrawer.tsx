import React, { useState } from 'react';
import MapLocationPicker from '../admin/MapLocationPicker';
import { saveUserAddress } from '../../services/userService';
import { useToast } from '../../context/ToastContext';
import './SaveAddressDrawer.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const CloseIcon = () => (
  <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"/>
    <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round"/>
  </svg>
);

// ─── Types ────────────────────────────────────────────────────────────────────

export interface SaveAddressDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  onAddressSelected?: (address: { label: string; formattedAddress: string; latitude: number; longitude: number }) => void;
  onAddressSaved?: () => void;
}

type AddressLabel = 'HOME' | 'WORK' | 'OTHER';

// ─── Component ────────────────────────────────────

const SaveAddressDrawer: React.FC<SaveAddressDrawerProps> = ({ isOpen, onClose, onAddressSelected, onAddressSaved }) => {
  const { showToast } = useToast();
  const [latitude, setLatitude] = useState(28.6139);
  const [longitude, setLongitude] = useState(77.209);
  const [formattedAddress, setFormattedAddress] = useState('');
  const [apartmentInput, setApartmentInput] = useState('');
  const [streetInput, setStreetInput] = useState('');
  const [selectedLabel, setSelectedLabel] = useState<AddressLabel>('HOME');
  const [saving, setSaving] = useState(false);
  const [locationLocked, setLocationLocked] = useState(false);

  // Apply location from map confirmation (same logic as RestaurantProvisioningScreen)
  const applyLocation = (addressText: string, lat: number, lng: number) => {
    setFormattedAddress(addressText);
    setLatitude(lat);
    setLongitude(lng);
  };

  // Handle map changes (when user drags pin or clicks on map)
  const handleMapChange = ({ addressText, latitude, longitude }: { addressText: string; latitude: number; longitude: number }) => {
    setLocationLocked(false);
    applyLocation(addressText, latitude, longitude);
  };

  const handleSaveAddress = async () => {
    // Validation
    if (!apartmentInput.trim()) {
      showToast('Please enter door/flat number', 'error');
      return;
    }

    if (!streetInput.trim()) {
      showToast('Please enter area/landmark', 'error');
      return;
    }

    if (!formattedAddress.trim()) {
      showToast('Please confirm a location on the map', 'error');
      return;
    }

    if (!locationLocked) {
      showToast('Please confirm the location before saving', 'error');
      return;
    }

    setSaving(true);
    try {
      // Call backend to save address
      const savedAddress = await saveUserAddress({
        formattedAddress,
        street: streetInput.trim(),
        apartment: apartmentInput.trim(),
        postCode: '', // Optional field
        label: selectedLabel,
        latitude,
        longitude,
      });

      showToast('Address saved successfully!', 'success');

      // Callback for CheckoutScreen to refresh the address list
      onAddressSaved?.();

      // Also call the original callback if provided
      onAddressSelected?.({
        label: selectedLabel,
        formattedAddress: savedAddress.formattedAddress,
        latitude: savedAddress.latitude,
        longitude: savedAddress.longitude,
      });

      handleClose();
    } catch (err) {
      const errorMsg = err instanceof Error ? err.message : 'Failed to save address';
      showToast(errorMsg, 'error');
      console.error('Save address error:', err);
    } finally {
      setSaving(false);
    }
  };

  const handleClose = () => {
    setApartmentInput('');
    setStreetInput('');
    setFormattedAddress('');
    setLatitude(28.6139);
    setLongitude(77.209);
    setSelectedLabel('HOME');
    setLocationLocked(false);
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="save-address-overlay" onClick={handleClose}>
      <div className="save-address-drawer" onClick={(e) => e.stopPropagation()}>
        {/* Close Button */}
        <button className="save-address-close" onClick={handleClose} aria-label="Close">
          <CloseIcon />
        </button>

        {/* Form Title */}
        <div className="save-address-form-header">
          <h2 className="save-address-title">Save Delivery Address</h2>
          <p className="save-address-subtitle">Confirm location on map, then add apartment &amp; landmark details.</p>
        </div>

        {/* Map Location Picker Section - using same component as restaurant provisioning */}
        <div className="save-address-map-section">
          <MapLocationPicker
            disabled={saving}
            onChange={handleMapChange}
            onConfirm={({ addressText, latitude, longitude }) =>
              (() => {
                applyLocation(addressText, latitude, longitude);
                setLocationLocked(true);
              })()
            }
            onLockChange={(locked) => setLocationLocked(locked)}
          />
        </div>

        {/* Form Section */}
        <div className="save-address-form">
          {/* Door / Flat No. */}
          <div className="save-address-field">
            <label className="save-address-label">Door / Flat No. <span className="save-address-req">*</span></label>
            <input
              type="text"
              className="save-address-input"
              placeholder="e.g., 123, Apt 4B"
              value={apartmentInput}
              onChange={(e) => setApartmentInput(e.target.value)}
              disabled={saving}
            />
          </div>

          {/* Area / Landmark */}
          <div className="save-address-field">
            <label className="save-address-label">Area / Landmark <span className="save-address-req">*</span></label>
            <input
              type="text"
              className="save-address-input"
              placeholder="e.g., Near Metro Station"
              value={streetInput}
              onChange={(e) => setStreetInput(e.target.value)}
              disabled={saving}
            />
          </div>

          {/* Label Selection */}
          <div className="save-address-field">
            <label className="save-address-label">Label</label>
            <div className="save-address-label-pills">
              {(['HOME', 'WORK', 'OTHER'] as AddressLabel[]).map((label) => (
                <button
                  key={label}
                  type="button"
                  className={`save-address-label-pill ${selectedLabel === label ? 'save-address-label-pill--active' : ''}`}
                  onClick={() => setSelectedLabel(label)}
                  disabled={saving}
                  aria-pressed={selectedLabel === label}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          {/* Save Button */}
          <button
            className="save-address-save-btn"
            onClick={handleSaveAddress}
            disabled={saving || !formattedAddress || !locationLocked}
            aria-busy={saving}
          >
            {saving ? 'Saving...' : 'SAVE ADDRESS & PROCEED'}
          </button>

          {/* Cancel Button */}
          <button
            className="save-address-cancel-btn"
            onClick={handleClose}
            disabled={saving}
          >
            CANCEL
          </button>
        </div>
      </div>
    </div>
  );
};

export default SaveAddressDrawer;
