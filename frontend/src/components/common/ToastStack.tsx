/**
 * ToastStack.tsx — Renders toast notifications in the bottom-right corner.
 */

import React from 'react';
import { useToast } from '../../context/ToastContext';
import './ToastStack.css';

// ─── Icons ────────────────────────────────────────────────────────────────────

const SuccessIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#22C55E" opacity="0.15"/>
    <polyline points="8 12 11 15 16 9" stroke="#22C55E" strokeWidth="2.2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const ErrorIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#EF4444" opacity="0.15"/>
    <line x1="15" y1="9" x2="9" y2="15" stroke="#EF4444" strokeWidth="2.2" strokeLinecap="round"/>
    <line x1="9" y1="9" x2="15" y2="15" stroke="#EF4444" strokeWidth="2.2" strokeLinecap="round"/>
  </svg>
);

const WarningIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#F59E0B" opacity="0.15"/>
    <line x1="12" y1="8" x2="12" y2="13" stroke="#F59E0B" strokeWidth="2.2" strokeLinecap="round"/>
    <circle cx="12" cy="16.5" r="1.2" fill="#F59E0B"/>
  </svg>
);

const InfoIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="10" fill="#3B82F6" opacity="0.15"/>
    <line x1="12" y1="8" x2="12" y2="13" stroke="#3B82F6" strokeWidth="2.2" strokeLinecap="round"/>
    <circle cx="12" cy="16.5" r="1.2" fill="#3B82F6"/>
  </svg>
);

const CloseIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <line x1="18" y1="6" x2="6" y2="18" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"/>
    <line x1="6" y1="6" x2="18" y2="18" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round"/>
  </svg>
);

// ─── Component ────────────────────────────────────────────────────────────────

const ToastStack: React.FC = () => {
  const { toasts, removeToast } = useToast();

  if (toasts.length === 0) return null;

  return (
    <div className="ts-stack" role="region" aria-live="polite" aria-atomic="false">
      {toasts.map((toast) => (
        <div
          key={toast.id}
          className={`ts-toast ts-toast--${toast.type}`}
          role="status"
        >
          <div className="ts-toast__icon">
            {toast.type === 'success' && <SuccessIcon />}
            {toast.type === 'error' && <ErrorIcon />}
            {toast.type === 'warning' && <WarningIcon />}
            {toast.type === 'info' && <InfoIcon />}
          </div>

          <span className="ts-toast__message">{toast.message}</span>

          <button
            className="ts-toast__close"
            onClick={() => removeToast(toast.id)}
            aria-label="Dismiss notification"
          >
            <CloseIcon />
          </button>
        </div>
      ))}
    </div>
  );
};

export default ToastStack;
