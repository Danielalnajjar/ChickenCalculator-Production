import React, { createContext, useContext, useState, useEffect } from 'react';
import apiService, { TOKEN_KEY } from '../services/api';

interface User {
  id: string;
  email: string;
  role: 'admin' | 'manager';
  name: string;
  passwordChangeRequired?: boolean;
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
    // Check authentication status via cookie (no direct access to httpOnly cookie)
    // Attempt to validate token with backend to check if user is logged in
    validateToken();
  }, []);

  const validateToken = async () => {
    try {
      const response = await apiService.validateToken();
      
      if (response.ok && response.data) {
        const data = response.data as any;
        const userData: User = {
          id: data.id || '',
          email: data.email || '',
          name: data.name || '',
          role: (data.role as 'admin' | 'manager') || 'admin',
          passwordChangeRequired: data.passwordChangeRequired || false
        };
        setUser(userData);
      } else {
        // Authentication failed - user is not logged in
        setUser(null);
      }
    } catch (error) {
      console.error('Token validation error:', error);
      setUser(null);
    } finally {
      setIsLoading(false);
    }
  };

  const login = async (email: string, password: string): Promise<boolean> => {
    try {
      const response = await apiService.login(email, password);

      if (response.ok && response.data) {
        const data = response.data as any;
        // httpOnly cookie is set automatically by the server
        // Extract user info from response
        const userData: User = {
          id: data.id || '',
          email: data.email || email,
          name: data.name || '',
          role: (data.role as 'admin' | 'manager') || 'admin',
          passwordChangeRequired: data.passwordChangeRequired || false
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
      // Notify backend about logout (clears httpOnly cookie)
      await apiService.logout();
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      setUser(null);
      // httpOnly cookie is cleared by the server
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