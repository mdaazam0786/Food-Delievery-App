/**
 * AdminGuard.tsx
 *
 * Intercepts ROLE_RESTAURANT owners who have not completed provisioning
 * and renders the full-page onboarding wizard instead of the dashboard.
 */

import React, { Suspense, lazy } from 'react';
import { Navigate } from 'react-router-dom';
import { getStoredUser } from '../../services/authService';
import { useRestaurant } from '../../context/useRestaurant';

const RestaurantProvisioningScreen = lazy(() => import('./RestaurantProvisioningScreen'));

const FullPageSpinner: React.FC = () => (
  <div style={{
    display: 'flex', flexDirection: 'column', alignItems: 'center',
    justifyContent: 'center', height: '100vh', width: '100vw',
    background: '#f0f2f8', gap: 16,
  }}>
    <svg
      width="48" height="48" viewBox="0 0 50 50" fill="none"
      style={{ animation: 'guard-spin 0.9s linear infinite' }}
      aria-label="Loading" role="status"
    >
      <style>{`@keyframes guard-spin { to { transform: rotate(360deg); } }`}</style>
      <circle cx="25" cy="25" r="20" stroke="#FF7622" strokeWidth="4"
        strokeLinecap="round" strokeDasharray="100" strokeDashoffset="60"/>
    </svg>
    <span style={{ fontSize: 14, color: '#9EA1B1', fontWeight: 500 }}>
      Loading your workspace…
    </span>
  </div>
);

const ErrorScreen: React.FC<{ message: string }> = ({ message }) => (
  <div style={{
    display: 'flex', flexDirection: 'column', alignItems: 'center',
    justifyContent: 'center', height: '100vh', width: '100vw',
    background: '#f0f2f8', gap: 12, padding: 32, textAlign: 'center',
  }}>
    <p style={{ fontSize: 15, fontWeight: 700, color: '#1E2229', margin: 0 }}>
      Something went wrong
    </p>
    <p style={{ fontSize: 13, color: '#9EA1B1', margin: 0, maxWidth: 360 }}>
      {message}
    </p>
    <button
      onClick={() => window.location.reload()}
      style={{
        marginTop: 8, padding: '10px 24px', background: '#FF7622', color: '#fff',
        border: 'none', borderRadius: 10, fontWeight: 700, fontSize: 13,
        cursor: 'pointer',
      }}
    >
      Retry
    </button>
  </div>
);

interface AdminGuardProps {
  children: React.ReactNode;
}

const AdminGuard: React.FC<AdminGuardProps> = ({ children }) => {
  const { profileLoading, noRestaurant, profileError, activeRestaurantId, isProvisioned } = useRestaurant();
  const storedUser = getStoredUser();
  const isRestaurantRole = storedUser?.roles?.includes('ROLE_RESTAURANT') ?? false;

  // Not authenticated or wrong role
  if (!storedUser) {
    return <Navigate to="/login" replace />;
  }

  if (!isRestaurantRole) {
    return <Navigate to="/app" replace />;
  }

  // Only block on the initial load — when we have no state at all yet.
  // If activeRestaurantId is already set (e.g. after provisioning + navigate),
  // skip the spinner and go straight to the dashboard even if a background
  // refetch is in progress.
  if (profileLoading && !activeRestaurantId && !profileError) {
    return <FullPageSpinner />;
  }

  if (profileError) {
    return <ErrorScreen message={profileError} />;
  }

  // Check if restaurant exists and is properly provisioned
  // Show provisioning screen if:
  // 1. No restaurant exists (noRestaurant is true), OR
  // 2. No activeRestaurantId, OR
  // 3. Restaurant is not fully provisioned yet
  if (noRestaurant || !activeRestaurantId || !isProvisioned) {
    return (
      <Suspense fallback={<FullPageSpinner />}>
        <RestaurantProvisioningScreen />
      </Suspense>
    );
  }

  return <>{children}</>;
};

export default AdminGuard;
