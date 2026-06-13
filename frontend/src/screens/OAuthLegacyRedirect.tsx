import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';

/** Forwards legacy backend redirects (/oauth2/redirect) to the current callback route. */
const OAuthLegacyRedirect: React.FC = () => {
  const { search } = useLocation();
  return <Navigate to={`/auth/callback${search}`} replace />;
};

export default OAuthLegacyRedirect;
