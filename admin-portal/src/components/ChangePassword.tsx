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

    </div>
  );
};

export default ChangePassword;