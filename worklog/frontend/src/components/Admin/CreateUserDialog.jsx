import React, { useState } from 'react'
import {
  Dialog,
  Heading,
  Divider,
  Content,
  Form,
  TextField,
  Picker,
  Item,
  ButtonGroup,
  Button,
  Text,
} from '@adobe/react-spectrum'

function CreateUserDialog({ isOpen, onClose, onSubmit }) {
  const [formData, setFormData] = useState({
    username: '',
    password: '',
    role: 'USER',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async () => {
    if (!formData.username || !formData.password) {
      setError('Please fill in all required fields')
      return
    }

    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters')
      return
    }

    try {
      setLoading(true)
      setError(null)
      await onSubmit(formData)
      setFormData({ username: '', password: '', role: 'USER' })
      onClose()
    } catch (err) {
      setError(err.response?.data || 'Failed to create user')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Dialog isOpen={isOpen} onDismiss={onClose}>
      <Heading>Create New User</Heading>
      <Divider />
      <Content>
        <Form>
          <TextField
            label="Username"
            value={formData.username}
            onChange={(value) => setFormData({ ...formData, username: value })}
            isRequired
            autoFocus
          />
          <TextField
            label="Password"
            type="password"
            value={formData.password}
            onChange={(value) => setFormData({ ...formData, password: value })}
            description="Minimum 8 characters"
            isRequired
          />
          <Picker
            label="Role"
            selectedKey={formData.role}
            onSelectionChange={(value) => setFormData({ ...formData, role: value })}
          >
            <Item key="USER">User</Item>
            <Item key="ADMIN">Admin</Item>
          </Picker>

          {error && (
            <Text>
              <span style={{ color: 'var(--spectrum-global-color-red-600)' }}>{error}</span>
            </Text>
          )}
        </Form>
      </Content>
      <ButtonGroup>
        <Button variant="secondary" onPress={onClose}>
          Cancel
        </Button>
        <Button variant="cta" onPress={handleSubmit} isDisabled={loading}>
          {loading ? 'Creating...' : 'Create User'}
        </Button>
      </ButtonGroup>
    </Dialog>
  )
}

export default CreateUserDialog
