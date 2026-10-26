import { Link, NavLink, useNavigate } from 'react-router-dom';

import Button from './Button';
import { useAuth } from '../context/useAuth';
import { roleLabel } from '../utils/format';

/** Navigation links for each role, mirroring the navigation structure in the design. */
const LINKS_BY_ROLE = {
  STUDENT: [
    { to: '/dashboard', label: 'Dashboard' },
    { to: '/discover', label: 'Discover' },
    { to: '/my-activity', label: 'My activity' },
    { to: '/profile', label: 'Profile' },
    { to: '/settings', label: 'Settings' },
  ],
  ORGANIZER: [
    { to: '/organizer', label: 'Dashboard' },
    { to: '/discover', label: 'Discover' },
    { to: '/profile', label: 'Profile' },
    { to: '/settings', label: 'Settings' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Dashboard' },
    { to: '/admin/events', label: 'Events' },
    { to: '/admin/users', label: 'Users' },
    { to: '/admin/clubs', label: 'Clubs' },
    { to: '/admin/analytics', label: 'Analytics' },
    { to: '/profile', label: 'Profile' },
    { to: '/settings', label: 'Settings' },
  ],
};

export default function Navbar() {
  const { user, isAuthenticated, role, logout } = useAuth();
  const navigate = useNavigate();

  const links = isAuthenticated ? LINKS_BY_ROLE[role] || [] : [{ to: '/discover', label: 'Discover' }];

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <header className="navbar">
      <div className="navbar-inner">
        <Link className="navbar-brand" to={isAuthenticated ? '/' : '/'}>
          <span aria-hidden="true">🎓</span>
          CampusConnect
        </Link>

        <nav className="navbar-links" aria-label="Main navigation">
          {links.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              className={({ isActive }) => `navbar-link${isActive ? ' active' : ''}`}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>

        <div className="navbar-actions">
          {isAuthenticated ? (
            <>
              <span className="user-chip" title={`${user.name} · ${roleLabel(user.role)}`}>
                <span aria-hidden="true">👤</span>
                <span className="user-chip-name">{user.name}</span>
                <span className="visually-hidden">
                  Signed in as {user.name}, {roleLabel(user.role)}
                </span>
              </span>
              <Button variant="secondary" size="sm" onClick={handleLogout}>
                Log out
              </Button>
            </>
          ) : (
            <>
              <Button variant="ghost" size="sm" onClick={() => navigate('/login')}>
                Log in
              </Button>
              <Button size="sm" onClick={() => navigate('/register')}>
                Register
              </Button>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
