import client from './axiosClient'

// The AI call can be slow (the model may take a while and the server retries), so allow 90s.
const AI_TIMEOUT_MS = 90000

export const generateSuggestions = ({ from, to, refresh }) =>
  client
    .post('/ai/suggestions', { from, to, refresh: Boolean(refresh) }, { timeout: AI_TIMEOUT_MS })
    .then((r) => r.data)

// 204 means nothing has been stored yet
export const getLatestSuggestions = () =>
  client.get('/ai/suggestions/latest').then((r) => (r.status === 204 || !r.data ? null : r.data))