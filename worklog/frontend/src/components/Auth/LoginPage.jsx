import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../contexts/AuthContext'
import {
  View,
  Flex,
  Heading,
  Form,
  TextField,
  Button,
  Text,
} from '@adobe/react-spectrum'

function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const { login, user } = useAuth()
  const navigate = useNavigate()

  // Redirect if already logged in
  useEffect(() => {
    if (user) {
      navigate('/dashboard')
    }
  }, [user, navigate])

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)
    setLoading(true)

    const result = await login(username, password)

    setLoading(false)

    if (result.success) {
      navigate('/dashboard')
    } else {
      setError(result.error)
    }
  }

  return (
    <Flex
      direction="column"
      justifyContent="center"
      alignItems="center"
      height="100vh"
      backgroundColor="gray-50"
    >
      <View
        backgroundColor="gray-100"
        padding="size-500"
        borderRadius="medium"
        width="size-4600"
        maxWidth="100%"
      >
        <Flex direction="column" gap="size-300">
          <Heading level={2} alignSelf="center">
            Consultant Worklog
          </Heading>
          <Heading level={3} alignSelf="center" marginTop="size-100">
            Login
          </Heading>

          <Form onSubmit={handleSubmit}>
            <Flex direction="column" gap="size-300">
              <TextField
                label="Username"
                value={username}
                onChange={setUsername}
                isRequired
                autoFocus
                width="100%"
              />
              <TextField
                label="Password"
                type="password"
                value={password}
                onChange={setPassword}
                isRequired
                width="100%"
              />

              {error && (
                <Text alignSelf="center">
                  <span style={{ color: 'var(--spectrum-global-color-red-600)' }}>
                    {error}
                  </span>
                </Text>
              )}

              <Button
                type="submit"
                variant="cta"
                isDisabled={loading || !username || !password}
                alignSelf="center"
                width="100%"
              >
                {loading ? 'Logging in...' : 'Login'}
              </Button>
            </Flex>
          </Form>
        </Flex>
      </View>
    </Flex>
  )
}

export default LoginPage
