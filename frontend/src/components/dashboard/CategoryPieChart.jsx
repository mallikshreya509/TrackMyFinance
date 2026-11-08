import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { formatMoney } from '../../utils/format'

export default function CategoryPieChart({ byCategory }) {
  const data = byCategory.map((c) => ({ ...c, total: Number(c.total) }))

  return (
    <div className="panel">
      <h2>Spending by category</h2>
      {data.length === 0 ? (
        <p className="muted">No spending in this period.</p>
      ) : (
        <>
          <div style={{ width: '100%', height: 280 }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={data} dataKey="total" nameKey="name" innerRadius={55} outerRadius={95}>
                  {data.map((c) => (
                    <Cell key={c.categoryId} fill={c.color} />
                  ))}
                </Pie>
                <Tooltip formatter={(value) => formatMoney(value)} />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <ul className="legend-list">
            {data.map((c) => (
              <li key={c.categoryId}>
                <span className="dot" style={{ background: c.color }} />
                <span className="grow">{c.name}</span>
                <span>{formatMoney(c.total)}</span>
                <span className="muted pct">{Number(c.percentage).toFixed(1)}%</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  )
}