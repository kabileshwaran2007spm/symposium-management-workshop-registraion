import { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import Input from '../components/Input';
import { homePathFor } from '../context/authPaths';
import { useAuth } from '../context/useAuth';
import { useToast } from '../context/useToast';
import { isValid, validateLogin } from '../utils/validation';

export default function Login() {
  const { login, loginWithGoogle, isAuthenticated, role, notice, clearNotice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const googleButton = useRef(null);
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID?.trim();

  // Where the user was originally headed, if they were bounced here by a guard.
  const redirectTo = location.state?.from;

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo || homePathFor(role), { replace: true });
    }
  }, [isAuthenticated, role, redirectTo, navigate]);

  useEffect(() => {
    if (!googleClientId || !googleButton.current) return undefined;

    let cancelled = false;
    let script = document.querySelector('script[data-google-identity]');
    const handleScriptError = () => toast.error('Google sign-in could not be loaded.');

    const renderGoogleButton = () => {
      if (cancelled || !window.google?.accounts?.id || !googleButton.current) return;
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: async ({ credential }) => {
          setSubmitting(true);
          try {
            const user = await loginWithGoogle(credential);
            clearNotice();
            toast.success(`Welcome, ${user.name}`);
            navigate(redirectTo || homePathFor(user.role), { replace: true });
          } catch (error) {
            setErrors({ password: error.message });
            toast.error(error.message);
          } finally {
            setSubmitting(false);
          }
        },
      });
      window.google.accounts.id.renderButton(googleButton.current, {
        theme: 'outline',
        size: 'large',
        shape: 'rectangular',
        text: 'continue_with',
        width: 320,
      });
    };

    if (window.google?.accounts?.id) {
      renderGoogleButton();
    } else {
      if (!script) {
        script = document.createElement('script');
        script.src = 'https://accounts.google.com/gsi/client';
        script.async = true;
        script.defer = true;
        script.dataset.googleIdentity = 'true';
      }
      script.addEventListener('load', renderGoogleButton);
      script.addEventListener('error', handleScriptError);
      if (!script.isConnected) document.head.appendChild(script);
    }

    return () => {
      cancelled = true;
      script?.removeEventListener('load', renderGoogleButton);
      script?.removeEventListener('error', handleScriptError);
    };
  }, [googleClientId, loginWithGoogle, clearNotice, navigate, redirectTo, toast]);

  const set = (field) => (event) => {
    setValues((current) => ({ ...current, [field]: event.target.value }));
    if (errors[field]) {
      setErrors((current) => ({ ...current, [field]: undefined }));
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const validation = validateLogin(values);
    setErrors(validation);
    if (!isValid(validation)) return;

    setSubmitting(true);
    try {
      const user = await login({ email: values.email.trim(), password: values.password });
      clearNotice();
      toast.success(`Welcome back, ${user.name}`);
      navigate(redirectTo || homePathFor(user.role), { replace: true });
    } catch (error) {
      setErrors({ password: error.message });
      toast.error(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card">
        <h1 className="auth-title">Log in</h1>
        <p className="auth-subtitle">Access your CampusConnect account.</p>

        {notice && (
          <div className="notice notice-warning" role="status" style={{ marginBottom: 'var(--space-4)' }}>
            {notice}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <Input
            label="Email"
            type="email"
            required
            autoComplete="email"
            value={values.email}
            onChange={set('email')}
            error={errors.email}
          />

          <Input
            label="Password"
            type="password"
            required
            autoComplete="current-password"
            value={values.password}
            onChange={set('password')}
            error={errors.password}
          />

          <p className="auth-footer" style={{ marginTop: 0, marginBottom: 'var(--space-4)', textAlign: 'right' }}>
            <Link to="/forgot-password">Forgot password?</Link>
          </p>

          <Button type="submit" block loading={submitting}>
            {submitting ? 'Logging in…' : 'Log in'}
          </Button>
        </form>

        {googleClientId && (
          <>
            <div className="auth-footer" style={{ margin: 'var(--space-4) 0', textAlign: 'center' }}>
              or
            </div>
            <div ref={googleButton} aria-label="Continue with Google" />
          </>
        )}

        <p className="auth-footer">
          New to CampusConnect? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  );
}
