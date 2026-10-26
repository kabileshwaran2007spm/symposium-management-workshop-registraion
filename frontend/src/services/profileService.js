import { api } from './api';

export const profileService = {
  get: () => api.get('/users/me'),
  update: (profile) => api.put('/users/me', profile),
};

export default profileService;
