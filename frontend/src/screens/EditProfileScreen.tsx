import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import FormInput from '../components/auth/FormInput';
import { fetchCurrentUser, updateUserProfile, type UserProfile } from '../services/userService';
import './EditProfileScreen.css';

interface FormState {
  fullName: string;
  email: string;
  phone: string;
}

const EditProfileScreen: React.FC = () => {
  const navigate = useNavigate();
  const [form, setForm] = useState<FormState>({
    fullName: '',
    email: '',
    phone: '',
  });
  const [user, setUser] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true);
        const profile = await fetchCurrentUser();
        setUser(profile);
        setForm({
          fullName: profile.fullName || '',
          email: profile.email || '',
          phone: profile.phone || '',
        });
        setError(null);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Failed to load profile';
        setError(message);
        setUser(null);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  const handleChange = (field: keyof FormState, value: string) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setSuccess(false);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!form.fullName.trim() || !form.email.trim()) {
      setError('Full Name and Email are required');
      return;
    }

    try {
      setSaving(true);
      setError(null);
      
      const updated = await updateUserProfile({
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || undefined,
      });

      setUser(updated);
      setSuccess(true);
      
      // Clear success message after 2 seconds and navigate back
      setTimeout(() => {
        navigate('/profile');
      }, 2000);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to update profile';
      setError(message);
      setSuccess(false);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="ep-screen">
        <div className="ep-header">
          <button className="ep-header__back" onClick={() => navigate('/profile')}>
            ←
          </button>
          <h1 className="ep-header__title">Edit Profile</h1>
          <div className="ep-header__spacer" />
        </div>
        <div className="ep-loading">
          <p>Loading profile…</p>
        </div>
      </div>
    );
  }

  if (!user) {
    return (
      <div className="ep-screen">
        <div className="ep-header">
          <button className="ep-header__back" onClick={() => navigate('/profile')}>
            ←
          </button>
          <h1 className="ep-header__title">Edit Profile</h1>
          <div className="ep-header__spacer" />
        </div>
        <div className="ep-error">
          <p>{error || 'Failed to load profile'}</p>
          <button 
            className="ep-error__retry"
            onClick={() => window.location.reload()}
          >
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="ep-screen">
      {/* ── Header ── */}
      <div className="ep-header">
        <button className="ep-header__back" onClick={() => navigate('/profile')}>
          ←
        </button>
        <h1 className="ep-header__title">Edit Profile</h1>
        <div className="ep-header__spacer" />
      </div>

      {/* ── Form ── */}
      <form className="ep-form" onSubmit={handleSubmit} noValidate>
        {/* Error Message */}
        {error && (
          <div className="ep-form__message ep-form__message--error">
            {error}
          </div>
        )}

        {/* Success Message */}
        {success && (
          <div className="ep-form__message ep-form__message--success">
            Profile updated successfully! Redirecting…
          </div>
        )}

        {/* Form Fields */}
        <div className="ep-form__fields">
          {/* Full Name */}
          <FormInput
            label="Full Name"
            placeholder="Enter your full name"
            value={form.fullName}
            onChange={(value) => handleChange('fullName', value)}
            type="text"
            autoComplete="name"
            className="ep-form__field"
          />

          {/* Email */}
          <FormInput
            label="Email"
            placeholder="Enter your email address"
            value={form.email}
            onChange={(value) => handleChange('email', value)}
            type="email"
            autoComplete="email"
            className="ep-form__field"
          />

          {/* Phone Number */}
          <FormInput
            label="Phone Number"
            placeholder="Enter your phone number"
            value={form.phone}
            onChange={(value) => handleChange('phone', value)}
            type="tel"
            autoComplete="tel"
            className="ep-form__field"
          />
        </div>

        {/* Save Button */}
        <button
          type="submit"
          className="ep-form__submit"
          disabled={saving}
          aria-busy={saving}
        >
          {saving ? 'Saving…' : 'SAVE'}
        </button>
      </form>
    </div>
  );
};

export default EditProfileScreen;
