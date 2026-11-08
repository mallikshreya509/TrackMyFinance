import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { generateSuggestions, getLatestSuggestions } from '../api/ai'

export function useLatestSuggestions() {
  return useQuery({ queryKey: ['ai', 'latest'], queryFn: getLatestSuggestions })
}

export function useGenerateSuggestions() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: generateSuggestions,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['ai', 'latest'] }),
  })
}