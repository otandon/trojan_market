import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import api, { TOKEN_KEY } from '../api/client.js';

const AuthContext = createContext(null);

function userFromAuthResponse(data) {
  return {
    userID: data.userID,
    username: data.username,
    firstName: data.firstName,
    lastName: data.lastName,
    email: data.email,
    isVerified: data.isVerified,
  };
}

function applyAuthResponse(data, setUser) {
  if (data?.token) {
    localStorage.setItem(TOKEN_KEY, data.token);
    setUser(userFromAuthResponse(data));
  }
  return data;
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [ready, setReady] = useState(false);

  const refresh = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) {
      setUser(null);
      setReady(true);
      return;
    }
    try {
      const { data } = await api.get('/auth/me');
      setUser(userFromAuthResponse(data));
    } catch {
      localStorage.removeItem(TOKEN_KEY);
      setUser(null);
    } finally {
      setReady(true);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const signup = useCallback(async ({ firstName, lastName, email, password }) => {
    const { data } = await api.post('/auth/signup', { firstName, lastName, email, password });
    return data;
  }, []);

  const verifyEmail = useCallback(async ({ email, code }) => {
    const { data } = await api.post('/auth/verify-email', { email, code });
    return applyAuthResponse(data, setUser);
  }, []);

  const resendVerification = useCallback(async (email) => {
    const { data } = await api.post('/auth/resend-verification', { email });
    return data;
  }, []);

  const login = useCallback(async ({ email, password }) => {
    const { data } = await api.post('/auth/login', { email, password });
    return applyAuthResponse(data, setUser);
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/auth/logout');
    } catch {
      // ignore
    }
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
  }, []);

  const value = useMemo(
    () => ({
      user, ready, isGuest: !user,
      signup, verifyEmail, resendVerification, login, logout, refresh,
    }),
    [user, ready, signup, verifyEmail, resendVerification, login, logout, refresh],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside an AuthProvider');
  return ctx;
}
