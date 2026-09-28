import { Link, useLocation, useNavigate } from 'react-router-dom';

import Button from '../components/Button';
import { homePathFor, useAuth } from '../context/AuthContext';

/**
 * Shown when a signed-in user reaches a screen their role cannot use.
 *
 * The message names what went wrong and offers the way back to their own home
 * page, so the dead end always has a signposted exit.
 */
export default function Unauthorized() {
  const { role } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const requiredRoles = location.state?.requiredRoles;

  return (
    <div className="auth-shell">
      <div className="auth-card" style={{ textAlign: 'center' }}>
        <span style={{ fontSize: '3rem' }} aria-hidden="true">
          🔒
        </span>
        <h1 className="auth-title">Access restricted</h1>
        <p className="auth-subtitle">
          {requiredRoles && requiredRoles.length > 0
            ? `This screen is for ${requiredRoles.map((item) => item.toLowerCase()).join(' or ')} accounts.`
            : 'Your account does not have permission to view this screen.'}
        </p>

        {role ? (
          <Button block onClick={() => navigate(homePathFor(role))}>
            Go to my dashboard
          </Button>
        ) : (
          <Link className="btn btn-primary btn-block" to="/login">
            Log in
          </Link>
        )}

        <p className="auth-footer">
          Think you should have access? Contact the CampusConnect administrator.
        </p>
      </div>
    </div>
  );
}
