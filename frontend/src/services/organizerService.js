import { api, downloadFile } from './api';

export const organizerService = {
  myEvents: (params = {}) => api.get('/organizer/events', params),

  attendees: (eventId, params = {}) => api.get(`/organizer/events/${eventId}/attendees`, params),

  analytics: (eventId) => api.get(`/organizer/events/${eventId}/analytics`),

  /** Flat attendee rows, the same shape the CSV export writes. */
  exportJson: (eventId) => api.get(`/organizer/events/${eventId}/export/json`),

  exportCsv: (eventId) => downloadFile(`/organizer/events/${eventId}/export/csv`, 'attendees.csv'),
};

export default organizerService;
