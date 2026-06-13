import React, { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getToken, getDestinationByRole } from '../services/authService';
import './SplashScreen.css';

const SplashScreen: React.FC = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const timer = setTimeout(() => {
      const token = getToken();
      if (token) {
        navigate(getDestinationByRole(), { replace: true });
      } else {
        navigate('/onboarding', { replace: true });
      }
    }, 2000);

    return () => clearTimeout(timer);
  }, [navigate]);

  return (
    <div className="splash">
      <div className="splash__viewport">
        <div className="splash__center">

          {/* ── Logo ── */}
          <div className="splash__logo-row">

            {/* "F" */}
            <span className="splash__letter">F</span>

            {/* "oo" group: cloche + rings + motion lines */}
            <div className="splash__oo-group">

              {/* Cloche icon */}
              <div className="splash__cloche" aria-hidden="true">
                <div className="splash__cloche-dome" />
                <div className="splash__cloche-handle" />
                <div className="splash__cloche-base" />
              </div>

              {/* The two "o" letters rendered as platter rings */}
              <div className="splash__oo-letters">
                <span className="splash__ring" aria-hidden="true" />
                <span className="splash__ring" aria-hidden="true" />
              </div>

              {/* Speed / motion lines */}
              <div className="splash__motion-lines" aria-hidden="true">
                <span className="splash__line splash__line--long" />
                <span className="splash__line splash__line--short" />
                <span className="splash__line splash__line--medium" />
              </div>
            </div>

            {/* "d" */}
            <span className="splash__letter">d</span>
          </div>

          {/* Tagline */}
          <p className="splash__tagline">Delicious food, delivered fast</p>
        </div>
      </div>
    </div>
  );
};

export default SplashScreen;
