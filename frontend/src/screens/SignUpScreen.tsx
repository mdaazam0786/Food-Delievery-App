import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AuthBottomSheetLayout from '../components/auth/AuthBottomSheetLayout';
import FormInput from '../components/auth/FormInput';
import PrimaryButton from '../components/auth/PrimaryButton';
import SocialLoginRow from '../components/auth/SocialLoginRow';
import { register, getPostLoginRoute } from '../services/authService';
import './SignUpScreen.css';

const SignUpScreen: React.FC = () => {
  const navigate = useNavigate();

  const [name,            setName]            = useState('');
  const [email,           setEmail]           = useState('');
  const [password,        setPassword]        = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordError,   setPasswordError]   = useState('');
  const [error,           setError]           = useState('');
  const [isLoading,       setIsLoading]       = useState(false);

  // ── Validation ────────────────────────────────────────────────────────────

  const validate = (): boolean => {
    setPasswordError('');
    setError('');

    if (!name.trim() || !email.trim() || !password || !confirmPassword) {
      setError('Please fill in all fields.');
      return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email.trim())) {
      setError('Please enter a valid email address.');
      return false;
    }

    if (password.length < 6) {
      setPasswordError('Password must be at least 6 characters.');
      return false;
    }

    if (password !== confirmPassword) {
      setPasswordError('Passwords do not match.');
      return false;
    }

    return true;
  };

  // ── Submit ────────────────────────────────────────────────────────────────

  const handleSignUp = async () => {
    if (!validate()) return;

    setIsLoading(true);
    try {
      const data = await register(name.trim(), email.trim(), password);
      if (data.mfaRequired) {
        setError('MFA is required. Please check your email for a one-time code.');
        return;
      }
      navigate(getPostLoginRoute(), { replace: true });
    } catch (err: unknown) {
      setError(
        err instanceof Error ? err.message : 'Something went wrong. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  // ── Render ────────────────────────────────────────────────────────────────

  return (
    <AuthBottomSheetLayout
      title="Sign Up"
      subtitle="Please sign up to get started"
      onBack={() => navigate(-1)}
    >
      {/* ── NAME ── */}
      <FormInput
        label="Name"
        placeholder="John Doe"
        value={name}
        onChange={setName}
        type="text"
        autoComplete="name"
      />

      {/* ── EMAIL ── */}
      <FormInput
        label="Email"
        placeholder="example@email.com"
        value={email}
        onChange={setEmail}
        type="email"
        autoComplete="email"
      />

      {/* ── PASSWORD ── */}
      <FormInput
        label="Password"
        placeholder="Enter your password"
        value={password}
        onChange={(val) => {
          setPassword(val);
          if (passwordError) setPasswordError('');
        }}
        type="password"
        autoComplete="new-password"
      />

      {/* ── RE-TYPE PASSWORD ── */}
      <FormInput
        label="Re-Type Password"
        placeholder="Confirm your password"
        value={confirmPassword}
        onChange={(val) => {
          setConfirmPassword(val);
          if (passwordError) setPasswordError('');
        }}
        type="password"
        autoComplete="new-password"
      />

      {/* ── Inline errors ── */}
      {passwordError && (
        <p className="su__error" role="alert">{passwordError}</p>
      )}
      {error && (
        <p className="su__error" role="alert">{error}</p>
      )}

      {/* ── SIGN UP button ── */}
      <PrimaryButton
        title="SIGN UP"
        onClick={handleSignUp}
        isLoading={isLoading}
        className="su__cta"
      />

      {/* ── Already have an account? ── */}
      <div className="su__login-row">
        <span className="su__login-prompt">Already have an account?&nbsp;</span>
        <button
          type="button"
          className="su__login-link"
          onClick={() => navigate('/login')}
        >
          LOG IN
        </button>
      </div>

      {/* ── Or divider ── */}
      <div className="login__or-row" aria-hidden="true">
        <span className="login__or-line" />
        <span className="login__or-text">Or</span>
        <span className="login__or-line" />
      </div>

      <SocialLoginRow />
    </AuthBottomSheetLayout>
  );
};

export default SignUpScreen;
