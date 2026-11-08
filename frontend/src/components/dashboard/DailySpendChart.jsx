import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { fillDays, formatDate, formatDayMonth, formatMoney } from '../../utils/format'

const compact = new Intl.NumberFormat('en-IN', { notation: 'compact' })

export default function DailySpendChart({ from, to, dailyTotals }) {
  const data = fillDays(from, to, dailyTotals)

  return (
    <div className="panel">
      <h2>Daily spending</h2>
      {dailyTotals.length === 0 ? (
        <p className="muted">No spending in this period.</p>
      ) : (
        <div style={{ width: '100%', height: 280 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="date" tickFormatter={formatDayMonth} minTickGap={24} />
              <YAxis tickFormatter={(v) => compact.format(v)} width={50} />
              <Tooltip
                labelFormatter={(label) => formatDate(label)}
                formatter={(value) => [formatMoney(value), 'Spent']}
              />
              <Bar dataKey="total" fill="#2563eb" radius={[3, 3, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      )}
    </div>
  )
}