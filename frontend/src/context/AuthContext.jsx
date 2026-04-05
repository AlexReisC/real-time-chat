import { createContext, useContext, useEffect, useReducer, useCallback } from 'react';
import { authApi } from '../api/auth.js';

const AuthContext = createContext(null);

const initialState = {
  status: 'loading', // 'loading' | 'authenticated' | 'unauthenticated'
  user: null,
  token: null,
};

function reducer(state, action) {
  switch (action.type) {
    case 'SET_AUTHENTICATED':
      return { status: 'authenticated', user: action.user, token: action.token };
    case 'SET_UNAUTHENTICATED':
      return { status: 'unauthenticated', user: null, token: null };
    case 'UPDATE_USER':
      return { ...state, user: { ...state.user, ...action.partial } };
    default:
      return state;
  }
}

export function AuthProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initialState);

  const logout = useCallback(() => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    dispatch({ type: 'SET_UNAUTHENTICATED' });
  }, []);

  // Bootstrap: validate stored token on mount
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      dispatch({ type: 'SET_UNAUTHENTICATED' });
      return;
    }

    authApi.getMe()
      .then((user) => dispatch({ type: 'SET_AUTHENTICATED', user, token }))
      .catch(() => {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        dispatch({ type: 'SET_UNAUTHENTICATED' });
      });
  }, []);

  // Listen for forced logout from API interceptor
  useEffect(() => {
    window.addEventListener('auth:logout', logout);
    return () => window.removeEventListener('auth:logout', logout);
  }, [logout]);

  const login = useCallback(async (email, password) => {
    const data = await authApi.login(email, password);
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);

    const user = await authApi.getMe();
    dispatch({ type: 'SET_AUTHENTICATED', user, token: data.accessToken });
  }, []);

  const register = useCallback(async (username, email, password) => {
    const data = await authApi.register(username, email, password);
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);

    const user = await authApi.getMe();
    dispatch({ type: 'SET_AUTHENTICATED', user, token: data.accessToken });
  }, []);

  const updateUser = useCallback((partial) => {
    dispatch({ type: 'UPDATE_USER', partial });
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout, updateUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
