import { formatMoney } from '../../utils/format'

export default function SummaryCards({ summary }) {
  const top = summary.byCategory[0]
  const change = summary.changePercent === null ? null : Number(summary.changePercent)

  return (
    <div className="cards">
      <div className="card">
        <span className="card-label">Total spent</span>
        <strong className="card-value">{formatMoney(summary.totalSpent)}</strong>
        <span className="muted">{summary.expenseCount} expenses</span>
      </div>

      <div className="card">
        <span className="card-label">Vs previous period</span>
        {change === null ? (
          <>
            <strong className="card-value">—</strong>
            <span className="muted">No spending in the previous period to compare</span>
          </>
        ) : (
          <>
            <strong className={`card-value ${change > 0 ? 'bad' : 'good'}`}>
              {change > 0 ? '+' : ''}{change.toFixed(1)}%
            </strong>
            <span className="muted">Previous: {formatMoney(summary.previousPeriodTotal)}</span>
          </>
        )}
      </div>

      <div className="card">
        <span className="card-label">Top category</span>
        {top ? (
          <>
            <strong className="card-value">{top.name}</strong>
            <span className="muted">
              {formatMoney(top.total)} · {Number(top.percentage).toFixed(1)}%
            </span>
          </>
        ) : (
          <strong className="card-value">—</strong>
        )}
      </div>
    </div>
  )
}