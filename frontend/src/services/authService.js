import { api } from './api';

export const authService = {
  /** Public signup. The server always creates a STUDENT account. */
  register: (payload) => api.post('/auth/register', payload),

  login: (credentials) => api.post('/auth/login', credentials),

  /**
   * The server holds no session, so this is a courtesy call: the real logout is
   * discarding the token locally.
   */
  logout: () => api.post('/auth/logout'),
};

export default authService;
