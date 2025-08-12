import React, { ReactElement } from 'react';
import { render, RenderOptions } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from '../../contexts/AuthContext';

// Mock auth context values
export const mockAuthContextValue = {
  user: null,
  token: null,
  login: jest.fn(),
  logout: jest.fn(),
  isAuthenticated: false,
  isLoading: false,
};

// Mock authenticated auth context values
export const mockAuthenticatedContextValue = {
  ...mockAuthContextValue,
  user: {
    id: 1,
    email: 'admin@test.com',
    name: 'Test Admin',
    role: 'ADMIN' as const,
  },
  token: 'mock-jwt-token',
  isAuthenticated: true,
};

// Custom render function that includes providers
interface CustomRenderOptions extends Omit<RenderOptions, 'wrapper'> {
  authContextValue?: typeof mockAuthContextValue;
  initialEntries?: string[];
}

export function renderWithProviders(
  ui: ReactElement,
  {
    authContextValue = mockAuthContextValue,
    initialEntries = ['/'],
    ...renderOptions
  }: CustomRenderOptions = {}
) {
  function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <BrowserRouter>
        <AuthProvider value={authContextValue}>
          {children}
        </AuthProvider>
      </BrowserRouter>
    );
  }

  return render(ui, { wrapper: Wrapper, ...renderOptions });
}

// Mock API responses
export const mockApiResponses = {
  locations: {
    success: {
      ok: true,
      data: [
        {
          id: '1',
          name: 'Test Restaurant',
          domain: 'test.chickencalc.com',
          status: 'active',
          lastSeen: '2024-01-15T10:30:00Z',
          managerEmail: 'manager@test.com',
          cloudProvider: 'AWS',
          region: 'us-east-1',
          createdAt: '2024-01-01T00:00:00Z'
        }
      ],
      error: null
    },
    error: {
      ok: false,
      data: null,
      error: 'Failed to fetch locations'
    },
    empty: {
      ok: true,
      data: [],
      error: null
    }
  },
  stats: {
    success: {
      ok: true,
      data: {
        totalLocations: 1,
        activeLocations: 1,
        deployingLocations: 0,
        errorLocations: 0,
        totalTransactions: 100,
        totalRevenue: 1000
      },
      error: null
    },
    error: {
      ok: false,
      data: null,
      error: 'Failed to fetch stats'
    }
  },
  login: {
    success: {
      ok: true,
      data: {
        token: 'mock-jwt-token',
        user: {
          id: 1,
          email: 'admin@test.com',
          name: 'Test Admin',
          role: 'ADMIN'
        }
      },
      error: null
    },
    error: {
      ok: false,
      data: null,
      error: 'Invalid credentials'
    }
  }
};

// Common test data
export const testData = {
  validCredentials: {
    email: 'admin@test.com',
    password: 'TestPassword123!'
  },
  invalidCredentials: {
    email: 'wrong@test.com',
    password: 'wrongpassword'
  },
  location: {
    id: '1',
    name: 'Test Restaurant',
    domain: 'test.chickencalc.com',
    status: 'active' as const,
    lastSeen: '2024-01-15T10:30:00Z',
    managerEmail: 'manager@test.com',
    cloudProvider: 'AWS',
    region: 'us-east-1',
    createdAt: '2024-01-01T00:00:00Z'
  }
};

// Test constants
export const TEST_CONSTANTS = {
  LOADING_SPINNER_TEST_ID: 'loading-spinner',
  ERROR_MESSAGE_TEST_ID: 'error-message',
  SUCCESS_MESSAGE_TEST_ID: 'success-message',
  FORM_TEST_ID: 'form',
};

// Helper functions for common test scenarios
export const waitForLoadingToFinish = () => {
  return new Promise(resolve => setTimeout(resolve, 0));
};

export const mockConsoleError = () => {
  const originalError = console.error;
  const mockError = jest.fn();
  console.error = mockError;
  
  return {
    mockError,
    restore: () => {
      console.error = originalError;
    }
  };
};

export const mockLocalStorage = () => {
  const mockStorage = {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
  };
  
  Object.defineProperty(window, 'localStorage', {
    value: mockStorage,
    writable: true
  });
  
  return mockStorage;
};

export const mockSessionStorage = () => {
  const mockStorage = {
    getItem: jest.fn(),
    setItem: jest.fn(),
    removeItem: jest.fn(),
    clear: jest.fn(),
  };
  
  Object.defineProperty(window, 'sessionStorage', {
    value: mockStorage,
    writable: true
  });
  
  return mockStorage;
};

// Custom matchers for better test assertions
export const customMatchers = {
  toBeLoadingSpinner: (received: HTMLElement) => {
    const hasSpinnerClass = received.classList.contains('animate-spin');
    const hasRoundedClass = received.classList.contains('rounded-full');
    
    return {
      message: () => `expected element to be a loading spinner`,
      pass: hasSpinnerClass && hasRoundedClass,
    };
  },
  
  toHaveProperAriaLabel: (received: HTMLElement, expectedLabel: string) => {
    const ariaLabel = received.getAttribute('aria-label');
    const ariaLabelledBy = received.getAttribute('aria-labelledby');
    
    return {
      message: () => `expected element to have aria-label "${expectedLabel}" or aria-labelledby`,
      pass: ariaLabel === expectedLabel || !!ariaLabelledBy,
    };
  }
};

// Re-export everything from testing-library
export * from '@testing-library/react';
export { default as userEvent } from '@testing-library/user-event';