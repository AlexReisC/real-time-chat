import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext.jsx';

export function PrivateRoute({ children }) {
  const { status } = useAuth();

  if (status === 'loading') {
    return (
      <div className="splash">
        <span className="splash-logo">◈</span>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return <Navigate to="/login" replace />;
  }

  return children;
}
