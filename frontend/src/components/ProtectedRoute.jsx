import { Navigate, Outlet, useLocation } from 'react-router-dom';

import { homePathFor, useAuth } from '../context/AuthContext';

/**
 * Requires any signed-in user, remembering where they were headed so they land
 * there after logging in.
 */
export function ProtectedRoute() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}

/**
 * Requires one of the listed roles.
 *
 * This is a usability guard, not a security boundary: it stops a student from
 * wandering into an admin screen and seeing a broken page, while the API refuses
 * the same requests with 403 regardless of what the browser does.
 */
export function RoleRoute({ roles }) {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!roles.includes(role)) {
    return <Navigate to="/unauthorized" replace state={{ requiredRoles: roles }} />;
  }

  return <Outlet />;
}

/** Sends a signed-in user to the right home page for their role. */
export function RoleHomeRedirect() {
  const { isAuthenticated, role } = useAuth();
  if (!isAuthenticated) return <Navigate to="/" replace />;
  return <Navigate to={homePathFor(role)} replace />;
}

export default ProtectedRoute;
