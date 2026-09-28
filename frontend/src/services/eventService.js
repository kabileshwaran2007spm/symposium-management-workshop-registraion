import { api } from './api';

/** Drops empty filter values so the query string stays clean. */
function clean(params = {}) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined),
  );
}

export const eventService = {
  /** Public discovery: approved events only, filtered and paged. */
  list: (params) => api.get('/events', clean(params)),

  getById: (id) => api.get(`/events/${id}`),

  create: (payload) => api.post('/events', payload),

  update: (id, payload) => api.put(`/events/${id}`, payload),

  remove: (id) => api.delete(`/events/${id}`),
};

export default eventService;
