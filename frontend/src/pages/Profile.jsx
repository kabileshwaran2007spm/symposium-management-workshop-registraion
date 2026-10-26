import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import ErrorState from '../components/ErrorState';
import Input from '../components/Input';
import LoadingSpinner from '../components/LoadingSpinner';
import { useAuth } from '../context/useAuth';
import { useToast } from '../context/useToast';
import { profileService } from '../services/profileService';
import { roleLabel } from '../utils/format';
import useAsync from '../utils/useAsync';

export default function Profile() {
  const { user, updateUserProfile, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const profileState = useAsync(() => profileService.get(), []);
  const [form, setForm] = useState(null);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const handleChange = (field) => (event) => {
    setForm((current) => ({
      ...(current ?? {
        name: profileState.data?.name ?? '',
        department: profileState.data?.department ?? '',
      }),
      [field]: event.target.value,
    }));
    setErrors((current) => ({ ...current, [field]: '' }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    const nextErrors = {};
    const name = formValues.name.trim();
    const department = formValues.department.trim();

    if (name.length < 2 || name.length > 100) {
      nextErrors.name = 'Name must be between 2 and 100 characters';
    }
    if (!department || department.length > 60) {
      nextErrors.department = 'Department is required and must be at most 60 characters';
    }
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    setSaving(true);
    try {
      const updated = await profileService.update({ name, department });
      profileState.setData(updated);
      setForm({ name: updated.name, department: updated.department });
      updateUserProfile(updated);
      toast.success('Profile updated');
    } catch (error) {
      toast.error(error.message);
    } finally {
      setSaving(false);
    }
  };

  const signOut = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  if (!user) return null;
  if (profileState.loading && !profileState.data) {
    return <LoadingSpinner message="Loading your profile…" />;
  }
  if (profileState.error && !profileState.data) {
    return (
      <ErrorState
        title="We couldn't load your profile"
        message={profileState.error.message}
        onRetry={profileState.reload}
      />
    );
  }

  const currentProfile = profileState.data ?? user;
  const formValues = form ?? {
    name: currentProfile.name ?? '',
    department: currentProfile.department ?? '',
  };

  return (
    <>
      <div className="page-header">
        <h1>Profile</h1>
        <p className="page-subtitle">View and update your account name and department.</p>
      </div>

      <form className="card card-padded stack" style={{ maxWidth: 620 }} onSubmit={handleSubmit} noValidate>
        <Input
          label="Name"
          required
          autoComplete="name"
          value={formValues.name}
          onChange={handleChange('name')}
          error={errors.name}
        />
        <Input label="Email" type="email" value={currentProfile.email} readOnly />
        <Input label="Student ID" value={currentProfile.studentId} readOnly />
        <Input
          label="Department"
          required
          value={formValues.department}
          onChange={handleChange('department')}
          error={errors.department}
        />
        <Input label="Role" value={roleLabel(currentProfile.role)} readOnly />
        <div className="form-actions">
          <Button type="submit" loading={saving}>
            {saving ? 'Saving…' : 'Save profile'}
          </Button>
        </div>
      </form>

      <p className="text-caption" style={{ marginTop: 'var(--space-4)' }}>
        Email, student ID, and account role can’t be changed here. Contact an administrator if those details need correction.
      </p>

      {user.role === 'STUDENT' && (
        <p>
          <Link to="/my-activity">View my activity →</Link>
        </p>
      )}

      <p>
        <Link to="/settings">Account settings →</Link>
      </p>

      <div style={{ marginTop: 'var(--space-5)' }}>
        <Button variant="secondary" onClick={signOut}>Log out</Button>
      </div>
    </>
  );
}
