import { useCallback, useEffect, useMemo, useState } from 'react';

import AuthContext from './authContext';
import { sessionStore, setUnauthorizedHandler } from '../services/api';
import { authService } from '../services/authService';

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => sessionStore.load());
  // Set when the server rejects a token, so the login screen can explain why the
  // user was bounced.
  const [notice, setNotice] = useState(null);

  useEffect(() => {
    setUnauthorizedHandler((error) => {
      setSession(null);
      setNotice(error?.message || 'Your session has expired. Please log in again.');
    });
  }, []);

  const login = useCallback(async (credentials) => {
    const data = await authService.login(credentials);

    const next = {
      token: data.token,
      user: {
        id: data.userId,
        name: data.name,
        email: data.email,
        studentId: data.studentId,
        department: data.department,
        role: data.role,
      },
    };

    sessionStore.save(next);
    setSession(next);
    setNotice(null);
    return next.user;
  }, []);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } catch {
      // The token may already be invalid; the local session is cleared regardless.
    }
    sessionStore.clear();
    setSession(null);
    setNotice(null);
  }, []);

  const clearNotice = useCallback(() => setNotice(null), []);

  const value = useMemo(() => {
    const user = session?.user ?? null;
    return {
      user,
      token: session?.token ?? null,
      role: user?.role ?? null,
      isAuthenticated: Boolean(session?.token),
      notice,
      clearNotice,
      login,
      logout,
      hasRole: (...roles) => Boolean(user && roles.includes(user.role)),
    };
  }, [session, notice, clearNotice, login, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
