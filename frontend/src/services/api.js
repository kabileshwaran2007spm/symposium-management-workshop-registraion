import axios from 'axios';

/**
 * The single Axios instance every service uses.
 *
 * Three things happen here so that no component ever has to think about them:
 *   1. the bearer token is attached automatically,
 *   2. the {success, message, data} envelope is unwrapped, so callers get `data`,
 *   3. failures become a plain Error with a message worth showing a user.
 */

const SESSION_KEY = 'campusconnect.session';

const DEFAULT_API_BASE_URL = 'https://symposium-management-workshop-registraion.onrender.com/api';
const LOCAL_API_BASE_URL = 'http://localhost:8080/api';

function resolveApiBaseUrl(configuredUrl) {
  const fallbackUrl = import.meta.env.PROD ? DEFAULT_API_BASE_URL : LOCAL_API_BASE_URL;
  let candidate = configuredUrl?.trim() || fallbackUrl;

  if (import.meta.env.PROD) {
    const isAbsoluteHttpUrl = /^https?:\/\//i.test(candidate);
    const isLocalhost = /^https?:\/\/(localhost|127\.0\.0\.1)(:\d+)?(?:\/|$)/i.test(candidate);
    if (!isAbsoluteHttpUrl || isLocalhost) {
      candidate = DEFAULT_API_BASE_URL;
    }
  }

  const url = new URL(candidate, window.location.origin);
  const basePath = url.pathname.replace(/\/+$/, '');
  if (!basePath.endsWith('/api')) {
    url.pathname = `${basePath}/api`;
  }

  return `${url.origin}${url.pathname.replace(/\/+$/, '')}`;
}

const BASE_URL = resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL);

/**
 * Session persistence. Kept separate from the auth context so this module has no
 * import cycle with React.
 */
export const sessionStore = {
  load() {
    try {
      const raw = window.localStorage.getItem(SESSION_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      // Corrupted or unreadable storage should log the user out, not crash.
      return null;
    }
  },
  save(session) {
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  },
  clear() {
    window.localStorage.removeItem(SESSION_KEY);
  },
};

/**
 * Called when the server rejects a token. AuthContext registers itself so an
 * expired session drops the user back to the login screen instead of leaving a
 * half-broken page.
 */
let unauthorizedHandler = () => {};

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

/** True when the request failed because the caller is not allowed. */
export function isForbidden(error) {
  return error?.status === 403;
}

export function isNotFound(error) {
  return error?.status === 404;
}

export function isConflict(error) {
  return error?.status === 409;
}

const http = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 20000,
});

http.interceptors.request.use((config) => {
  const token = sessionStore.load()?.token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/** Turns an axios failure into an Error carrying a user-facing message. */
function toAppError(error) {
  const { response } = error;

  if (!response) {
    const message = error.code === 'ECONNABORTED'
      ? 'The server request timed out. Please try again.'
      : 'Could not contact the API. Check your network connection and the backend CORS configuration.';
    const appError = new Error(message);
    appError.status = 0;
    appError.kind = 'network';
    appError.code = error.code;
    return appError;
  }

  const body = response.data;
  let message = body?.message;

  if (!message) {
    if (response.status === 401) message = 'Your session has expired. Please log in again.';
    else if (response.status === 403) message = 'You do not have permission to do that.';
    else if (response.status === 404) message = 'The requested item could not be found.';
    else if (response.status === 409) message = 'That action conflicts with the current state.';
    else if (response.status >= 500) message = 'Something went wrong on the server.';
    else message = 'The request could not be completed.';
  }

  const appError = new Error(message);
  appError.status = response.status;
  appError.kind = 'http';
  return appError;
}

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const appError = toAppError(error);

    if (appError.status === 401) {
      sessionStore.clear();
      unauthorizedHandler(appError);
    }

    return Promise.reject(appError);
  },
);

/**
 * JSON request that resolves to the envelope's `data`.
 *
 * Endpoints that legitimately return nothing resolve to `null`.
 */
export async function request(config) {
  const response = await http.request(config);
  const body = response.data;

  // A non-enveloped body (or a 204) is passed through unchanged.
  if (body === null || body === undefined || typeof body !== 'object' || !('data' in body)) {
    return body;
  }
  return body.data;
}

export const api = {
  get: (url, params) => request({ method: 'GET', url, params }),
  post: (url, data) => request({ method: 'POST', url, data }),
  put: (url, data) => request({ method: 'PUT', url, data }),
  patch: (url, data) => request({ method: 'PATCH', url, data }),
  delete: (url) => request({ method: 'DELETE', url }),
};

/** Reads the server-supplied filename, falling back to a sensible default. */
function filenameFrom(disposition, fallback) {
  if (!disposition) return fallback;
  const match = /filename="?([^"]+)"?/.exec(disposition);
  return match ? match[1] : fallback;
}

/**
 * Downloads a CSV export.
 *
 * These endpoints return a file rather than the JSON envelope, so they bypass
 * `request()` and hand the browser a Blob.
 */
export async function downloadFile(url, fallbackName) {
  const response = await http.request({ method: 'GET', url, responseType: 'blob' });
  const filename = filenameFrom(response.headers['content-disposition'], fallbackName);

  const objectUrl = window.URL.createObjectURL(new Blob([response.data], { type: 'text/csv' }));
  const link = document.createElement('a');
  link.href = objectUrl;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.URL.revokeObjectURL(objectUrl);

  return filename;
}

export default api;
