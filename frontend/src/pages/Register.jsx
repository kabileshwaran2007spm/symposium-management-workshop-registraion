import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import Input from '../components/Input';
import { useToast } from '../context/ToastContext';
import { authService } from '../services/authService';
import { isValid, validateRegister } from '../utils/validation';

const EMPTY = {
  name: '',
  email: '',
  studentId: '',
  department: '',
  password: '',
  confirmPassword: '',
};

/**
 * Student sign-up.
 *
 * There is no role selector: the server always creates a STUDENT account, so
 * offering the choice would only invite a request the API refuses anyway.
 */
export default function Register() {
  const navigate = useNavigate();
  const toast = useToast();

  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const set = (field) => (event) => {
    setValues((current) => ({ ...current, [field]: event.target.value }));
    if (errors[field]) {
      setErrors((current) => ({ ...current, [field]: undefined }));
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    const validation = validateRegister(values);
    setErrors(validation);
    if (!isValid(validation)) return;

    setSubmitting(true);
    try {
      await authService.register({
        name: values.name.trim(),
        email: values.email.trim(),
        studentId: values.studentId.trim(),
        department: values.department.trim(),
        password: values.password,
      });

      toast.success('Account created. Please log in.');
      navigate('/login', { replace: true });
    } catch (error) {
      // The server reports duplicate email or student id here.
      setErrors({ email: error.message });
      toast.error(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth-shell">
      <div className="auth-card auth-card-wide">
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">Register as a student to browse and join events.</p>

        <form onSubmit={handleSubmit} noValidate>
          <Input
            label="Full name"
            required
            autoComplete="name"
            value={values.name}
            onChange={set('name')}
            error={errors.name}
          />

          <Input
            label="Email"
            type="email"
            required
            autoComplete="email"
            hint="Your college email address"
            value={values.email}
            onChange={set('email')}
            error={errors.email}
          />

          <div className="form-grid">
            <Input
              label="Student ID"
              required
              hint="For example CS2026001"
              value={values.studentId}
              onChange={set('studentId')}
              error={errors.studentId}
            />

            <Input
              label="Department"
              required
              hint="For example CSE"
              value={values.department}
              onChange={set('department')}
              error={errors.department}
            />
          </div>

          <div className="form-grid">
            <Input
              label="Password"
              type="password"
              required
              autoComplete="new-password"
              hint="At least 8 characters"
              value={values.password}
              onChange={set('password')}
              error={errors.password}
            />

            <Input
              label="Confirm password"
              type="password"
              required
              autoComplete="new-password"
              value={values.confirmPassword}
              onChange={set('confirmPassword')}
              error={errors.confirmPassword}
            />
          </div>

          <Button type="submit" block loading={submitting}>
            {submitting ? 'Creating account…' : 'Create account'}
          </Button>
        </form>

        <p className="auth-footer">
          Already registered? <Link to="/login">Log in</Link>
        </p>
      </div>
    </div>
  );
}
