/**
 * Client-side validation.
 *
 * This exists to give immediate feedback, not to enforce anything: the server
 * validates every request again, and its rules are the ones that matter. The
 * messages here deliberately mirror the server's so a user never sees two
 * different explanations for the same problem.
 */

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
const STUDENT_ID_PATTERN = /^[A-Za-z0-9-]+$/;

export const PASSWORD_MIN_LENGTH = 8;

function required(value, label) {
  return value === undefined || value === null || String(value).trim() === ''
    ? `${label} is required`
    : null;
}

export function validateLogin(values) {
  const errors = {};

  const email = required(values.email, 'Email');
  if (email) errors.email = email;
  else if (!EMAIL_PATTERN.test(values.email.trim())) errors.email = 'Enter a valid email address';

  const password = required(values.password, 'Password');
  if (password) errors.password = password;

  return errors;
}

export function validateRegister(values) {
  const errors = {};

  const name = required(values.name, 'Name');
  if (name) errors.name = name;
  else if (values.name.trim().length < 2) errors.name = 'Name must be at least 2 characters';

  const email = required(values.email, 'Email');
  if (email) errors.email = email;
  else if (!EMAIL_PATTERN.test(values.email.trim())) errors.email = 'Enter a valid email address';

  const studentId = required(values.studentId, 'Student ID');
  if (studentId) errors.studentId = studentId;
  else if (values.studentId.trim().length < 3) errors.studentId = 'Student ID must be at least 3 characters';
  else if (!STUDENT_ID_PATTERN.test(values.studentId.trim()))
    errors.studentId = 'Student ID may contain only letters, digits and hyphens';

  const password = required(values.password, 'Password');
  if (password) errors.password = password;
  else if (values.password.length < PASSWORD_MIN_LENGTH)
    errors.password = `Password must be at least ${PASSWORD_MIN_LENGTH} characters`;

  const confirm = required(values.confirmPassword, 'Password confirmation');
  if (confirm) errors.confirmPassword = confirm;
  else if (values.confirmPassword !== values.password) errors.confirmPassword = 'Passwords do not match';

  const department = required(values.department, 'Department');
  if (department) errors.department = department;

  return errors;
}

export function validateEvent(values) {
  const errors = {};

  const title = required(values.title, 'Event title');
  if (title) errors.title = title;
  else if (values.title.trim().length < 3) errors.title = 'Title must be at least 3 characters';

  const description = required(values.description, 'Description');
  if (description) errors.description = description;
  else if (values.description.trim().length < 10)
    errors.description = 'Description must be at least 10 characters';

  if (required(values.category, 'Category')) errors.category = 'Category is required';
  if (required(values.department, 'Department')) errors.department = 'Department is required';
  if (required(values.venue, 'Venue')) errors.venue = 'Venue is required';
  if (required(values.date, 'Event date')) errors.date = 'Event date is required';
  if (required(values.startTime, 'Start time')) errors.startTime = 'Start time is required';
  if (required(values.endTime, 'End time')) errors.endTime = 'End time is required';
  if (required(values.registrationDeadline, 'Registration deadline'))
    errors.registrationDeadline = 'Registration deadline is required';

  const capacity = Number(values.capacity);
  if (!values.capacity) errors.capacity = 'Capacity is required';
  else if (!Number.isFinite(capacity) || capacity < 1) errors.capacity = 'Capacity must be at least 1';
  else if (capacity > 100000) errors.capacity = 'Capacity must be at most 100000';

  // Cross-field rules, matching EventService.validateSchedule.
  if (!errors.startTime && !errors.endTime && values.endTime <= values.startTime) {
    errors.endTime = 'End time must be after the start time';
  }
  if (!errors.date && !errors.registrationDeadline && values.registrationDeadline > values.date) {
    errors.registrationDeadline = 'Registration deadline must be on or before the event date';
  }

  return errors;
}

/** True when a validation result contains no errors. */
export function isValid(errors) {
  return Object.keys(errors).length === 0;
}
