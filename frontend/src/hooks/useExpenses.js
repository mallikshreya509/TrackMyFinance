import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { listExpenses } from '../api/expenses'

export function useExpenses(filters) {
  return useQuery({
    queryKey: ['expenses', filters],
    queryFn: () => listExpenses(filters),
    placeholderData: keepPreviousData, // keeps the old rows on screen while the next page loads
  })
}