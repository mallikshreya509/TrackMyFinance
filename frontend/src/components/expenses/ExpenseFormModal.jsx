import { useState } from 'react'
import { getErrorMessage } from '../../api/errors'
import { useExpenseMutations } from '../../hooks/useExpenseMutations'
import { todayLocal } from '../../utils/format'
import Modal from '../common/Modal'

// `expense` = the one being edited, or null when adding
export default function ExpenseFormModal({ expense, categories, onClose }) {
  const { create, update } = useExpenseMutations()
  const editing = Boolean(expense)

  const [amount, setAmount] = useState(editing ? String(expense.amount) : '')
  const [categoryId, setCategoryId] = useState(editing ? String(expense.category.id) : '')
  const [description, setDescription] = useState(editing ? expense.description ?? '' : '')
  const [date, setDate] = useState(editing ? expense.date : todayLocal())
  const [error, setError] = useState('')

  const busy = create.isPending || update.isPending

  function handleSubmit(e) {
    e.preventDefault()
    setError('')

    if (!/^\d+(\.\d{1,2})?$/.test(amount) || Number(amount) < 0.01) {
      setError('Enter an amount like 450 or 99.50 (max 2 decimals).')
      return
    }
    if (!categoryId) { setError('Choose a category.'); return }
    if (!date) { setError('Choose a date.'); return }

    const body = {
      amount: Number(amount),
      categoryId: Number(categoryId),
      description: description.trim() || null,
      date,
    }
    const callbacks = { onSuccess: onClose, onError: (err) => setError(getErrorMessage(err)) }

    if (editing) update.mutate({ id: expense.id, body }, callbacks)
    else create.mutate(body, callbacks)
  }

  return (
    <Modal title={editing ? 'Edit expense' : 'Add expense'} onClose={onClose}>
      <form onSubmit={handleSubmit}>
        <label>
          Amount
          <input type="number" inputMode="decimal" step="0.01" min="0.01" required autoFocus
                 value={amount} onChange={(e) => setAmount(e.target.value)} />
        </label>
        <label>
          Category
          <select required value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="">Choose…</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </select>
        </label>
        <label>
          Date
          <input type="date" required value={date} onChange={(e) => setDate(e.target.value)} />
        </label>
        <label>
          Description (optional)
          <input value={description} maxLength={255}
                 onChange={(e) => setDescription(e.target.value)} />
        </label>
        {error && <p className="error">{error}</p>}
        <div className="modal-actions">
          <button type="button" className="secondary" onClick={onClose}>Cancel</button>
          <button type="submit" disabled={busy}>{busy ? 'Saving…' : 'Save'}</button>
        </div>
      </form>
    </Modal>
  )
}