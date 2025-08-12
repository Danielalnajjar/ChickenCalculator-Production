import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useLocation } from '../contexts/LocationContext';
import '../styles/LocationLogin.css';

const LocationLogin: React.FC = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { location, isAuthenticated, isLoading, error, login } = useLocation();
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [rememberLocation, setRememberLocation] = useState(false);

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated && location?.slug) {
      navigate(`/${location.slug}/calculator`);
    }
  }, [isAuthenticated, location, navigate]);

  // Handle form submission
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!password.trim()) {
      return;
    }

    setIsSubmitting(true);
    const success = await login(password);
    setIsSubmitting(false);

    if (success) {
      // Save location preference if checked
      if (rememberLocation) {
        localStorage.setItem('preferredLocation', slug || '');
      }
      navigate(`/${slug}/calculator`);
    } else {
      // Clear password on failed attempt
      setPassword('');
    }
  };

  // Show loading state
  if (isLoading) {
    return (
      <div className="location-login-container">
        <div className="loading-spinner">Loading...</div>
      </div>
    );
  }

  // Show error if location not found
  if (!location) {
    return (
      <div className="location-login-container">
        <div className="error-message">
          <h2>Location Not Found</h2>
          <p>The location you're trying to access doesn't exist.</p>
          <button onClick={() => navigate('/')} className="btn-primary">
            Go to Home
          </button>
        </div>
      </div>
    );
  }

  // Show login form
  return (
    <div className="location-login-container">
      <div className="login-card">
        <div className="login-header">
          <h1>Chicken Calculator</h1>
          <h2>{location.name}</h2>
        </div>

        <form onSubmit={handleSubmit} className="login-form">
          {error && (
            <div className="alert error" role="alert">
              {error}
            </div>
          )}

          <div className="form-group">
            <label htmlFor="password">
              <span className="label-text">Location Password</span>
            </label>
            <input
              type="password"
              id="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter location password"
              required
              disabled={isSubmitting}
              autoFocus
              autoComplete="current-password"
              aria-describedby={error ? 'error-message' : undefined}
            />
          </div>

          <div className="form-group checkbox-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={rememberLocation}
                onChange={(e) => setRememberLocation(e.target.checked)}
                disabled={isSubmitting}
              />
              <span>Remember this location</span>
            </label>
          </div>

          <button
            type="submit"
            className="btn-primary btn-login"
            disabled={isSubmitting || !password.trim()}
          >
            {isSubmitting ? 'Logging in...' : 'Login'}
          </button>
        </form>

        <div className="login-footer">
          <p className="help-text">
            Contact your manager if you've forgotten the password.
          </p>
          <a href="/" className="link-secondary">
            Back to Location List
          </a>
        </div>
      </div>
    </div>
  );
};

export default LocationLogin;