import { useQuery } from '@tanstack/react-query'
import { listCategories } from '../api/categories'

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: listCategories,
    staleTime: 10 * 60 * 1000, // categories rarely change
  })
}