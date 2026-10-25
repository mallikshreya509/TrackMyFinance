import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './auth/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'
import DashboardPage from './pages/DashboardPage'
import ExpensesPage from './pages/ExpensesPage'
import InsightsPage from './pages/InsightsPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<DashboardPage />} />
          <Route path="/expenses" element={<ExpensesPage />} />
          <Route path="/insights" element={<InsightsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}