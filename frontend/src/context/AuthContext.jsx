import { useCallback, useEffect, useMemo, useState } from 'react';

import AuthContext from './authContext';
import { sessionStore, setUnauthorizedHandler } from '../services/api';
import { authService } from '../services/authService';

function createSession(data) {
  return {
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
}

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

  const acceptLogin = useCallback((data) => {
    const next = createSession(data);
    sessionStore.save(next);
    setSession(next);
    setNotice(null);
    return next.user;
  }, []);

  const updateUserProfile = useCallback(({ name, department }) => {
    const current = sessionStore.load();
    if (!current) return;

    const next = {
      ...current,
      user: { ...current.user, name, department },
    };
    sessionStore.save(next);
    setSession(next);
  }, []);

  const login = useCallback(async (credentials) => {
    return acceptLogin(await authService.login(credentials));
  }, [acceptLogin]);

  const loginWithGoogle = useCallback(async (credential) => {
    return acceptLogin(await authService.loginWithGoogle(credential));
  }, [acceptLogin]);

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
      loginWithGoogle,
      updateUserProfile,
      logout,
      hasRole: (...roles) => Boolean(user && roles.includes(user.role)),
    };
  }, [session, notice, clearNotice, login, loginWithGoogle, updateUserProfile, logout]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
