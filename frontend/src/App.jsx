import { useAuth } from './context/AuthContext';
import { LoginPage } from './pages/LoginPage';
import { ChatPage } from './pages/ChatPage';
import './styles/globals.css';

export default function App() {
  const { user } = useAuth();
  return user ? <ChatPage /> : <LoginPage />;
}
