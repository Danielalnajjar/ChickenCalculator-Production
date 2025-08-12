import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import axios from 'axios';
import '../styles/LandingPage.css';

interface Location {
  id: number;
  name: string;
  slug: string;
  status: string;
}

const LandingPage: React.FC = () => {
  const [locations, setLocations] = useState<Location[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadLocations();
    
    // Check for preferred location
    const preferredLocation = localStorage.getItem('preferredLocation');
    if (preferredLocation) {
      window.location.href = `/${preferredLocation}`;
    }
  }, []);

  const loadLocations = async () => {
    try {
      const response = await axios.get('/api/v1/calculator/locations');
      const activeLocations = response.data.filter(
        (loc: Location) => loc.status === 'ACTIVE'
      );
      setLocations(activeLocations);
    } catch (err) {
      setError('Failed to load locations');
      console.error('Error loading locations:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="landing-container">
        <div className="loading-message">Loading locations...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="landing-container">
        <div className="error-message">
          <h2>Error</h2>
          <p>{error}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="landing-container">
      <div className="landing-content">
        <div className="landing-header">
          <h1>Chicken Calculator</h1>
          <p className="subtitle">Select your location to continue</p>
        </div>

        {locations.length === 0 ? (
          <div className="no-locations">
            <p>No locations available at this time.</p>
            <p>Please contact your administrator.</p>
          </div>
        ) : (
          <div className="locations-grid">
            {locations.map((location) => (
              <Link
                key={location.id}
                to={`/${location.slug}`}
                className="location-card"
              >
                <div className="location-card-content">
                  <h3>{location.name}</h3>
                  <span className="location-arrow">→</span>
                </div>
              </Link>
            ))}
          </div>
        )}

        <div className="landing-footer">
          <Link to="/admin" className="admin-link">
            Admin Portal
          </Link>
          <p className="copyright">
            © 2024 Your Company. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  );
};

export default LandingPage;