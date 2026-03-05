import React from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import { View, ProgressCircle, Flex } from '@adobe/react-spectrum'

function ProtectedRoute({ children }) {
  const { user, loading } = useAuth()

  if (loading) {
    return (
      <Flex justifyContent="center" alignItems="center" height="100vh">
        <ProgressCircle aria-label="Loading..." isIndeterminate />
      </Flex>
    )
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return children
}

export default ProtectedRoute
