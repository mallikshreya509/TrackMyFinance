import { formatDate, formatMoney } from '../../utils/format'

export default function ExpenseTable({ expenses, onEdit, onDelete }) {
  if (expenses.length === 0) {
    return <p className="muted">No expenses found. Add one, or adjust the filters.</p>
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>Date</th><th>Category</th><th>Description</th>
            <th className="num">Amount</th><th />
          </tr>
        </thead>
        <tbody>
          {expenses.map((e) => (
            <tr key={e.id}>
              <td>{formatDate(e.date)}</td>
              <td>
                <span className="dot" style={{ background: e.category.color }} />
                {e.category.name}
              </td>
              <td>{e.description || <span className="muted">—</span>}</td>
              <td className="num">{formatMoney(e.amount)}</td>
              <td className="actions">
                <button type="button" className="secondary" onClick={() => onEdit(e)}>Edit</button>
                <button type="button" className="danger" onClick={() => onDelete(e)}>Delete</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}