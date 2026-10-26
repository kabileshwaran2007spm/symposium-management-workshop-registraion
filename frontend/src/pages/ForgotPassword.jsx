import { useState } from 'react';
import { Link } from 'react-router-dom';

import Button from '../components/Button';
import Input from '../components/Input';
import { authService } from '../services/authService';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(email.trim())) {
      setError('Enter a valid email address');
      return;
    }

    setSubmitting(true);
    try {
      await authService.requestPasswordReset(email.trim());
      setSubmitted(true);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h1 className="auth-title">Forgot password?</h1>
        {submitted ? (
          <>
            <p className="auth-subtitle" role="status">
              If an account exists for that email, a password reset link has been sent.
              Check your inbox and spam folder.
            </p>
            <p className="auth-footer"><Link to="/login">Back to log in</Link></p>
          </>
        ) : (
          <>
            <p className="auth-subtitle">
              Enter your account email and we’ll send a secure reset link if it matches an account.
            </p>
            <form onSubmit={handleSubmit} noValidate>
              <Input
                label="Email"
                type="email"
                required
                autoComplete="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                error={error}
              />
              <Button type="submit" block loading={submitting}>
                {submitting ? 'Sending…' : 'Send reset link'}
              </Button>
            </form>
            <p className="auth-footer"><Link to="/login">Back to log in</Link></p>
          </>
        )}
      </div>
    </div>
  );
}
