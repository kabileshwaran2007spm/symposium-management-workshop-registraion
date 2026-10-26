import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import { useAuth } from '../context/useAuth';
import { authService } from '../services/authService';
import { roleLabel } from '../utils/format';

export default function Settings() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [sendingReset, setSendingReset] = useState(false);
  const [resetRequested, setResetRequested] = useState(false);
  const [error, setError] = useState('');

  const requestPasswordReset = async () => {
    if (!user?.email) return;

    setSendingReset(true);
    setError('');
    try {
      await authService.requestPasswordReset(user.email);
      setResetRequested(true);
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setSendingReset(false);
    }
  };

  const signOut = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <>
      <div className="page-header">
        <h1>Settings</h1>
        <p className="page-subtitle">Manage account security and your active session.</p>
      </div>

      <section className="card card-padded" style={{ maxWidth: 720, marginBottom: 'var(--space-4)' }}>
        <h2>Account</h2>
        <div className="review-row">
          <span className="review-key">Signed in as</span>
          <span>{user?.email}</span>
        </div>
        <div className="review-row">
          <span className="review-key">Account type</span>
          <span>{roleLabel(user?.role)}</span>
        </div>
        <p className="text-caption" style={{ marginTop: 'var(--space-3)' }}>
          <Link to="/profile">View your full profile</Link>
        </p>
      </section>

      <section className="card card-padded" style={{ maxWidth: 720, marginBottom: 'var(--space-4)' }}>
        <h2>Account security</h2>
        <p>Request a secure password reset link for your account email.</p>
        {resetRequested ? (
          <p className="notice notice-success" role="status">
            If an account exists for this email, a password reset link has been sent. Check your inbox and spam folder.
          </p>
        ) : (
          <Button loading={sendingReset} onClick={requestPasswordReset}>
            {sendingReset ? 'Sending…' : 'Email me a password reset link'}
          </Button>
        )}
        {error && <p className="notice notice-error" role="alert">{error}</p>}
      </section>

      {user?.role === 'STUDENT' && (
        <section className="card card-padded" style={{ maxWidth: 720, marginBottom: 'var(--space-4)' }}>
          <h2>My activity</h2>
          <p>Review upcoming, completed, and cancelled event registrations.</p>
          <Link className="btn btn-secondary" to="/my-activity">View my activity</Link>
        </section>
      )}

      <section className="card card-padded" style={{ maxWidth: 720 }}>
        <h2>Session</h2>
        <p>Sign out of CampusConnect on this device.</p>
        <Button variant="secondary" onClick={signOut}>Sign out</Button>
      </section>
    </>
  );
}
