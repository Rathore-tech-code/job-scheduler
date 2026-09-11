import { useState } from 'react';
import { getToken } from './api/client';
import { LoginScreen } from './components/LoginScreen';
import { Dashboard } from './components/Dashboard';

function usernameFromToken(): string | null {
  const token = getToken();
  if (!token) return null;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.sub ?? null;
  } catch {
    return null;
  }
}

export default function App() {
  const [username, setUsername] = useState<string | null>(usernameFromToken());

  if (!username) {
    return <LoginScreen onAuthenticated={() => setUsername(usernameFromToken())} />;
  }

  return <Dashboard username={username} onLogout={() => setUsername(null)} />;
}
