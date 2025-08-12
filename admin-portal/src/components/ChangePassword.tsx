import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import apiService, { PasswordChangeRequest } from '../services/api';

interface Props {
  onSuccess?: () => void;
  onCancel?: () => void;
  isRequired?: boolean;
}

const ChangePassword: React.FC<Props> = ({ onSuccess, onCancel, isRequired = false }) => {
  const { user } = useAuth();
  const [formData, setFormData] = useState<PasswordChangeRequest>({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    // Validate form
    if (formData.newPassword !== formData.confirmPassword) {
      setError('New password and confirmation do not match');
      setLoading(false);
      return;
    }

    if (formData.newPassword.length < 8) {
      setError('New password must be at least 8 characters long');
      setLoading(false);
      return;
    }

    try {
      const response = await apiService.changePassword(formData);

      if (response.ok) {
        setSuccess('Password changed successfully!');
        setFormData({
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
        
        // Call success callback after a short delay
        setTimeout(() => {
          if (onSuccess) {
            onSuccess();
          }
        }, 1500);
      } else {
        setError(response.error || 'Failed to change password');
      }
    } catch (error) {
      setError('Network error. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={isRequired ? "change-password-container" : ""}>
      <div className={isRequired ? "change-password-form" : ""}>
        <h2>Change Password</h2>
        {isRequired && (
          <div className="alert alert-warning">
            <strong>Password Change Required!</strong>
            <p>You must change your password before you can access the admin portal.</p>
          </div>
        )}
        
        <form onSubmit={handleSubmit}>
          {user && (
            <div className="form-group">
              <label>Account: {user.email}</label>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="currentPassword">Current Password</label>
            <input
              type="password"
              id="currentPassword"
              name="currentPassword"
              value={formData.currentPassword}
              onChange={handleChange}
              required
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="newPassword">New Password</label>
            <input
              type="password"
              id="newPassword"
              name="newPassword"
              value={formData.newPassword}
              onChange={handleChange}
              required
              disabled={loading}
              minLength={8}
            />
            <small className="form-text">
              Password must be at least 8 characters long and contain uppercase, lowercase, and number.
            </small>
          </div>

          <div className="form-group">
            <label htmlFor="confirmPassword">Confirm New Password</label>
            <input
              type="password"
              id="confirmPassword"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              disabled={loading}
            />
          </div>

          {error && <div className="alert alert-error">{error}</div>}
          {success && <div className="alert alert-success">{success}</div>}

          <div className="form-actions">
            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Changing Password...' : 'Change Password'}
            </button>
            
            {!isRequired && onCancel && (
              <button
                type="button"
                onClick={onCancel}
                disabled={loading}
                className="btn btn-secondary"
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      </div>

      <style jsx>{`
        .change-password-container {
          display: flex;
          justify-content: center;
          align-items: center;
          min-height: 100vh;
          background-color: #f5f5f5;
        }

        .change-password-form {
          background: white;
          padding: 2rem;
          border-radius: 8px;
          box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
          width: 100%;
          max-width: 400px;
        }

        h2 {
          text-align: center;
          margin-bottom: 1.5rem;
          color: #333;
        }

        .form-group {
          margin-bottom: 1rem;
        }

        label {
          display: block;
          margin-bottom: 0.5rem;
          font-weight: 500;
          color: #333;
        }

        input {
          width: 100%;
          padding: 0.75rem;
          border: 1px solid #ddd;
          border-radius: 4px;
          font-size: 1rem;
          box-sizing: border-box;
        }

        input:focus {
          outline: none;
          border-color: #007bff;
          box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
        }

        input:disabled {
          background-color: #f8f9fa;
          cursor: not-allowed;
        }

        .form-text {
          display: block;
          margin-top: 0.25rem;
          font-size: 0.875rem;
          color: #6c757d;
        }

        .alert {
          padding: 0.75rem;
          margin-bottom: 1rem;
          border-radius: 4px;
          font-size: 0.9rem;
        }

        .alert-error {
          background-color: #f8d7da;
          color: #721c24;
          border: 1px solid #f5c6cb;
        }

        .alert-success {
          background-color: #d4edda;
          color: #155724;
          border: 1px solid #c3e6cb;
        }

        .alert-warning {
          background-color: #fff3cd;
          color: #856404;
          border: 1px solid #ffeaa7;
        }

        .form-actions {
          display: flex;
          gap: 1rem;
          margin-top: 1.5rem;
        }

        .btn {
          flex: 1;
          padding: 0.75rem;
          border: none;
          border-radius: 4px;
          font-size: 1rem;
          cursor: pointer;
          transition: background-color 0.2s;
        }

        .btn:disabled {
          opacity: 0.6;
          cursor: not-allowed;
        }

        .btn-primary {
          background-color: #007bff;
          color: white;
        }

        .btn-primary:hover:not(:disabled) {
          background-color: #0056b3;
        }

        .btn-secondary {
          background-color: #6c757d;
          color: white;
        }

        .btn-secondary:hover:not(:disabled) {
          background-color: #545b62;
        }
      `}</style>
    </div>
  );
};

export default ChangePassword;