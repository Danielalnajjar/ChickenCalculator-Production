import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';

interface LocationInfo {
  slug: string;
  name: string;
  id: number;
  requiresAuth: boolean;
}

interface LocationContextType {
  location: LocationInfo | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  login: (password: string) => Promise<boolean>;
  logout: () => void;
  checkAuth: () => Promise<void>;
}

const LocationContext = createContext<LocationContextType | undefined>(undefined);

export const useLocation = () => {
  const context = useContext(LocationContext);
  if (!context) {
    throw new Error('useLocation must be used within LocationProvider');
  }
  return context;
};

interface LocationProviderProps {
  children: ReactNode;
}

export const LocationProvider: React.FC<LocationProviderProps> = ({ children }) => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const [location, setLocation] = useState<LocationInfo | null>(null);
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Check if location requires auth and validate session
  const checkAuth = async () => {
    if (!slug) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);

      // Check if location requires authentication
      const authRequiredRes = await axios.get(`/api/v1/location/${slug}/auth/required`);
      const locationData: LocationInfo = {
        slug,
        name: authRequiredRes.data.locationName,
        id: 0, // Will be set after successful auth
        requiresAuth: authRequiredRes.data.requiresAuth
      };
      setLocation(locationData);

      if (authRequiredRes.data.requiresAuth) {
        // Validate existing session
        try {
          const validateRes = await axios.get(`/api/v1/location/${slug}/auth/validate`);
          if (validateRes.data.valid) {
            setIsAuthenticated(true);
            setLocation({
              ...locationData,
              id: validateRes.data.locationId,
              name: validateRes.data.locationName
            });
          } else {
            setIsAuthenticated(false);
          }
        } catch {
          setIsAuthenticated(false);
        }
      } else {
        // No auth required for this location
        setIsAuthenticated(true);
      }
    } catch (err: any) {
      if (err.response?.status === 404) {
        setError('Location not found');
        navigate('/');
      } else {
        setError('Failed to load location information');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Login function
  const login = async (password: string): Promise<boolean> => {
    if (!slug) return false;

    try {
      setError(null);
      const response = await axios.post(`/api/v1/location/${slug}/auth/login`, {
        password
      });

      if (response.data.success) {
        setIsAuthenticated(true);
        await checkAuth(); // Refresh location data
        return true;
      }
      return false;
    } catch (err: any) {
      if (err.response?.status === 401) {
        setError('Invalid password');
      } else if (err.response?.status === 429) {
        setError('Too many failed attempts. Please try again later.');
      } else {
        setError('Login failed. Please try again.');
      }
      return false;
    }
  };

  // Logout function
  const logout = async () => {
    if (!slug) return;

    try {
      await axios.post(`/api/v1/location/${slug}/auth/logout`);
    } catch (err) {
      console.error('Logout error:', err);
    } finally {
      setIsAuthenticated(false);
      navigate(`/${slug}`);
    }
  };

  // Check auth on mount and when slug changes
  useEffect(() => {
    checkAuth();
  }, [slug]);

  // Set up axios interceptor to include location headers
  useEffect(() => {
    if (location?.id) {
      const interceptor = axios.interceptors.request.use((config) => {
        config.headers['X-Location-Id'] = location.id.toString();
        config.headers['X-Location-Slug'] = location.slug;
        config.headers['X-Location-Name'] = location.name;
        return config;
      });

      return () => {
        axios.interceptors.request.eject(interceptor);
      };
    }
  }, [location]);

  return (
    <LocationContext.Provider
      value={{
        location,
        isAuthenticated,
        isLoading,
        error,
        login,
        logout,
        checkAuth
      }}
    >
      {children}
    </LocationContext.Provider>
  );
};