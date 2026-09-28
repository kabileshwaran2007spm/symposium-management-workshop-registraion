import { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import Input from '../components/Input';
import { useAuth, homePathFor } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { isValid, validateLogin } from '../utils/validation';

export default function Login() {
  const { login, isAuthenticated, role, notice, clearNotice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();

  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  // Where the user was originally headed, if they were bounced here by a guard.
  const redirectTo = location.state?.from;

  useEffect(() => {
    if (isAuthenticated) {
      navigate(redirectTo || homePathFor(role), { replace: true });
    }
  }, [isAuthenticated, role, redirectTo, navigate]);

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

          <Button type="submit" block loading={submitting}>
            {submitting ? 'Logging in…' : 'Log in'}
          </Button>
        </form>

        <p className="auth-footer">
          New to CampusConnect? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  );
}
