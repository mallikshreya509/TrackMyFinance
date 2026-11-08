import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/useAuth'

export default function AppLayout() {
  const { user, logout } = useAuth()

  return (
    <div>
      <header className="nav">
        <strong>TrackMyFinance</strong>
        <nav className="nav-links">
          <NavLink to="/" end>Dashboard</NavLink>
          <NavLink to="/expenses">Expenses</NavLink>
          <NavLink to="/insights">Insights</NavLink>
        </nav>
        <span className="spacer" />
        <span>{user.displayName || user.email}</span>
        <button onClick={logout}>Log out</button>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </div>
  )
}