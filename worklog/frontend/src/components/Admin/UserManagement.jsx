import React, { useState, useEffect } from 'react'
import {
  View,
  Flex,
  Heading,
  Button,
  TableView,
  TableHeader,
  TableBody,
  Column,
  Row,
  Cell,
  ActionButton,
  AlertDialog,
  DialogTrigger,
  Text,
  Divider,
} from '@adobe/react-spectrum'
import { useNavigate } from 'react-router-dom'
import { usersAPI } from '../../api/client'
import Add from '@spectrum-icons/workflow/Add'
import Delete from '@spectrum-icons/workflow/Delete'
import ArrowLeft from '@spectrum-icons/workflow/ArrowLeft'
import CreateUserDialog from './CreateUserDialog'

function UserManagement() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [userToDelete, setUserToDelete] = useState(null)
  const navigate = useNavigate()

  const fetchUsers = async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await usersAPI.getAll()
      setUsers(response.data)
    } catch (err) {
      console.error('Failed to fetch users:', err)
      setError('Failed to load users')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchUsers()
  }, [])

  const handleCreateUser = async (userData) => {
    try {
      await usersAPI.create(userData)
      setIsCreateDialogOpen(false)
      fetchUsers() // Refresh the list
    } catch (err) {
      throw err
    }
  }

  const handleDeleteConfirm = async () => {
    if (!userToDelete) return

    try {
      setLoading(true)
      await usersAPI.delete(userToDelete.id)
      setUserToDelete(null)
      fetchUsers() // Refresh the list
    } catch (err) {
      console.error('Failed to delete user:', err)
      setError('Failed to delete user')
    } finally {
      setLoading(false)
    }
  }

  return (
    <View padding="size-400" backgroundColor="gray-50" minHeight="100vh">
      <Flex direction="column" gap="size-300">
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Flex direction="row" gap="size-200" alignItems="center">
            <ActionButton onPress={() => navigate('/dashboard')} isQuiet>
              <ArrowLeft />
            </ActionButton>
            <Heading level={1}>User Management</Heading>
          </Flex>
          <Button variant="cta" onPress={() => setIsCreateDialogOpen(true)}>
            <Add />
            <Text>Create User</Text>
          </Button>
        </Flex>
        <Divider size="M" />

        {error && (
          <Text>
            <span style={{ color: 'var(--spectrum-global-color-red-600)' }}>{error}</span>
          </Text>
        )}

        <View backgroundColor="gray-100" padding="size-300" borderRadius="medium">
          <TableView
            aria-label="Users table"
            selectionMode="none"
            density="spacious"
            overflowMode="wrap"
          >
            <TableHeader>
              <Column key="username" width="25%">
                Username
              </Column>
              <Column key="role" width="15%">
                Role
              </Column>
              <Column key="enabled" width="15%">
                Status
              </Column>
              <Column key="createdAt" width="25%">
                Created At
              </Column>
              <Column key="actions" width="20%">
                Actions
              </Column>
            </TableHeader>
            <TableBody>
              {users.map((user) => (
                <Row key={user.id}>
                  <Cell>{user.username}</Cell>
                  <Cell>{user.role}</Cell>
                  <Cell>{user.enabled ? 'Enabled' : 'Disabled'}</Cell>
                  <Cell>{new Date(user.createdAt).toLocaleDateString()}</Cell>
                  <Cell>
                    <DialogTrigger>
                      <ActionButton isQuiet>
                        <Delete />
                      </ActionButton>
                      <AlertDialog
                        variant="destructive"
                        title="Delete User"
                        primaryActionLabel="Delete"
                        cancelLabel="Cancel"
                        onPrimaryAction={() => {
                          setUserToDelete(user)
                          handleDeleteConfirm()
                        }}
                      >
                        Are you sure you want to delete user "{user.username}"? This will also
                        delete all their projects and worklog entries.
                      </AlertDialog>
                    </DialogTrigger>
                  </Cell>
                </Row>
              ))}
            </TableBody>
          </TableView>

          {users.length === 0 && !loading && (
            <Flex justifyContent="center" marginTop="size-300">
              <Text>No users found</Text>
            </Flex>
          )}
        </View>
      </Flex>

      {isCreateDialogOpen && (
        <CreateUserDialog
          isOpen={isCreateDialogOpen}
          onClose={() => setIsCreateDialogOpen(false)}
          onSubmit={handleCreateUser}
        />
      )}
    </View>
  )
}

export default UserManagement
