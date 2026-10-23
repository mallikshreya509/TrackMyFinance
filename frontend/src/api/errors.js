// Turns the backend's error JSON (ProblemDetail) into one readable sentence.
export function getErrorMessage(err) {
  const data = err?.response?.data
  if (data?.errors) return Object.values(data.errors).join(' ')
  if (data?.detail) return data.detail
  return 'Something went wrong. Is the backend running?'
}
