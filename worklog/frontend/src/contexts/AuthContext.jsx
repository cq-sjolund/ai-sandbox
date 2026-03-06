import React, { createContext, useContext, useState, useEffect } from 'react'
import { authAPI } from '../api/client'
import { useNavigate } from 'react-router-dom'

const AuthContext = createContext()

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [token, setToken] = useState(localStorage.getItem('jwt_token'))
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const loadUser = async () => {
    const storedToken = localStorage.getItem('jwt_token')
    if (storedToken) {
      try {
        setLoading(true)
        const response = await authAPI.getCurrentUser()
        setUser(response.data)
        setToken(storedToken)
      } catch (err) {
        console.error('Failed to load user:', err)
        // Token is invalid or expired
        localStorage.removeItem('jwt_token')
        setToken(null)
        setUser(null)
      } finally {
        setLoading(false)
      }
    } else {
      setLoading(false)
    }
  }

  // Load current user on mount if token exists
  useEffect(() => {
    loadUser()
  }, [])

  const login = async (username, password) => {
    try {
      setLoading(true)
      setError(null)
      const response = await authAPI.login(username, password)
      const { token: jwtToken, username: userName, role } = response.data

      // Store token in localStorage
      localStorage.setItem('jwt_token', jwtToken)
      setToken(jwtToken)

      // Set user info
      const userInfo = { username: userName, role }
      setUser(userInfo)

      return { success: true }
    } catch (err) {
      console.error('Login failed:', err)
      const errorMessage = err.response?.data || 'Login failed'
      setError(errorMessage)
      return { success: false, error: errorMessage }
    } finally {
      setLoading(false)
    }
  }

  const logout = () => {
    localStorage.removeItem('jwt_token')
    setToken(null)
    setUser(null)
    setError(null)
  }

  const value = {
    user,
    token,
    loading,
    error,
    login,
    logout,
    loadUser,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
