import { BrowserRouter } from 'react-router-dom';

import { AuthProvider } from './context/AuthContext.jsx';
import { ToastProvider } from './context/ToastContext.jsx';
import AppRoutes from './routes/AppRoutes';

/**
 * Application composition.
 *
 * Providers wrap the router so every screen can read the session and raise
 * toasts. Keep the order: ToastProvider does not depend on AuthProvider, but a
 * future provider that needs auth should go inside it.
 */
export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <BrowserRouter>
          <AppRoutes />
        </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  );
}
