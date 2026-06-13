import React from 'react';
import { useNavigate } from 'react-router-dom';
import './HomeSideDrawer.css';

interface HomeSideDrawerProps {
  open:    boolean;
  onClose: () => void;
}

const NAV = [
  { label: 'Home',     path: '/app'     },
  { label: 'Search',   path: '/search'  },
  { label: 'Profile',  path: '/profile' },
  { label: 'Settings', path: '/app'     },
];

const HomeSideDrawer: React.FC<HomeSideDrawerProps> = ({ open, onClose }) => {
  const navigate = useNavigate();

  const go = (path: string) => {
    onClose();
    navigate(path);
  };

  return (
    <>
      <div
        className={`hf-drawer__backdrop${open ? ' hf-drawer__backdrop--visible' : ''}`}
        onClick={onClose}
        aria-hidden={!open}
      />
      <nav
        className={`hf-drawer${open ? ' hf-drawer--open' : ''}`}
        aria-label="Main navigation"
        aria-hidden={!open}
      >
        <div className="hf-drawer__header">
          <span className="hf-drawer__brand">Foodzie</span>
          <button className="hf-drawer__close" onClick={onClose} aria-label="Close menu">
            ×
          </button>
        </div>
        <ul className="hf-drawer__list">
          {NAV.map((item) => (
            <li key={item.label}>
              <button className="hf-drawer__link" onClick={() => go(item.path)}>
                {item.label}
              </button>
            </li>
          ))}
        </ul>
      </nav>
    </>
  );
};

export default HomeSideDrawer;
