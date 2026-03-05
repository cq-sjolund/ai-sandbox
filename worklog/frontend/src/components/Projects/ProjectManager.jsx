import React, { useState } from 'react'
import {
  View,
  Heading,
  Button,
  Flex,
  Text,
  Dialog,
  DialogTrigger,
  Content,
  Divider,
  ButtonGroup,
  Form,
  TextField,
  ActionButton,
  AlertDialog
} from '@adobe/react-spectrum'
import { useProjects } from '../../contexts/ProjectContext'
import Add from '@spectrum-icons/workflow/Add'
import Delete from '@spectrum-icons/workflow/Delete'

export default function ProjectManager() {
  const { projects, createProject, deleteProject, countProjectEntries } = useProjects()
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [projectToDelete, setProjectToDelete] = useState(null)
  const [entryCount, setEntryCount] = useState(0)
  const [formData, setFormData] = useState({
    name: '',
    colorCode: '#1473E6',
    description: ''
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async () => {
    try {
      setLoading(true)
      setError(null)

      if (!formData.name || !formData.colorCode) {
        setError('Please fill in all required fields')
        return
      }

      await createProject(formData)
      setFormData({ name: '', colorCode: '#1473E6', description: '' })
      setIsDialogOpen(false)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create project')
    } finally {
      setLoading(false)
    }
  }

  const handleDeleteClick = async (project) => {
    try {
      setProjectToDelete(project)
      const count = await countProjectEntries(project.id)
      setEntryCount(count)
      setDeleteDialogOpen(true)
    } catch (err) {
      console.error('Failed to count entries:', err)
    }
  }

  const handleDeleteConfirm = async (deleteEntries) => {
    try {
      setLoading(true)
      await deleteProject(projectToDelete.id, deleteEntries)
      setDeleteDialogOpen(false)
      setProjectToDelete(null)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to delete project')
    } finally {
      setLoading(false)
    }
  }

  return (
    <View>
      <Flex direction="column" gap="size-200">
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Heading level={3}>Projects</Heading>
          <DialogTrigger isOpen={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <Button variant="primary" isQuiet>
              <Add />
            </Button>
            {(close) => (
              <Dialog>
                <Heading>New Project</Heading>
                <Divider />
                <Content>
                  <Form>
                    <TextField
                      label="Name"
                      value={formData.name}
                      onChange={(value) => setFormData({ ...formData, name: value })}
                      isRequired
                    />

                    <TextField
                      label="Color Code"
                      value={formData.colorCode}
                      onChange={(value) => setFormData({ ...formData, colorCode: value })}
                      description="Hex color (e.g., #FF5733)"
                      isRequired
                    />

                    <TextField
                      label="Description"
                      value={formData.description}
                      onChange={(value) => setFormData({ ...formData, description: value })}
                    />

                    {error && (
                      <Text UNSAFE_style={{ color: 'red' }}>{error}</Text>
                    )}
                  </Form>
                </Content>
                <ButtonGroup>
                  <Button variant="secondary" onPress={close}>
                    Cancel
                  </Button>
                  <Button variant="cta" onPress={handleSubmit} isDisabled={loading}>
                    {loading ? 'Creating...' : 'Create'}
                  </Button>
                </ButtonGroup>
              </Dialog>
            )}
          </DialogTrigger>
        </Flex>

        <Flex direction="column" gap="size-100">
          {projects.map((project) => (
            <Flex
              key={project.id}
              direction="row"
              gap="size-100"
              alignItems="center"
              justifyContent="space-between"
              UNSAFE_style={{ padding: '8px' }}
            >
              <Flex direction="row" gap="size-100" alignItems="center">
                <View
                  width="size-200"
                  height="size-200"
                  borderRadius="50%"
                  UNSAFE_style={{ backgroundColor: project.colorCode }}
                />
                <Text>{project.name}</Text>
              </Flex>
              <ActionButton
                isQuiet
                onPress={() => handleDeleteClick(project)}
              >
                <Delete />
              </ActionButton>
            </Flex>
          ))}
        </Flex>

        <DialogTrigger isOpen={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <div />
          {(close) => (
            <AlertDialog
              variant="destructive"
              title="Delete Project"
              primaryActionLabel={entryCount > 0 ? "Delete Project & Entries" : "Delete Project"}
              secondaryActionLabel={entryCount > 0 ? "Delete Project Only" : null}
              cancelLabel="Cancel"
              onPrimaryAction={() => handleDeleteConfirm(true)}
              onSecondaryAction={entryCount > 0 ? () => handleDeleteConfirm(false) : undefined}
              onCancel={close}
            >
              {entryCount > 0 ? (
                <>
                  <Text>
                    This project has <strong>{entryCount}</strong> worklog {entryCount === 1 ? 'entry' : 'entries'}.
                  </Text>
                  <Text marginTop="size-200">
                    Do you want to delete the project and all related entries, or just the project?
                  </Text>
                  <Text marginTop="size-200" UNSAFE_style={{ fontSize: '12px', color: '#666' }}>
                    Note: Deleting only the project will fail if entries still exist. You must delete entries first or choose to delete both.
                  </Text>
                </>
              ) : (
                <Text>
                  Are you sure you want to delete the project "<strong>{projectToDelete?.name}</strong>"?
                </Text>
              )}
            </AlertDialog>
          )}
        </DialogTrigger>
      </Flex>
    </View>
  )
}
