import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { getSummary } from '../api/dashboard'

export function useDashboard({ from, to }) {
  const validRange = Boolean(from && to && from <= to)
  return useQuery({
    queryKey: ['dashboard', from, to],
    queryFn: () => getSummary({ from, to }),
    enabled: validRange,
    placeholderData: keepPreviousData,
  })
}