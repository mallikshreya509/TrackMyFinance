const KEY = 'tmf_access_token'

// sessionStorage can throw (private mode, blocked storage), so every access is guarded.
export const tokenStore = {
  get() {
    try { return sessionStorage.getItem(KEY) } catch { return null }
  },
  set(token) {
    try { sessionStorage.setItem(KEY, token) } catch { /* ignore */ }
  },
  clear() {
    try { sessionStorage.removeItem(KEY) } catch { /* ignore */ }
  },
}