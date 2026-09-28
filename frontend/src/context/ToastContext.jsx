import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react';

import { ToastViewport } from '../components/Toast';

const ToastContext = createContext(null);

const DEFAULT_DURATION_MS = 4500;

/**
 * Short-lived feedback messages.
 *
 * The region is an ARIA status area, so a screen reader announces the message
 * without stealing focus, and every toast can be dismissed by keyboard.
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const nextId = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id));
  }, []);

  const push = useCallback(
    (type, message, duration = DEFAULT_DURATION_MS) => {
      const id = nextId.current++;
      setToasts((current) => [...current, { id, type, message }]);

      if (duration > 0) {
        window.setTimeout(() => {
          setToasts((current) => current.filter((toast) => toast.id !== id));
        }, duration);
      }
      return id;
    },
    [],
  );

  const value = useMemo(
    () => ({
      success: (message) => push('success', message),
      error: (message) => push('error', message),
      warning: (message) => push('warning', message),
      info: (message) => push('info', message),
      dismiss,
    }),
    [push, dismiss],
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <ToastViewport toasts={toasts} onDismiss={dismiss} />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside a ToastProvider');
  }
  return context;
}

export default ToastContext;
