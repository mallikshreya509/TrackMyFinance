import { useMutation, useQueryClient } from '@tanstack/react-query'
import { createExpense, deleteExpense, updateExpense } from '../api/expenses'

export function useExpenseMutations() {
  const qc = useQueryClient()
  // Any change can alter the list AND the dashboard numbers
  const refresh = () => {
    qc.invalidateQueries({ queryKey: ['expenses'] })
    qc.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const create = useMutation({ mutationFn: createExpense, onSuccess: refresh })
  const update = useMutation({
    mutationFn: ({ id, body }) => updateExpense(id, body),
    onSuccess: refresh,
  })
  const remove = useMutation({ mutationFn: deleteExpense, onSuccess: refresh })
  return { create, update, remove }
}