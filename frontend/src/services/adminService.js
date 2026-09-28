import { api } from './api';

/** Drops empty filter values so the query string stays clean. */
function clean(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined),
  );
}

export const adminService = {
  users: (params = {}) => api.get('/admin/users', clean(params)),

  events: (params = {}) => api.get('/admin/events', clean(params)),

  pendingEvents: (params = {}) => api.get('/admin/events/pending', clean(params)),

  registrations: (params = {}) => api.get('/admin/registrations', clean(params)),

  clubs: () => api.get('/admin/clubs'),

  approve: (eventId) => api.put(`/admin/events/${eventId}/approve`),

  reject: (eventId) => api.put(`/admin/events/${eventId}/reject`),

  analytics: () => api.get('/admin/analytics'),
};

export default adminService;
