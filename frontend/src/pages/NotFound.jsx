import { Link, useLocation } from 'react-router-dom';

import Button from '../components/Button';
import { homePathFor } from '../context/authPaths';
import { useAuth } from '../context/useAuth';

/**
 * 404 screen.
 *
 * Offers the most useful destination available: the signed-in user's dashboard,
 * or the public events list otherwise.
 */
export default function NotFound() {
  const location = useLocation();
  const { isAuthenticated, role } = useAuth();

  return (
    <div className="auth-shell">
      <div className="auth-card" style={{ textAlign: 'center' }}>
        <span style={{ fontSize: '3rem' }} aria-hidden="true">
          🧭
        </span>
        <h1 className="auth-title">Page not found</h1>
        <p className="auth-subtitle">
          Nothing lives at <code>{location.pathname}</code>.
        </p>

        {isAuthenticated ? (
          <Button block onClick={() => homePathFor(role)}>
            Go to my dashboard
          </Button>
        ) : (
          <>
            <Link className="btn btn-primary btn-block" to="/events">
              Browse events
            </Link>
            <p className="auth-footer">
              Or <Link to="/login">log in</Link> to see your dashboard.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
