import { useState, type FormEvent } from 'react';
import { api, setToken } from '../api/client';

export function LoginScreen({ onAuthenticated }: { onAuthenticated: () => void }) {
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('');
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      const res = mode === 'login' ? await api.login(username, password) : await api.register(username, password);
      setToken(res.token);
      onAuthenticated();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Authentication failed.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-base px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <div className="mx-auto mb-3 flex h-10 w-10 items-center justify-center rounded-full border border-amber/30 text-amber">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <circle cx="12" cy="12" r="3" />
              <path d="M12 2v4M12 18v4M22 12h-4M6 12H2M19 5l-2.8 2.8M7.8 16.2 5 19M19 19l-2.8-2.8M7.8 7.8 5 5" strokeLinecap="round" />
            </svg>
          </div>
          <h1 className="font-display text-xl font-semibold text-ink">Orbit</h1>
          <p className="mt-1 text-sm text-muted">Distributed job scheduler console</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-3 rounded border border-hairline bg-panel p-5">
          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Username</label>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full rounded border border-hairline bg-raised px-3 py-2 text-sm text-ink focus:border-amber/50 focus:outline-none"
            />
          </div>
          <div>
            <label className="mb-1 block text-xs font-medium text-muted">Password</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded border border-hairline bg-raised px-3 py-2 text-sm text-ink focus:border-amber/50 focus:outline-none"
            />
          </div>

          {error && <p className="text-sm text-coral">{error}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded bg-amber px-4 py-2 text-sm font-medium text-base transition hover:bg-amber/90 disabled:opacity-50"
          >
            {submitting ? 'Please wait…' : mode === 'login' ? 'Sign in' : 'Create account'}
          </button>

          <button
            type="button"
            onClick={() => setMode((m) => (m === 'login' ? 'register' : 'login'))}
            className="w-full text-center text-xs text-muted transition hover:text-ink"
          >
            {mode === 'login' ? "Don't have an account? Register" : 'Already have an account? Sign in'}
          </button>
        </form>

        <p className="mt-4 text-center text-xs text-faint">
          Default seed account: <span className="font-mono">admin / admin123</span>
        </p>
      </div>
    </div>
  );
}
