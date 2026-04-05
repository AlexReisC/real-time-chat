const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api/v1';

let isRefreshing = false;
let refreshQueue = [];

function processQueue(error, token = null) {
  refreshQueue.forEach(({ resolve, reject }) => {
    if (error) reject(error);
    else resolve(token);
  });
  refreshQueue = [];
}

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (!refreshToken) throw new Error('No refresh token');

  const res = await fetch(`${BASE_URL}/auth/refresh`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  });

  if (!res.ok) throw new Error('Refresh failed');

  const data = await res.json();
  localStorage.setItem('accessToken', data.accessToken);
  localStorage.setItem('refreshToken', data.refreshToken);
  return data.accessToken;
}

export async function request(path, options = {}) {
  const token = localStorage.getItem('accessToken');

  const headers = {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...options.headers,
  };

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        refreshQueue.push({ resolve, reject });
      }).then((newToken) => {
        return request(path, {
          ...options,
          headers: { ...options.headers, Authorization: `Bearer ${newToken}` },
        });
      });
    }

    isRefreshing = true;

    try {
      const newToken = await refreshAccessToken();
      processQueue(null, newToken);
      return request(path, {
        ...options,
        headers: { ...options.headers, Authorization: `Bearer ${newToken}` },
      });
    } catch (err) {
      processQueue(err, null);
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      window.dispatchEvent(new Event('auth:logout'));
      throw err;
    } finally {
      isRefreshing = false;
    }
  }

  return res;
}

export async function requestJson(path, options = {}) {
  const res = await request(path, options);

  if (!res.ok) {
    let errorMessage = `Erro ${res.status}`;
    try {
      const body = await res.json();
      errorMessage = body.message ?? body.error ?? errorMessage;
    } catch {}
    const err = new Error(errorMessage);
    err.status = res.status;
    throw err;
  }

  if (res.status === 204) return null;
  return res.json();
}
