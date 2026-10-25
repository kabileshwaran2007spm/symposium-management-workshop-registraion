import { Link } from 'react-router-dom';

import { useAuth } from '../context/useAuth';
import { roleLabel } from '../utils/format';

/**
 * Profile.
 *
 * The details come from the signed-in session rather than a dedicated endpoint: the
 * API contract has no "current user" route, and the login response already carries
 * everything shown here.
 */
export default function Profile() {
  const { user, logout } = useAuth();

  if (!user) {
    return null;
  }

  return (
    <>
      <div className="page-header">
        <h1>Profile</h1>
        <p className="page-subtitle">Your CampusConnect account details.</p>
      </div>

      <div className="card card-padded" style={{ maxWidth: 620 }}>
        <div className="review-row">
          <span className="review-key">Name</span>
          <span>{user.name}</span>
        </div>
        <div className="review-row">
          <span className="review-key">Email</span>
          <span>{user.email}</span>
        </div>
        <div className="review-row">
          <span className="review-key">Student ID</span>
          <span>{user.studentId}</span>
        </div>
        <div className="review-row">
          <span className="review-key">Department</span>
          <span>{user.department}</span>
        </div>
        <div className="review-row">
          <span className="review-key">Role</span>
          <span>{roleLabel(user.role)}</span>
        </div>
      </div>

      <p className="text-caption" style={{ marginTop: 'var(--space-4)' }}>
        To change your details, contact the CampusConnect administrator.
      </p>

      {user.role === 'STUDENT' && (
        <p>
          <Link to="/my-registrations">View my registrations →</Link>
        </p>
      )}

      <div style={{ marginTop: 'var(--space-5)' }}>
        <button type="button" className="btn btn-secondary" onClick={logout}>
          Log out
        </button>
      </div>
    </>
  );
}
