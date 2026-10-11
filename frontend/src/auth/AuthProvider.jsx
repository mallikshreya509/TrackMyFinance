import { useCallback, useEffect, useMemo, useState } from 'react'
import * as authApi from '../api/auth'
import { setUnauthorizedHandler } from '../api/axiosClient'
import { AuthContext } from './authContext'
import { tokenStore } from './tokenStore'
import { useQueryClient } from '@tanstack/react-query'

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  // If a token is already stored, we must verify it before deciding "logged in or not"
  const [loading, setLoading] = useState(Boolean(tokenStore.get()))
  const queryClient = useQueryClient()

  const clearSession = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    queryClient.clear() // never show one user's cached data to the next
  }, [queryClient])
  
  // Lets the axios interceptor log us out on any 401
  useEffect(() => {
    setUnauthorizedHandler(clearSession)
    return () => setUnauthorizedHandler(null)
  }, [clearSession])

  // On first load: if a token exists, ask the backend who it belongs to
  useEffect(() => {
    if (!tokenStore.get()) return
    authApi
      .me()
      .then(setUser)
      .catch(clearSession)
      .finally(() => setLoading(false))
  }, [clearSession])

  const login = useCallback(async (email, password) => {
    const data = await authApi.login(email, password)
    tokenStore.set(data.accessToken)
    setUser(data.user)
  }, [])

  const register = useCallback(
    async (email, password, displayName) => {
      await authApi.register(email, password, displayName)
      await login(email, password)
    },
    [login],
  )

  const value = useMemo(
    () => ({ user, loading, login, register, logout: clearSession }),
    [user, loading, login, register, clearSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}