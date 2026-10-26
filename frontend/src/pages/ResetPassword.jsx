import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';

import Button from '../components/Button';
import Input from '../components/Input';
import { authService } from '../services/authService';
import { PASSWORD_MIN_LENGTH } from '../utils/validation';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [complete, setComplete] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = {};

    if (!token) nextErrors.password = 'This reset link is missing its token. Request a new link.';
    if (password.length < PASSWORD_MIN_LENGTH || password.length > 72) {
      nextErrors.password = 'Password must be between 8 and 72 characters';
    }
    if (password !== confirmPassword) nextErrors.confirmPassword = 'Passwords do not match';
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSubmitting(true);
    try {
      await authService.resetPassword({ token, password });
      setComplete(true);
    } catch (error) {
      setErrors({ password: error.message });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h1 className="auth-title">Reset password</h1>
        {complete ? (
          <>
            <p className="auth-subtitle" role="status">
              Your password has been changed. You can now log in with your new password.
            </p>
            <p className="auth-footer"><Link to="/login">Go to log in</Link></p>
          </>
        ) : (
          <>
            <p className="auth-subtitle">Choose a new password for your account.</p>
            <form onSubmit={handleSubmit} noValidate>
              <Input
                label="New password"
                type="password"
                required
                autoComplete="new-password"
                hint="Use 8–72 characters"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                error={errors.password}
              />
              <Input
                label="Confirm new password"
                type="password"
                required
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(event) => setConfirmPassword(event.target.value)}
                error={errors.confirmPassword}
              />
              <Button type="submit" block loading={submitting}>
                {submitting ? 'Updating…' : 'Update password'}
              </Button>
            </form>
            <p className="auth-footer">
              <Link to="/forgot-password">Request a new reset link</Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
}
