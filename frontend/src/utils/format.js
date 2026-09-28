/**
 * Presentation helpers.
 *
 * Dates and times arrive from the API as ISO strings ("2026-10-12", "10:00:00").
 * They are parsed by hand rather than with `new Date(string)`, because that treats
 * a date-only string as UTC midnight and can shift an event to the previous day for
 * anyone west of Greenwich.
 */

const MONTHS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
];

const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

/** "2026-10-12" -> "12 Oct 2026" */
export function formatDate(value) {
  if (!value) return '—';
  const [year, month, day] = String(value).split('-').map(Number);
  if (!year || !month || !day) return String(value);
  return `${day} ${MONTHS[month - 1]} ${year}`;
}

/** "2026-10-12" -> "Mon, 12 Oct 2026" */
export function formatDateLong(value) {
  if (!value) return '—';
  const [year, month, day] = String(value).split('-').map(Number);
  if (!year || !month || !day) return String(value);
  const weekday = WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()];
  return `${weekday}, ${day} ${MONTHS[month - 1]} ${year}`;
}

/** "10:00:00" or "10:00" -> "10:00 AM" */
export function formatTime(value) {
  if (!value) return '—';
  const [hourText, minuteText] = String(value).split(':');
  const hour = Number(hourText);
  if (Number.isNaN(hour)) return String(value);

  const minute = (minuteText ?? '00').padStart(2, '0');
  const suffix = hour >= 12 ? 'PM' : 'AM';
  const displayHour = hour % 12 === 0 ? 12 : hour % 12;
  return `${displayHour}:${minute} ${suffix}`;
}

/** "10:00:00" + "13:00:00" -> "10:00 AM – 1:00 PM" */
export function formatTimeRange(start, end) {
  if (!start && !end) return '—';
  if (!end) return formatTime(start);
  return `${formatTime(start)} – ${formatTime(end)}`;
}

/** ISO instant -> "28 Sep 2026, 10:30 AM" */
export function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);

  const day = date.getDate();
  const month = MONTHS[date.getMonth()];
  const year = date.getFullYear();
  const hours = date.getHours();
  const minutes = String(date.getMinutes()).padStart(2, '0');
  const suffix = hours >= 12 ? 'PM' : 'AM';
  const displayHour = hours % 12 === 0 ? 12 : hours % 12;

  return `${day} ${month} ${year}, ${displayHour}:${minutes} ${suffix}`;
}

/** Human wording for an enum value: COMPLETED -> "Completed". */
function humanise(value) {
  if (!value) return '—';
  return String(value)
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export const statusLabel = humanise;
export const roleLabel = humanise;
export const categoryLabel = humanise;

const STATUS_TONES = {
  // Event statuses
  DRAFT: 'neutral',
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'error',
  CANCELLED: 'error',
  COMPLETED: 'info',
  // Registration statuses
  CONFIRMED: 'success',
  WAITLISTED: 'warning',
  ATTENDED: 'info',
};

/** Maps a status to a badge tone, so colour always agrees with the wording. */
export function statusTone(status) {
  return STATUS_TONES[status] || 'neutral';
}

/** "12 of 200 seats available" style phrasing for the seat block. */
export function seatSummary(available, capacity) {
  if (capacity === null || capacity === undefined) return '—';
  return `${available} of ${capacity} seats available`;
}

/** Today's date as YYYY-MM-DD, the format the API expects for date inputs. */
export function todayIso() {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

/** Adds a number of days to a YYYY-MM-DD value. */
export function addDays(isoDate, days) {
  const [year, month, day] = String(isoDate).split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  date.setUTCDate(date.getUTCDate() + days);
  const outMonth = String(date.getUTCMonth() + 1).padStart(2, '0');
  const outDay = String(date.getUTCDate()).padStart(2, '0');
  return `${date.getUTCFullYear()}-${outMonth}-${outDay}`;
}
