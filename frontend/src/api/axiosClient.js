import axios from 'axios'
import { tokenStore } from '../auth/tokenStore'

const client = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api/v1',
  timeout: 20000,
})

let onUnauthorized = null
export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn
}

client.interceptors.request.use((config) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url ?? ''
    // A 401 from login/register just means "wrong credentials", not "session expired"
    const isAuthCall = url.startsWith('/auth/login') || url.startsWith('/auth/register')
    if (error.response?.status === 401 && !isAuthCall && onUnauthorized) {
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)

export default client