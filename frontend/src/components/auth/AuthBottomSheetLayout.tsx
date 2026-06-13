import React from 'react';
import './AuthBottomSheetLayout.css';

interface AuthBottomSheetLayoutProps {
  title: string;
  subtitle: string;
  onBack: () => void;
  children: React.ReactNode;
}

const AuthBottomSheetLayout: React.FC<AuthBottomSheetLayoutProps> = ({
  title,
  subtitle,
  onBack,
  children,
}) => {
  return (
    <div className="auth-layout">
      <div className="auth-layout__viewport">

        {/* ── Dark header ── */}
        <div className="auth-layout__top">
          {/* Circular white back button */}
          <button
            className="auth-layout__back-btn"
            onClick={onBack}
            aria-label="Go back"
          >
            {/* Inline SVG chevron-left for crisp rendering at any size */}
            <svg
              className="auth-layout__back-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </button>

          {/* Title & subtitle */}
          <div className="auth-layout__header-text">
            <h1 className="auth-layout__title">{title}</h1>
            <p className="auth-layout__subtitle">{subtitle}</p>
          </div>
        </div>

        {/* ── White card sheet ── */}
        <div className="auth-layout__sheet">
          <div className="auth-layout__scroll-content">
            {children}
          </div>
        </div>

      </div>
    </div>
  );
};

export default AuthBottomSheetLayout;
