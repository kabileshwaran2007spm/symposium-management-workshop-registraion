import { NavLink } from 'react-router-dom';

import { useAuth } from '../context/AuthContext';
import { roleLabel } from '../utils/format';

/**
 * Sidebar navigation.
 *
 * The items are chosen by role, but this is a convenience only — every underlying
 * endpoint re-checks the role on the server, so hiding a link is never what keeps
 * someone out.
 */
const SECTIONS_BY_ROLE = {
  STUDENT: [
    {
      heading: 'Discover',
      items: [
        { to: '/dashboard', label: 'Dashboard' },
        { to: '/events', label: 'Browse events' },
      ],
    },
    {
      heading: 'My activity',
      items: [
        { to: '/my-registrations', label: 'My registrations' },
        { to: '/profile', label: 'Profile' },
      ],
    },
  ],
  ORGANIZER: [
    {
      heading: 'Events',
      items: [
        { to: '/organizer', label: 'Dashboard' },
        { to: '/organizer/events/new', label: 'Create event' },
      ],
    },
    {
      heading: 'Discover',
      items: [{ to: '/events', label: 'Browse events' }],
    },
    {
      heading: 'Account',
      items: [{ to: '/profile', label: 'Profile' }],
    },
  ],
  ADMIN: [
    {
      heading: 'Oversight',
      items: [
        { to: '/admin', label: 'Dashboard' },
        { to: '/admin/events', label: 'Events & approvals' },
      ],
    },
    {
      heading: 'Records',
      items: [
        { to: '/admin/users', label: 'Users' },
        { to: '/admin/registrations', label: 'Registrations' },
        { to: '/admin/clubs', label: 'Clubs' },
      ],
    },
    {
      heading: 'Insight',
      items: [{ to: '/admin/analytics', label: 'Analytics' }],
    },
  ],
};

export default function Sidebar() {
  const { role, user } = useAuth();
  const sections = SECTIONS_BY_ROLE[role] || [];

  if (sections.length === 0) return null;

  return (
    <aside className="sidebar">
      <nav aria-label={`${roleLabel(role)} navigation`}>
        <p className="sidebar-heading">{roleLabel(role)}</p>

        {sections.map((section) => (
          <div key={section.heading}>
            <p className="sidebar-heading">{section.heading}</p>
            {section.items.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                end={item.to === '/organizer' || item.to === '/admin' || item.to === '/dashboard'}
                className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
              >
                {item.label}
              </NavLink>
            ))}
          </div>
        ))}

        {user && (
          <p className="text-caption" style={{ padding: 'var(--space-3)', margin: 0 }}>
            {user.department}
          </p>
        )}
      </nav>
    </aside>
  );
}
