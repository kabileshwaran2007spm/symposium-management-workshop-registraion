/** Where each role lands after signing in. */
export const HOME_PATH_BY_ROLE = {
  STUDENT: '/dashboard',
  ORGANIZER: '/organizer',
  ADMIN: '/admin',
};

export function homePathFor(role) {
  return HOME_PATH_BY_ROLE[role] || '/events';
}
