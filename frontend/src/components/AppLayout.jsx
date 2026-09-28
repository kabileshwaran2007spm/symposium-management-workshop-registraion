import { Outlet } from 'react-router-dom';

import BottomNav from './BottomNav';
import Navbar from './Navbar';
import Sidebar from './Sidebar';

/**
 * The authenticated application shell.
 *
 * Renders the navbar, the role-aware sidebar and the current page, plus the mobile
 * bottom bar. A skip link lets keyboard users jump straight past the navigation to
 * the content.
 */
export default function AppLayout() {
  return (
    <div className="app-shell">
      <a className="skip-link" href="#main-content">
        Skip to main content
      </a>

      <Navbar />

      <div className="layout-body">
        <Sidebar />
        <main className="content" id="main-content">
          <Outlet />
        </main>
      </div>

      <BottomNav />
    </div>
  );
}
