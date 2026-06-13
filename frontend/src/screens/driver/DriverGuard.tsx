/**
 * DriverGuard.tsx
 *
 * Protects driver routes and ensures the user has ROLE_DRIVER.
 * Redirects to login if not authenticated, or to /app if wrong role.
 * Also provides the DriverAuthContext for the driver dashboard.
 */

import React from 'react';
import { Navigate } from 'react-router-dom';
import { getStoredUser } from '../../services/authService';
import { DriverAuthProvider } from '../../context/DriverAuthContext';
import { OfferProvider } from '../../context/OfferContext';

interface DriverGuardProps {
  children: React.ReactNode;
}

const DriverGuard: React.FC<DriverGuardProps> = ({ children }) => {
  const storedUser = getStoredUser();
  const isDriverRole = storedUser?.roles?.includes('ROLE_DRIVER') ?? false;

  // Not authenticated → redirect to login
  if (!storedUser) {
    return <Navigate to="/login" replace />;
  }

  // Authenticated but not a driver → redirect to home
  if (!isDriverRole) {
    return <Navigate to="/app" replace />;
  }

  return (
    <DriverAuthProvider>
      <OfferProvider>
        {children}
      </OfferProvider>
    </DriverAuthProvider>
  );
};

export default DriverGuard;
