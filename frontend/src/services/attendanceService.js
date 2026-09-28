import { api, downloadFile } from './api';

export const attendanceService = {
  /** Records a check-in from a QR pass token. */
  checkIn: (qrToken) => api.post('/attendance/check-in', { qrToken }),

  list: (eventId) => api.get(`/events/${eventId}/attendance`),

  exportCsv: (eventId) => downloadFile(`/events/${eventId}/attendance/export`, 'attendance.csv'),
};

export default attendanceService;
