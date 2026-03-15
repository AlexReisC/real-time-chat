import { createContext, useContext, useState, useCallback } from 'react';
import { authApi, userApi } from '../services/api';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem('relay_token'));

  const login = useCallback(async (email, password) => {
    const res = await authApi.login(email, password);
    localStorage.setItem('relay_token', res.accessToken);
    if (res.refreshToken) localStorage.setItem('relay_refresh_token', res.refreshToken);
    setToken(res.accessToken);
    const userData = { id: res.user.id, username: res.user.username, email: res.user.email };
    setUser(userData);
    return userData;
  }, []);

  const register = useCallback(async (username, email, password) => {
    const res = await authApi.register(username, email, password);
    localStorage.setItem('relay_token', res.accessToken);
    if (res.refreshToken) localStorage.setItem('relay_refresh_token', res.refreshToken);
    setToken(res.accessToken);
    const userData = { id: res.user.id, username: res.user.username, email: res.user.email };
    setUser(userData);
    return userData;
  }, []);

  /**
   * PATCH /api/v1/users/me          → atualiza username
   * PUT   /api/v1/users/me/password → troca senha (exige senha atual)
   */
  const updateProfile = useCallback(async (username, currentPassword, newPassword) => {
    if (username) {
      const updated = await userApi.updateMe(username);
      setUser(prev => ({ ...prev, username: updated.username ?? username }));
    }
    if (currentPassword && newPassword) {
      await userApi.updatePassword(currentPassword, newPassword);
    }
  }, []);

  const logout = useCallback(() => {
    setUser(null);
    setToken(null);
    localStorage.removeItem('relay_token');
    localStorage.removeItem('relay_refresh_token');
  }, []);

  return (
    <AuthContext.Provider value={{ user, token, login, register, updateProfile, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
