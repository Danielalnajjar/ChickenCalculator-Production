import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import ChickenCalculator from './components/ChickenCalculator';
import SalesDataManager from './components/SalesDataManager';
import MarinationHistory from './components/MarinationHistory';
import ErrorBoundary from './components/ErrorBoundary';
import './App.css';

function App() {
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen(!isMobileMenuOpen);
  };

  const closeMobileMenu = () => {
    setIsMobileMenuOpen(false);
  };

  return (
    <ErrorBoundary>
      <Router>
        <div className="App">
        {/* Skip Navigation Link */}
        <a href="#main-content" className="skip-link" onClick={closeMobileMenu}>
          Skip to main content
        </a>
        
        <nav className="navbar" role="navigation" aria-label="Main navigation">
          <div className="nav-container">
            <Link to="/" className="nav-logo" onClick={closeMobileMenu}>
              Chicken Calculator
            </Link>
            
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
                <Link to="/" className="nav-link" onClick={closeMobileMenu}>
                  Calculator
                </Link>
              </li>
              <li className="nav-item">
                <Link to="/sales-data" className="nav-link" onClick={closeMobileMenu}>
                  Sales Data
                </Link>
              </li>
              <li className="nav-item">
                <Link to="/history" className="nav-link" onClick={closeMobileMenu}>
                  Marination History
                </Link>
              </li>
            </ul>
          </div>
        </nav>

        <main className="main-content" id="main-content" tabIndex={-1}>
          <Routes>
            <Route path="/" element={<ChickenCalculator />} />
            <Route path="/sales-data" element={<SalesDataManager />} />
            <Route path="/history" element={<MarinationHistory />} />
          </Routes>
        </main>
      </div>
    </Router>
    </ErrorBoundary>
  );
}

export default App;