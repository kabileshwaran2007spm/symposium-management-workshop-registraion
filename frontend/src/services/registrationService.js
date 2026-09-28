import { api } from './api';

export const registrationService = {
  register: (eventId) => api.post(`/events/${eventId}/register`),

  cancel: (eventId) => api.delete(`/events/${eventId}/register`),

  myRegistrations: () => api.get('/users/me/registrations'),
};

export default registrationService;
