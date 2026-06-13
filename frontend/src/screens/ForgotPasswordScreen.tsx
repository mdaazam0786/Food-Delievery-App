import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthBottomSheetLayout from '../components/auth/AuthBottomSheetLayout';
import FormInput from '../components/auth/FormInput';
import PrimaryButton from '../components/auth/PrimaryButton';
import { forgotPassword } from '../services/authService';
import './ForgotPasswordScreen.css';

// ── Envelope SVG — used in the success state ──────────────────────────────────

const EnvelopeIcon: React.FC = () => (
  <svg
    className="fp-success__icon"
    viewBox="0 0 80 80"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    {/* Outer circle background */}
    <circle cx="40" cy="40" r="40" fill="#FFF3E8" />
    {/* Envelope body */}
    <rect x="16" y="26" width="48" height="34" rx="5" fill="#ff7a28" opacity="0.15"
      stroke="#ff7a28" strokeWidth="2.5" />
    {/* Envelope flap (V shape) */}
    <polyline points="16,26 40,46 64,26"
      stroke="#ff7a28" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
      fill="none" />
    {/* Check badge — bottom-right */}
    <circle cx="56" cy="54" r="12" fill="#ff7a28" />
    <polyline points="50,54 54,58 62,49"
      stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
      fill="none" />
  </svg>
);

// ── Component ─────────────────────────────────────────────────────────────────

const ForgotPasswordScreen: React.FC = () => {
  const navigate = useNavigate();

  const [email,     setEmail]     = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [codeSent,  setCodeSent]  = useState(false);
  const [error,     setError]     = useState('');

  const handleSendCode = async () => {
    setError('');

    if (!email.trim()) {
      setError('Please enter your email address.');
      return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      setError('Please enter a valid email address.');
      return;
    }

    setIsLoading(true);
    try {
      await forgotPassword(email.trim());
      setCodeSent(true);
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : 'Something went wrong. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <AuthBottomSheetLayout
      title="Forgot Password"
      subtitle="Please sign in to your existing account"
      onBack={() => navigate(-1)}
    >
      {codeSent ? (
        /* ── Success state ─────────────────────────────────────── */
        <div className="fp-success">
          <EnvelopeIcon />

          <h2 className="fp-success__title">Check your inbox</h2>

          <p className="fp-success__body">
            We've sent a password reset code to{' '}
            <span className="fp-success__email">{email}</span>.
            Please check your inbox and follow the instructions.
          </p>

          <PrimaryButton
            title="BACK TO LOG IN"
            onClick={() => navigate('/login')}
            className="fp__cta"
          />

          <button
            type="button"
            className="fp-success__resend-btn"
            onClick={() => { setCodeSent(false); setError(''); }}
          >
            Didn't receive it? Try again
          </button>
        </div>
      ) : (
        /* ── Form state ────────────────────────────────────────── */
        <>
          {/* Single email field */}
          <FormInput
            label="Email"
            placeholder="example@gmail.com"
            value={email}
            onChange={setEmail}
            type="email"
            autoComplete="email"
          />

          {/* Helper hint */}
          <p className="fp__hint">
            Enter the email address associated with your account and we'll
            send you a code to reset your password.
          </p>

          {/* Inline error */}
          {error && (
            <p className="fp__error" role="alert">{error}</p>
          )}

          {/* SEND CODE */}
          <PrimaryButton
            title="SEND CODE"
            onClick={handleSendCode}
            isLoading={isLoading}
            className="fp__cta"
          />
        </>
      )}
    </AuthBottomSheetLayout>
  );
};

export default ForgotPasswordScreen;
