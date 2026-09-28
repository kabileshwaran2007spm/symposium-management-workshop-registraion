import { Navigate, Route, Routes } from 'react-router-dom';

import AppLayout from '../components/AppLayout';
import { ProtectedRoute, RoleHomeRedirect, RoleRoute } from '../components/ProtectedRoute';

import Home from '../pages/Home';
import Login from '../pages/Login';
import Register from '../pages/Register';
import Events from '../pages/Events';
import EventDetails from '../pages/EventDetails';
import StudentDashboard from '../pages/StudentDashboard';
import MyRegistrations from '../pages/MyRegistrations';
import QrPass from '../pages/QrPass';
import Profile from '../pages/Profile';

import OrganizerDashboard from '../pages/organizer/OrganizerDashboard';
import CreateEvent from '../pages/organizer/CreateEvent';
import EditEvent from '../pages/organizer/EditEvent';
import Attendees from '../pages/organizer/Attendees';
import Attendance from '../pages/organizer/Attendance';
import OrganizerAnalytics from '../pages/organizer/Analytics';

import AdminDashboard from '../pages/admin/AdminDashboard';
import AdminEvents from '../pages/admin/AdminEvents';
import AdminEventReview from '../pages/admin/AdminEventReview';
import AdminUsers from '../pages/admin/AdminUsers';
import AdminRegistrations from '../pages/admin/AdminRegistrations';
import AdminClubs from '../pages/admin/AdminClubs';
import AdminAnalytics from '../pages/admin/AdminAnalytics';

import Unauthorized from '../pages/Unauthorized';
import NotFound from '../pages/NotFound';

/**
 * Route table.
 *
 * Layout-owned routes render inside AppLayout (navbar + role sidebar); auth and
 * error screens stand alone. Role guards live on the route element so the UI
 * never shows a screen the API would refuse — the server remains the authority.
 */
export default function AppRoutes() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/" element={<Home />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />
      <Route path="/events" element={<Events />} />
      <Route path="/events/:id" element={<EventDetails />} />

      {/* Any signed-in user */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/profile" element={<Profile />} />
        </Route>
      </Route>

      {/* Student */}
      <Route element={<RoleRoute roles={['STUDENT']} />}>
        <Route element={<AppLayout />}>
          <Route path="/dashboard" element={<StudentDashboard />} />
          <Route path="/my-registrations" element={<MyRegistrations />} />
          <Route path="/my-registrations/:registrationId/pass" element={<QrPass />} />
        </Route>
      </Route>

      {/* Organizer (admins also pass — the API allows oversight) */}
      <Route element={<RoleRoute roles={['ORGANIZER', 'ADMIN']} />}>
        <Route element={<AppLayout />}>
          <Route path="/organizer" element={<OrganizerDashboard />} />
          <Route path="/organizer/events/new" element={<CreateEvent />} />
          <Route path="/organizer/events/:id/edit" element={<EditEvent />} />
          <Route path="/organizer/events/:id/attendees" element={<Attendees />} />
          <Route path="/organizer/events/:id/attendance" element={<Attendance />} />
          <Route path="/organizer/events/:id/analytics" element={<OrganizerAnalytics />} />
        </Route>
      </Route>

      {/* Admin */}
      <Route element={<RoleRoute roles={['ADMIN']} />}>
        <Route element={<AppLayout />}>
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/admin/events" element={<AdminEvents />} />
          <Route path="/admin/events/:id/review" element={<AdminEventReview />} />
          <Route path="/admin/users" element={<AdminUsers />} />
          <Route path="/admin/registrations" element={<AdminRegistrations />} />
          <Route path="/admin/clubs" element={<AdminClubs />} />
          <Route path="/admin/analytics" element={<AdminAnalytics />} />
        </Route>
      </Route>

      {/* Utilities */}
      <Route path="/login-success" element={<RoleHomeRedirect />} />
      <Route path="/unauthorized" element={<Unauthorized />} />
      <Route path="/home" element={<Navigate to="/" replace />} />
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
