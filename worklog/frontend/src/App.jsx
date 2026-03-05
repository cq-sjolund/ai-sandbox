import React from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { Provider, defaultTheme } from '@adobe/react-spectrum'
import { AuthProvider } from './contexts/AuthContext'
import LoginPage from './components/Auth/LoginPage'
import Dashboard from './components/Dashboard/Dashboard'
import ProtectedRoute from './components/Routes/ProtectedRoute'
import AdminRoute from './components/Routes/AdminRoute'
import UserManagement from './components/Admin/UserManagement'

function App() {
  return (
    <Provider theme={defaultTheme} colorScheme="light">
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/admin/users"
              element={
                <AdminRoute>
                  <UserManagement />
                </AdminRoute>
              }
            />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    </Provider>
  )
}

export default App
