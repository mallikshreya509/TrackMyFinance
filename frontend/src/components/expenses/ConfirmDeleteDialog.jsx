import { useState } from 'react'
import { getErrorMessage } from '../../api/errors'
import { useExpenseMutations } from '../../hooks/useExpenseMutations'
import { formatDate, formatMoney } from '../../utils/format'
import Modal from '../common/Modal'

export default function ConfirmDeleteDialog({ expense, onClose, onDeleted }) {
  const { remove } = useExpenseMutations()
  const [error, setError] = useState('')

  function confirm() {
    setError('')
    remove.mutate(expense.id, {
      onSuccess: () => { onDeleted(); onClose() },
      onError: (err) => setError(getErrorMessage(err)),
    })
  }

  return (
    <Modal title="Delete expense?" onClose={onClose}>
      <p>
        {formatMoney(expense.amount)} · {expense.category.name} · {formatDate(expense.date)}
      </p>
      <p className="muted">This can't be undone.</p>
      {error && <p className="error">{error}</p>}
      <div className="modal-actions">
        <button type="button" className="secondary" onClick={onClose}>Cancel</button>
        <button type="button" className="danger" disabled={remove.isPending} onClick={confirm}>
          {remove.isPending ? 'Deleting…' : 'Delete'}
        </button>
      </div>
    </Modal>
  )
}