import { NavLink } from 'react-router-dom';

import { useAuth } from '../context/AuthContext';

/**
 * Mobile bottom navigation, shown only on narrow screens.
 *
 * It carries the four most-used destinations, which for a student are exactly the
 * ones in the design document: home, events, tickets and profile.
 */
const ITEMS_BY_ROLE = {
  STUDENT: [
    { to: '/dashboard', label: 'Home', icon: '🏠' },
    { to: '/events', label: 'Events', icon: '🔍' },
    { to: '/my-registrations', label: 'Tickets', icon: '🎫' },
    { to: '/profile', label: 'Profile', icon: '👤' },
  ],
  ORGANIZER: [
    { to: '/organizer', label: 'Home', icon: '🏠' },
    { to: '/organizer/events/new', label: 'Create', icon: '➕' },
    { to: '/events', label: 'Events', icon: '🔍' },
    { to: '/profile', label: 'Profile', icon: '👤' },
  ],
  ADMIN: [
    { to: '/admin', label: 'Home', icon: '🏠' },
    { to: '/admin/events', label: 'Events', icon: '✅' },
    { to: '/admin/users', label: 'Users', icon: '👥' },
    { to: '/admin/analytics', label: 'Reports', icon: '📊' },
  ],
};

export default function BottomNav() {
  const { role, isAuthenticated } = useAuth();

  const items = isAuthenticated
    ? ITEMS_BY_ROLE[role] || ITEMS_BY_ROLE.STUDENT
    : [{ to: '/events', label: 'Events', icon: '🔍' }];

  return (
    <nav className="bottom-nav" aria-label="Mobile navigation">
      <div className="bottom-nav-inner">
        {items.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.to === '/organizer' || item.to === '/admin' || item.to === '/dashboard'}
            className={({ isActive }) => `bottom-nav-link${isActive ? ' active' : ''}`}
          >
            <span className="bottom-nav-icon" aria-hidden="true">
              {item.icon}
            </span>
            {item.label}
          </NavLink>
        ))}
      </div>
    </nav>
  );
}
