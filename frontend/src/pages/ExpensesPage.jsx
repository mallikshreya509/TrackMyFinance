import { useState } from 'react'
import { getErrorMessage } from '../api/errors'
import ConfirmDeleteDialog from '../components/expenses/ConfirmDeleteDialog'
import ExpenseFilters from '../components/expenses/ExpenseFilters'
import ExpenseFormModal from '../components/expenses/ExpenseFormModal'
import ExpenseTable from '../components/expenses/ExpenseTable'
import { useCategories } from '../hooks/useCategories'
import { useExpenses } from '../hooks/useExpenses'

const EMPTY_FILTERS = { from: '', to: '', categoryId: '' }
const PAGE_SIZE = 10

export default function ExpensesPage() {
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [page, setPage] = useState(0)
  const [dialog, setDialog] = useState(null) // { type: 'add' | 'edit' | 'delete', expense? }

  const categoriesQuery = useCategories()
  const { data, isPending, isError, error, isFetching } =
    useExpenses({ ...filters, page, size: PAGE_SIZE })

  const categories = categoriesQuery.data ?? []
  const closeDialog = () => setDialog(null)

  function changeFilters(patch) {
    setFilters((f) => ({ ...f, ...patch }))
    setPage(0) // a new filter always starts at the first page
  }

  // If we just deleted the only row on a later page, step back one page
  function handleDeleted() {
    if (data && data.content.length === 1 && page > 0) setPage(page - 1)
  }

  return (
    <>
      <div className="page-head">
        <h1>Expenses</h1>
        <button onClick={() => setDialog({ type: 'add' })}>+ Add expense</button>
      </div>

      <ExpenseFilters
        filters={filters}
        categories={categories}
        onChange={changeFilters}
        onClear={() => { setFilters(EMPTY_FILTERS); setPage(0) }}
      />

      {isPending && <p className="muted">Loading…</p>}
      {isError && <p className="error">{getErrorMessage(error)}</p>}

      {data && (
        <>
          <ExpenseTable
            expenses={data.content}
            onEdit={(expense) => setDialog({ type: 'edit', expense })}
            onDelete={(expense) => setDialog({ type: 'delete', expense })}
          />
          <div className="pager">
            <button className="secondary" disabled={page === 0}
                    onClick={() => setPage(page - 1)}>Previous</button>
            <span>
              Page {data.totalPages === 0 ? 0 : page + 1} of {data.totalPages}
              {' · '}{data.totalElements} total{isFetching ? ' · updating…' : ''}
            </span>
            <button className="secondary" disabled={page + 1 >= data.totalPages}
                    onClick={() => setPage(page + 1)}>Next</button>
          </div>
        </>
      )}

      {dialog?.type === 'add' && (
        <ExpenseFormModal expense={null} categories={categories} onClose={closeDialog} />
      )}
      {dialog?.type === 'edit' && (
        <ExpenseFormModal expense={dialog.expense} categories={categories} onClose={closeDialog} />
      )}
      {dialog?.type === 'delete' && (
        <ConfirmDeleteDialog expense={dialog.expense} onClose={closeDialog}
                             onDeleted={handleDeleted} />
      )}
    </>
  )
}