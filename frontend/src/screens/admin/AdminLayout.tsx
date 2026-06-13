import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { logout } from '../../services/authService';
import { useRestaurant } from '../../context/useRestaurant';
import './AdminLayout.css';

// ─── Sidebar Icons ────────────────────────────────────────────────────────────

const LogoIcon = () => (
  <svg width="36" height="36" viewBox="0 0 36 36" fill="none" aria-hidden="true">
    <path d="M4 22 Q4 10 18 10 Q32 10 32 22 Z" fill="white"/>
    <rect x="2" y="22" width="32" height="4" rx="2" fill="white"/>
    <circle cx="18" cy="10" r="3" fill="white"/>
    <circle cx="18" cy="10" r="1.5" fill="#FF7622"/>
    <path d="M12 7 Q13 4 12 1" stroke="#FF7622" strokeWidth="1.5" strokeLinecap="round" fill="none"/>
    <path d="M18 6 Q19 3 18 0" stroke="#FF7622" strokeWidth="1.5" strokeLinecap="round" fill="none"/>
    <path d="M24 7 Q25 4 24 1" stroke="#FF7622" strokeWidth="1.5" strokeLinecap="round" fill="none"/>
  </svg>
);

const DashboardIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <rect x="3" y="3" width="7" height="7" rx="1.5"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8"/>
    <rect x="14" y="3" width="7" height="7" rx="1.5"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8"/>
    <rect x="3" y="14" width="7" height="7" rx="1.5"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8"/>
    <rect x="14" y="14" width="7" height="7" rx="1.5"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8"/>
  </svg>
);

const OrdersIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8" strokeLinecap="round"/>
    <rect x="9" y="3" width="6" height="4" rx="1"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8"/>
    <line x1="9" y1="12" x2="15" y2="12"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8" strokeLinecap="round"/>
    <line x1="9" y1="16" x2="13" y2="16"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8" strokeLinecap="round"/>
  </svg>
);

const MenuNavIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="12" cy="12" r="9"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8"/>
    <path d="M8 12 Q8 8 12 8 Q16 8 16 12"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8" strokeLinecap="round"/>
    <line x1="6" y1="15" x2="18" y2="15"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8" strokeLinecap="round"/>
    <circle cx="12" cy="8" r="1.5" fill={active ? '#FF7622' : '#8B8FA8'}/>
  </svg>
);

