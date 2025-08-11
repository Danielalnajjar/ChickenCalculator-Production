import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import ChickenCalculator from './components/ChickenCalculator';
import SalesDataManager from './components/SalesDataManager';
import MarinationHistory from './components/MarinationHistory';
import ErrorBoundary from './components/ErrorBoundary';
import './App.css';

function App() {
  return (
    <ErrorBoundary>
      <Router>
        <div className="App">
        <nav className="navbar">
          <div className="nav-container">
            <Link to="/" className="nav-logo">
              Chicken Calculator
            </Link>
            <ul className="nav-menu">
              <li className="nav-item">
                <Link to="/" className="nav-link">
                  Calculator
                </Link>
              </li>
              <li className="nav-item">
                <Link to="/sales-data" className="nav-link">
                  Sales Data
                </Link>
              </li>
              <li className="nav-item">
                <Link to="/history" className="nav-link">
                  Marination History
                </Link>
              </li>
            </ul>
          </div>
        </nav>

        <main className="main-content">
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