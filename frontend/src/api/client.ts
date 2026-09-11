import type { AuthResponse, CreateJobRequest, Execution, Job } from '../types';

const AUTH_TOKEN_KEY = 'orbit_token';

export function getToken(): string | null {
  return localStorage_safe_get(AUTH_TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage_safe_set(AUTH_TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage_safe_remove(AUTH_TOKEN_KEY);
}

// Wrapped so the module still works in environments where localStorage is
// unavailable (e.g. certain sandboxed preview iframes) without crashing.
function localStorage_safe_get(key: string): string | null {
  try {
    return window.localStorage.getItem(key);
  } catch {
    return memoryStore[key] ?? null;
  }
}
function localStorage_safe_set(key: string, value: string): void {
  try {
    window.localStorage.setItem(key, value);
  } catch {
    memoryStore[key] = value;
  }
}
function localStorage_safe_remove(key: string): void {
  try {
    window.localStorage.removeItem(key);
  } catch {
    delete memoryStore[key];
  }
}
const memoryStore: Record<string, string> = {};

class ApiError extends Error {
  constructor(message: string, public status: number) {
    super(message);
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> | undefined)
  };
  if (token) headers.Authorization = `Bearer ${token}`;

const API_BASE_URL = import.meta.env.VITE_API_URL ?? '';
const res = await fetch(`${API_BASE_URL}/api${path}`, { ...options, headers });

  if (!res.ok) {
    let message = `Request failed with status ${res.status}`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      // response wasn't JSON, keep default message
    }
    throw new ApiError(message, res.status);
  }

  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  login: (username: string, password: string) =>
    request<AuthResponse>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),

  register: (username: string, password: string) =>
    request<AuthResponse>('/auth/register', { method: 'POST', body: JSON.stringify({ username, password }) }),

  listJobs: () => request<Job[]>('/jobs'),

  getJob: (id: string) => request<Job>(`/jobs/${id}`),

  createJob: (payload: CreateJobRequest) =>
    request<Job>('/jobs', { method: 'POST', body: JSON.stringify(payload) }),

  triggerJob: (id: string) => request<{ status: string }>(`/jobs/${id}/trigger`, { method: 'POST' }),

  setJobStatus: (id: string, status: string) =>
    request<Job>(`/jobs/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }),

  recentExecutions: () => request<Execution[]>('/executions'),

  executionsForJob: (jobId: string) => request<Execution[]>(`/executions/job/${jobId}`)
};

export { ApiError };
