import React, { useState } from 'react';
import { Link, Outlet, useParams } from 'react-router-dom';
import { useLocation } from '../contexts/LocationContext';

const LocationLayout: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const { location, logout } = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  const handleLogout = () => {
    closeMobileMenu();
    logout();
  };

  return (
    <div className="App">
      {/* Skip Navigation Link */}
      <a href="#main-content" className="skip-link" onClick={closeMobileMenu}>
        Skip to main content
      </a>
      
      <nav className="navbar" role="navigation" aria-label="Main navigation">
        <div className="nav-container">
          <div className="nav-logo">
            <span>Chicken Calculator</span>
            {location && (
              <span className="location-badge">{location.name}</span>
            )}
          </div>
          
          {/* Mobile menu button */}
          <button 
            className="mobile-menu-btn"
            onClick={toggleMobileMenu}
            aria-expanded={isMobileMenuOpen}
            aria-controls="nav-menu"
            aria-label={isMobileMenuOpen ? 'Close navigation menu' : 'Open navigation menu'}
          >
            <span className="hamburger"></span>
            <span className="hamburger"></span>
            <span className="hamburger"></span>
          </button>
          
          <ul className={`nav-menu ${isMobileMenuOpen ? 'nav-menu-open' : ''}`} id="nav-menu">
            <li className="nav-item">
              <Link to={`/${slug}/calculator`} className="nav-link" onClick={closeMobileMenu}>
                Calculator
              </Link>
            </li>
            <li className="nav-item">
              <Link to={`/${slug}/sales-data`} className="nav-link" onClick={closeMobileMenu}>
                Sales Data
              </Link>
            </li>
            <li className="nav-item">
              <Link to={`/${slug}/history`} className="nav-link" onClick={closeMobileMenu}>
                Marination History
              </Link>
            </li>
            <li className="nav-item nav-item-logout">
              <button onClick={handleLogout} className="nav-link nav-logout">
                Logout
              </button>
            </li>
          </ul>
        </div>
      </nav>

      <main className="main-content" id="main-content" tabIndex={-1}>
        <Outlet />
      </main>
    </div>
  );
};

export default LocationLayout;