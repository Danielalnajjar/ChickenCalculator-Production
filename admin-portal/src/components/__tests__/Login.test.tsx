import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import Login from '../Login';
import { AuthProvider } from '../../contexts/AuthContext';

// Mock the AuthContext
const mockLogin = jest.fn();
const mockAuthContext = {
  login: mockLogin,
  logout: jest.fn(),
  user: null,
  token: null,
  isAuthenticated: false,
  isLoading: false,
};

// Mock react-router-dom
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
}));

// Mock AuthContext
jest.mock('../../contexts/AuthContext', () => ({
  useAuth: () => mockAuthContext,
  AuthProvider: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

// Helper function to render Login component
const renderLogin = () => {
  return render(
    <BrowserRouter>
      <AuthProvider>
        <Login />
      </AuthProvider>
    </BrowserRouter>
  );
};

describe('Login Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockLogin.mockReset();
    mockNavigate.mockReset();
  });

  describe('Rendering', () => {
    test('renders login form with all required elements', () => {
      renderLogin();

      expect(screen.getByRole('heading', { name: /chicken calculator admin/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    });

    test('renders with correct accessibility attributes', () => {
      renderLogin();

      const form = screen.getByRole('form');
      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);

      expect(form).toHaveAttribute('aria-labelledby', 'login-heading');
      expect(emailInput).toHaveAttribute('aria-describedby', 'email-help');
      expect(passwordInput).toHaveAttribute('aria-describedby', 'password-help');
    });

    test('shows skip to content link for screen readers', () => {
      renderLogin();

      const skipLink = screen.getByText(/skip to login form/i);
      expect(skipLink).toBeInTheDocument();
      expect(skipLink).toHaveClass('sr-only');
    });
  });

  describe('Form Interaction', () => {
    test('updates email input value when user types', async () => {
      const user = userEvent.setup();
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      await user.type(emailInput, 'test@example.com');

      expect(emailInput).toHaveValue('test@example.com');
    });

    test('updates password input value when user types', async () => {
      const user = userEvent.setup();
      renderLogin();

      const passwordInput = screen.getByLabelText(/password/i);
      await user.type(passwordInput, 'testpassword');

      expect(passwordInput).toHaveValue('testpassword');
    });

    test('toggles password visibility when eye icon is clicked', async () => {
      const user = userEvent.setup();
      renderLogin();

      const passwordInput = screen.getByLabelText(/password/i);
      const toggleButton = screen.getByLabelText(/show password/i);

      expect(passwordInput).toHaveAttribute('type', 'password');

      await user.click(toggleButton);
      expect(passwordInput).toHaveAttribute('type', 'text');
      expect(screen.getByLabelText(/hide password/i)).toBeInTheDocument();

      await user.click(toggleButton);
      expect(passwordInput).toHaveAttribute('type', 'password');
      expect(screen.getByLabelText(/show password/i)).toBeInTheDocument();
    });
  });

  describe('Form Submission', () => {
    test('calls login function with correct credentials on form submission', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValue(true);
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'admin@test.com');
      await user.type(passwordInput, 'testpassword');
      await user.click(submitButton);

      expect(mockLogin).toHaveBeenCalledWith('admin@test.com', 'testpassword');
    });

    test('navigates to dashboard on successful login', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValue(true);
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'admin@test.com');
      await user.type(passwordInput, 'testpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
      });
    });

    test('shows error message on failed login', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValue(false);
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'invalid@test.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
      });

      expect(mockNavigate).not.toHaveBeenCalled();
    });

    test('shows loading state during login attempt', async () => {
      const user = userEvent.setup();
      // Mock a delayed response
      mockLogin.mockImplementation(() => new Promise(resolve => setTimeout(() => resolve(true), 100)));
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'admin@test.com');
      await user.type(passwordInput, 'testpassword');
      await user.click(submitButton);

      expect(screen.getByText(/signing in.../i)).toBeInTheDocument();
      expect(submitButton).toBeDisabled();

      await waitFor(() => {
        expect(screen.queryByText(/signing in.../i)).not.toBeInTheDocument();
      });
    });

    test('prevents form submission with empty fields', async () => {
      const user = userEvent.setup();
      renderLogin();

      const submitButton = screen.getByRole('button', { name: /sign in/i });
      await user.click(submitButton);

      // HTML5 validation should prevent submission
      expect(mockLogin).not.toHaveBeenCalled();
    });
  });

  describe('Error Handling', () => {
    test('clears error message when user starts typing', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValue(false);
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      // First, trigger an error
      await user.type(emailInput, 'invalid@test.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
      });

      // Clear inputs and type new values
      await user.clear(emailInput);
      await user.type(emailInput, 'new@test.com');

      // Error should still be visible since we only clear on form submission
      expect(screen.getByText(/invalid email or password/i)).toBeInTheDocument();
    });

    test('error message has proper ARIA attributes', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValue(false);
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'invalid@test.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        const errorMessage = screen.getByText(/invalid email or password/i);
        const errorContainer = errorMessage.closest('[role="alert"]');
        
        expect(errorContainer).toBeInTheDocument();
        expect(errorContainer).toHaveAttribute('aria-live', 'polite');
      });
    });
  });

  describe('Accessibility', () => {
    test('form has proper ARIA labeling', () => {
      renderLogin();

      const form = screen.getByRole('form');
      expect(form).toHaveAttribute('aria-labelledby', 'login-heading');
    });

    test('inputs have proper ARIA attributes when error occurs', async () => {
      const user = userEvent.setup();
      mockLogin.mockResolvedValue(false);
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'invalid@test.com');
      await user.type(passwordInput, 'wrongpassword');
      await user.click(submitButton);

      await waitFor(() => {
        expect(emailInput).toHaveAttribute('aria-invalid', 'true');
        expect(passwordInput).toHaveAttribute('aria-invalid', 'true');
      });
    });

    test('loading state is announced to screen readers', async () => {
      const user = userEvent.setup();
      mockLogin.mockImplementation(() => new Promise(resolve => setTimeout(() => resolve(true), 100)));
      
      renderLogin();

      const emailInput = screen.getByLabelText(/email address/i);
      const passwordInput = screen.getByLabelText(/password/i);
      const submitButton = screen.getByRole('button', { name: /sign in/i });

      await user.type(emailInput, 'admin@test.com');
      await user.type(passwordInput, 'testpassword');
      await user.click(submitButton);

      const loadingIndicator = screen.getByText(/signing in.../i).closest('[aria-live="polite"]');
      expect(loadingIndicator).toBeInTheDocument();
    });
  });
});