const AnalyticsIcon = ({ active }: { active: boolean }) => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"
      stroke={active ? '#FF7622' : '#8B8FA8'} strokeWidth="1.8"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const SearchIcon = () => (
  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="11" cy="11" r="8" stroke="#9EA1B1" strokeWidth="2"/>
    <path d="M21 21l-4.35-4.35" stroke="#9EA1B1" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

const ChevronDownIcon = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M6 9l6 6 6-6" stroke="#6B7280" strokeWidth="2"
      strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

const BellIcon = () => (
  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"
      stroke="#6B7280" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M13.73 21a2 2 0 01-3.46 0"
      stroke="#6B7280" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);

/** Sign-out / exit icon shown inside the dropdown */
const SignOutIcon = () => (
  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <polyline points="16 17 21 12 16 7"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    <line x1="21" y1="12" x2="9" y2="12"
      stroke="currentColor" strokeWidth="2" strokeLinecap="round"/>
  </svg>
);

// ─── Generic avatar SVG ───────────────────────────────────────────────────────

const AdminAvatar = () => (
  <svg width="36" height="36" viewBox="0 0 36 36" fill="none" aria-hidden="true">
    <circle cx="18" cy="18" r="18" fill="#FF7622" opacity="0.15"/>
    <circle cx="18" cy="14" r="6" fill="#FF7622"/>
    <path d="M6 30 Q6 22 18 22 Q30 22 30 30" fill="#FF7622" opacity="0.7"/>
  </svg>
);

// ─── Nav config ───────────────────────────────────────────────────────────────

interface NavItem {
  label: string;
  path:  string;
  Icon:  React.FC<{ active: boolean }>;
}

const NAV_ITEMS: NavItem[] = [
  { label: 'Dashboard',        path: '/admin',           Icon: DashboardIcon },
  { label: 'Order Management', path: '/admin/orders',    Icon: OrdersIcon    },
  { label: 'Menu Management',  path: '/admin/menu',      Icon: MenuNavIcon   },
  { label: 'Analytics',        path: '/admin/analytics', Icon: AnalyticsIcon },
];

// ─── Helper: derive display name from profile ─────────────────────────────────

function displayName(profile: { fullName?: string; username?: string } | null): string {
  if (!profile) return '';
  if (profile.fullName?.trim()) return profile.fullName.trim();
  if (!profile.username) return '';
  return profile.username.charAt(0).toUpperCase() + profile.username.slice(1);
}

// ─── Layout ───────────────────────────────────────────────────────────────────

interface AdminLayoutProps {
  children: React.ReactNode;
}

const AdminLayout: React.FC<AdminLayoutProps> = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();

  const [profileOpen, setProfileOpen]   = useState(false);
  const [signingOut,  setSigningOut]    = useState(false);

  // ── Pull profile from shared context (no extra fetch) ───────────────────
  const { userProfile, profileLoading } = useRestaurant();

  const dropdownRef = useRef<HTMLDivElement>(null);
  const currentPath = location.pathname;

  // ── Close dropdown on outside click ─────────────────────────────────────
  useEffect(() => {
    if (!profileOpen) return;
    const handler = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setProfileOpen(false);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [profileOpen]);

  // ── Sign-out handler ─────────────────────────────────────────────────────
  const handleSignOut = async () => {
    setProfileOpen(false);
    setSigningOut(true);
    try {
      await logout(); // POST /api/v1/auth/logout + clearSession()
    } finally {
      setSigningOut(false);
      navigate('/login', { replace: true });
    }
  };

  const name = displayName(userProfile);

  return (
    <div className="adm-shell">

      {/* ── Sidebar ── */}
      <aside className="adm-sidebar" aria-label="Admin navigation">

        <div className="adm-sidebar__logo">
          <div className="adm-sidebar__logo-icon"><LogoIcon /></div>
          <span className="adm-sidebar__logo-text">Foodzie</span>
        </div>

        <nav className="adm-sidebar__nav" aria-label="Main navigation">
          {NAV_ITEMS.map((item) => {
            const isActive =
              currentPath === item.path ||
              (item.path !== '/admin' && currentPath.startsWith(item.path));
            return (
              <button
                key={item.path}
                className={`adm-nav-item${isActive ? ' adm-nav-item--active' : ''}`}
                onClick={() => navigate(item.path)}
                aria-current={isActive ? 'page' : undefined}
              >
                <span className="adm-nav-item__bar" aria-hidden="true"/>
                <span className="adm-nav-item__icon">
                  <item.Icon active={isActive} />
                </span>
                <span className="adm-nav-item__label">{item.label}</span>
              </button>
            );
          })}
        </nav>

        <div className="adm-sidebar__footer">
          <span className="adm-sidebar__version">v1.0.0</span>
        </div>
      </aside>

      {/* ── Main area ── */}
      <div className="adm-main">

        {/* ── Top header bar ── */}
        <header className="adm-topbar">

          {/* Left: fixed greeting */}
          <div className="adm-topbar__left">
            <h2 className="adm-topbar__greeting">
              Welcome Back, {profileLoading ? 'Admin' : (name || 'Admin')}
            </h2>
          </div>

          {/* Centre: search */}
          <div className="adm-topbar__center">
            <div className="adm-topbar__search" role="search">
              <SearchIcon />
              <input
                className="adm-topbar__search-input"
                type="search"
                placeholder="Search orders, menu items…"
                aria-label="Global search"
              />
            </div>
          </div>

          {/* Right: bell + profile */}
          <div className="adm-topbar__right">

            <button className="adm-topbar__icon-btn" aria-label="Notifications">
              <BellIcon />
              <span className="adm-topbar__notif-dot" aria-hidden="true"/>
            </button>

            {/* Profile block */}
            <div className="adm-profile" ref={dropdownRef}>
              <button
                className="adm-profile__trigger"
                onClick={() => setProfileOpen((p) => !p)}
                aria-haspopup="true"
                aria-expanded={profileOpen}
                aria-label="Open profile menu"
                disabled={signingOut}
              >
                {/* Circular generic avatar */}
                <div className="adm-profile__avatar">
                  <AdminAvatar />
                </div>

                {/* Name — skeleton while loading */}
                <div className="adm-profile__info">
                  {profileLoading ? (
                    <>
                      <span className="adm-skeleton adm-skeleton--name" aria-hidden="true"/>
                      <span className="adm-skeleton adm-skeleton--role" aria-hidden="true"/>
                    </>
                  ) : (
                    <>
                      <span className="adm-profile__name">{name || 'Admin'}</span>
                      <span className="adm-profile__role">Restaurant Admin</span>
                    </>
                  )}
                </div>

                <ChevronDownIcon />
              </button>

              {/* Dropdown — sign out only */}
              {profileOpen && (
                <div className="adm-profile__dropdown" role="menu">
                  <button
                    className="adm-profile__menu-item adm-profile__menu-item--signout"
                    role="menuitem"
                    onClick={handleSignOut}
                    disabled={signingOut}
                    aria-busy={signingOut}
                  >
                    <SignOutIcon />
                    {signingOut ? 'Signing out…' : 'Sign Out'}
                  </button>
                </div>
              )}
            </div>

          </div>
        </header>

        {/* Page content */}
        <main className="adm-content">
          {children}
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
