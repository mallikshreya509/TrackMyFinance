import { useState } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage } from '../api/errors'
import { useAuth } from '../auth/useAuth'
import CategoryPieChart from '../components/dashboard/CategoryPieChart'
import DailySpendChart from '../components/dashboard/DailySpendChart'
import DateRangePicker from '../components/dashboard/DateRangePicker'
import SummaryCards from '../components/dashboard/SummaryCards'
import { useDashboard } from '../hooks/useDashboard'
import { monthRange } from '../utils/format'

export default function DashboardPage() {
  const [range, setRange] = useState(() => monthRange(0))
  const { data, isPending, isError, error, isFetching } = useDashboard(range)
  const { user, logout } = useAuth()
  const invalidRange = !range.from || !range.to || range.from > range.to

  return (
    <>
      <div className="page-head">
      <h1>Hello, {user.displayName || user.email}</h1>
      <div className="head-actions">
      <Link to="/expenses" className="btn">Manage expenses</Link>
      <button type="button" className="secondary" onClick={logout}>Sign out</button>
      </div>
     </div>

      <DateRangePicker range={range} onChange={setRange} />

      {invalidRange && (
        <p className="error">Choose a valid date range (From must not be after To).</p>
      )}
      {!invalidRange && isPending && <p className="muted">Loading…</p>}
      {!invalidRange && isError && <p className="error">{getErrorMessage(error)}</p>}

      {data && !invalidRange && (
        <div className={isFetching ? 'fading' : ''}>
          <SummaryCards summary={data} />
          <div className="chart-grid">
            <CategoryPieChart byCategory={data.byCategory} />
            <DailySpendChart from={data.period.from} to={data.period.to}
                             dailyTotals={data.dailyTotals} />
          </div>
        </div>
      )}
    </>
  )
}