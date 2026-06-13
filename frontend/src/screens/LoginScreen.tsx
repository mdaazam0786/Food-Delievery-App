import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import AuthBottomSheetLayout from '../components/auth/AuthBottomSheetLayout';
import FormInput from '../components/auth/FormInput';
import PrimaryButton from '../components/auth/PrimaryButton';
import SocialLoginRow from '../components/auth/SocialLoginRow';
import { login, getPostLoginRoute } from '../services/authService';
import './LoginScreen.css';

const LoginScreen: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [email, setEmail]         = useState('');
  const [password, setPassword]   = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError]         = useState('');

  // Surface OAuth failure messages redirected from auth-service
  useEffect(() => {
    const oauthError = searchParams.get('error');
    if (oauthError) {
      setError(oauthError);
      setSearchParams({}, { replace: true });
    }
  }, [searchParams, setSearchParams]);

  const handleLogin = async () => {
    setError('');
    if (!email.trim() || !password.trim()) {
      setError('Please enter your email and password.');
      return;
    }
    setIsLoading(true);
    try {
      const data = await login(email.trim(), password);
      if (data.mfaRequired) {
        // MFA flow — store challenge token and redirect to MFA screen (future)
        // For now surface a clear message
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

  return (
    <AuthBottomSheetLayout
      title="Log In"
      subtitle="Please sign in to your existing account"
      onBack={() => navigate(-1)}
    >
      {/* ── Email ── */}
      <FormInput
        label="Email"
        placeholder="example@email.com"
        value={email}
        onChange={setEmail}
        type="email"
        autoComplete="email"
      />

      {/* ── Password ── */}
      <FormInput
        label="Password"
        placeholder="Enter your password"
        value={password}
        onChange={setPassword}
        type="password"
        autoComplete="current-password"
      />

      {/* ── Remember me + Forgot password ── */}
      <div className="login__controls-row">
        {/* Custom checkbox */}
        <label className="login__remember-label">
          <input
            type="checkbox"
            className="login__checkbox-native"
            checked={rememberMe}
            onChange={(e) => setRememberMe(e.target.checked)}
          />
          <span className={`login__checkbox-box${rememberMe ? ' login__checkbox-box--on' : ''}`}>
            {rememberMe && (
              <svg width="11" height="11" viewBox="0 0 12 12" fill="none" aria-hidden="true">
                <polyline points="2,6 5,9 10,3" stroke="white" strokeWidth="2"
                  strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            )}
          </span>
          <span className="login__remember-text">Remember me</span>
        </label>

        <button
          type="button"
          className="login__forgot-btn"
          onClick={() => navigate('/forgot-password')}
        >
          Forgot Password
        </button>
      </div>

      {/* ── Inline error ── */}
      {error && (
        <p className="login__error" role="alert">{error}</p>
      )}

      {/* ── LOG IN button ── */}
      <PrimaryButton
        title="LOG IN"
        onClick={handleLogin}
        isLoading={isLoading}
        className="login__cta"
      />

      {/* ── Sign up prompt ── */}
      <div className="login__signup-row">
        <span className="login__signup-prompt">Don't have an account?&nbsp;</span>
        <button
          type="button"
          className="login__signup-link"
          onClick={() => navigate('/register')}
        >
          SIGN UP
        </button>
      </div>

      {/* ── Or divider ── */}
      <div className="login__or-row" aria-hidden="true">
        <span className="login__or-line" />
        <span className="login__or-text">Or</span>
        <span className="login__or-line" />
      </div>

      {/* ── Social buttons ── */}
      <SocialLoginRow />
    </AuthBottomSheetLayout>
  );
};

export default LoginScreen;
