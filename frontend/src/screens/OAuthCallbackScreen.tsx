import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getPostLoginRoute, persistOAuthSession } from '../services/authService';
import './OAuthRedirectScreen.css';

/**
 * Landing page after a successful OAuth2 login.
 *
 * auth-service redirects here with ?token=…&refreshToken=… query params.
 * Route is /auth/callback (NOT /oauth2/redirect) so Vite does not proxy
 * this path to the backend.
 */
const OAuthCallbackScreen: React.FC = () => {
  const navigate       = useNavigate();
  const [searchParams] = useSearchParams();

  useEffect(() => {
    const token        = searchParams.get('token');
    const refreshToken = searchParams.get('refreshToken');
    const error        = searchParams.get('error');

    if (error) {
      navigate(`/login?error=${encodeURIComponent(error)}`, { replace: true });
      return;
    }

    if (!token || !refreshToken) {
      navigate('/login?error=OAuth+sign-in+did+not+return+tokens', { replace: true });
      return;
    }

    try {
      persistOAuthSession(token, refreshToken);
      navigate(getPostLoginRoute(), { replace: true });
    } catch {
      navigate('/login?error=Failed+to+complete+OAuth+sign-in', { replace: true });
    }
  }, [navigate, searchParams]);

  return (
    <div className="oauth-redirect" role="status" aria-live="polite">
      <div className="oauth-redirect__spinner" aria-hidden="true"/>
      <p className="oauth-redirect__text">Completing sign-in…</p>
    </div>
  );
};

export default OAuthCallbackScreen;
