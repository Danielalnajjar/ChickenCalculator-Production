import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { BuildingStorefrontIcon, EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';

const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    const success = await login(email, password);
    if (success) {
      navigate('/dashboard');
    } else {
      setError('Invalid email or password');
    }
    setIsLoading(false);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        {/* Skip to main content link */}
        <a href="#login-form" className="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 bg-blue-600 text-white px-4 py-2 rounded z-50">
          Skip to login form
        </a>
        
        {/* Logo and Header */}
        <div className="text-center">
          <div className="flex justify-center">
            <BuildingStorefrontIcon className="h-16 w-16 text-blue-600" aria-hidden="true" />
          </div>
          <h1 className="mt-6 text-3xl font-bold text-gray-900">
            Chicken Calculator Admin
          </h1>
          <p className="mt-2 text-sm text-gray-600">
            Manage your restaurant locations
          </p>
        </div>

        {/* Login Form */}
        <div className="bg-white shadow-xl rounded-lg">
          <div className="px-8 py-6">
            <form onSubmit={handleSubmit} className="space-y-6" id="login-form" role="form" aria-labelledby="login-heading">
              <h2 id="login-heading" className="sr-only">Login to admin portal</h2>
              
              {error && (
                <div className="bg-red-50 border border-red-200 rounded-md p-4" role="alert" aria-live="polite">
                  <div className="text-sm text-red-800">{error}</div>
                </div>
              )}

              <div>
                <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                  Email address
                </label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  autoComplete="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 min-h-[44px]"
                  placeholder="admin@yourcompany.com"
                  aria-describedby="email-help"
                  aria-invalid={error ? 'true' : 'false'}
                />
                <div id="email-help" className="sr-only">
                  Enter your admin email address to log in
                </div>
              </div>

              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                  Password
                </label>
                <div className="mt-1 relative">
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 pr-10 min-h-[44px]"
                    aria-describedby="password-help"
                    aria-invalid={error ? 'true' : 'false'}
                  />
                  <button
                    type="button"
                    className="absolute inset-y-0 right-0 pr-3 flex items-center min-h-[44px] min-w-[44px] hover:bg-gray-50 rounded-r-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    onClick={() => setShowPassword(!showPassword)}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                    aria-describedby="password-toggle-help"
                  >
                    {showPassword ? (
                      <EyeSlashIcon className="h-5 w-5 text-gray-400" aria-hidden="true" />
                    ) : (
                      <EyeIcon className="h-5 w-5 text-gray-400" aria-hidden="true" />
                    )}
                  </button>
                  <div id="password-help" className="sr-only">
                    Enter your admin password
                  </div>
                  <div id="password-toggle-help" className="sr-only">
                    Click to toggle password visibility
                  </div>
                </div>
              </div>

              <div>
                <button
                  type="submit"
                  disabled={isLoading}
                  className={`w-full flex justify-center py-3 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white min-h-[44px] ${
                    isLoading
                      ? 'bg-gray-500 cursor-not-allowed'
                      : 'bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500'
                  }`}
                  aria-describedby="submit-help"
                >
                  {isLoading ? (
                    <div className="flex items-center" aria-live="polite">
                      <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" aria-hidden="true"></div>
                      Signing in...
                    </div>
                  ) : (
                    'Sign in'
                  )}
                </button>
                <div id="submit-help" className="sr-only">
                  Click to submit the login form and access the admin portal
                </div>
              </div>
            </form>
          </div>

        </div>

        {/* Footer */}
        <div className="text-center">
          <p className="text-xs text-gray-500">
            Chicken Calculator Admin Portal v1.0
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;