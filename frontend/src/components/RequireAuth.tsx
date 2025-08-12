import React from 'react';
import { Navigate, useParams } from 'react-router-dom';
import { useLocation } from '../contexts/LocationContext';

interface RequireAuthProps {
  children: React.ReactNode;
}

const RequireAuth: React.FC<RequireAuthProps> = ({ children }) => {
  const { slug } = useParams<{ slug: string }>();
  const { isAuthenticated, isLoading, location } = useLocation();

  // Show loading state while checking auth
  if (isLoading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner">Loading...</div>
      </div>
    );
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated) {
    return <Navigate to={`/${slug}`} replace />;
  }

  // Render children if authenticated
  return <>{children}</>;
};

export default RequireAuth;