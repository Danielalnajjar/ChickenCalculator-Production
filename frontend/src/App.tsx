import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { LocationProvider } from './contexts/LocationContext';
import LocationLogin from './components/LocationLogin';
import RequireAuth from './components/RequireAuth';
import LocationLayout from './components/LocationLayout';
import ChickenCalculator from './components/ChickenCalculator';
import SalesDataManager from './components/SalesDataManager';
import MarinationHistory from './components/MarinationHistory';
import LandingPage from './components/LandingPage';
import ErrorBoundary from './components/ErrorBoundary';
import './App.css';

function App() {
  return (
    <ErrorBoundary>
      <Router>
        <Routes>
          {/* Landing page - list of locations */}
          <Route path="/" element={<LandingPage />} />
          
          {/* Admin portal routes (separate from location routes) */}
          <Route path="/admin/*" element={
            <div>Admin Portal - Loaded separately</div>
          } />
          
          {/* Location-specific routes */}
          <Route path="/:slug/*" element={
            <LocationProvider>
              <Routes>
                {/* Location login page */}
                <Route index element={<LocationLogin />} />
                
                {/* Protected location routes */}
                <Route element={<RequireAuth><LocationLayout /></RequireAuth>}>
                  <Route path="calculator" element={<ChickenCalculator />} />
                  <Route path="sales-data" element={<SalesDataManager />} />
                  <Route path="history" element={<MarinationHistory />} />
                </Route>
              </Routes>
            </LocationProvider>
          } />
          
          {/* Catch all - redirect to landing */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </ErrorBoundary>
  );
}

export default App;