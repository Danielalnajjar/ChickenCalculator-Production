import React, { createContext, useContext, useState, useEffect } from 'react';
import apiService, { TOKEN_KEY } from '../services/api';

interface User {
  id: string;
  email: string;
  role: 'admin' | 'manager';
  name: string;
}

interface AuthContextType {
  user: User | null;
  login: (email: string, password: string) => Promise<boolean>;
  logout: () => void;
  isLoading: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // Check for stored authentication token (not user data)
    const storedToken = sessionStorage.getItem(TOKEN_KEY);
    if (storedToken) {
      // Validate token with backend
      validateToken(storedToken);
    } else {
      setIsLoading(false);
    }
  }, []);

  const validateToken = async (token: string) => {
    try {
      const response = await apiService.validateToken();
      
      if (response.ok && response.data) {
        setUser(response.data as User);
      } else {
        sessionStorage.removeItem(TOKEN_KEY);
      }
    } catch (error) {
      console.error('Token validation error:', error);
      sessionStorage.removeItem(TOKEN_KEY);
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await apiService.login(email, password);

      if (response.ok && response.data) {
        const data = response.data as any;
        // Store only the token, not user data
        if (data.token) {
          sessionStorage.setItem(TOKEN_KEY, data.token);
        }
        // Extract user info from response
        const userData: User = {
          id: data.id || '',
          email: data.email || email,
          name: data.name || '',
          role: (data.role as 'admin' | 'manager') || 'admin'
        };
        setUser(userData);
        return true;
      }
      return false;
    } catch (error) {
      console.error('Login error:', error);
      return false;
    }
  };

  const logout = async () => {
    try {
      // Notify backend about logout
      await apiService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      sessionStorage.removeItem(TOKEN_KEY);
    }
  };

  const value: AuthContextType = {
    user,
    login,
    logout,
    isLoading,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};