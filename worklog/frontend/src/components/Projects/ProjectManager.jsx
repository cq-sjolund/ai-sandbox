import React, { useState, useEffect } from 'react'
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
  AlertDialog,
  TooltipTrigger,
  Tooltip
} from '@adobe/react-spectrum'
import { useProjects } from '../../contexts/ProjectContext'
import { aiAPI } from '../../api/client'
import Add from '@spectrum-icons/workflow/Add'
import Delete from '@spectrum-icons/workflow/Delete'
import Edit from '@spectrum-icons/workflow/Edit'
import InfoOutline from '@spectrum-icons/workflow/InfoOutline'

export default function ProjectManager() {
  const { projects, createProject, updateProject, deleteProject, countProjectEntries } = useProjects()
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [projectToDelete, setProjectToDelete] = useState(null)
  const [editingProject, setEditingProject] = useState(null)
  const [entryCount, setEntryCount] = useState(0)
  const [formData, setFormData] = useState({
    name: '',
    colorCode: '#1473E6',
    description: ''
  })
  const [loading, setLoading] = useState(false)
  const [colorLoading, setColorLoading] = useState(false)
  const [error, setError] = useState(null)

  // Auto-suggest color when name changes (only for new projects)
  useEffect(() => {
    if (!formData.name || formData.name.trim().length < 3 || editingProject) {
      return
    }

    const timeoutId = setTimeout(async () => {
      try {
        setColorLoading(true)
        const response = await aiAPI.suggestColor(formData.name)
        setFormData(prev => ({ ...prev, colorCode: response.data.colorCode }))
      } catch (err) {
        console.error('Failed to suggest color:', err)
      } finally {
        setColorLoading(false)
      }
    }, 800)

    return () => clearTimeout(timeoutId)
  }, [formData.name, editingProject])

  const handleSubmit = async () => {
    try {
      setLoading(true)
      setError(null)

      if (!formData.name || !formData.colorCode) {
        setError('Please fill in all required fields')
        return
      }

      if (editingProject) {
        await updateProject(editingProject.id, formData)
      } else {
        await createProject(formData)
      }

      setFormData({ name: '', colorCode: '#1473E6', description: '' })
      setEditingProject(null)
      setIsDialogOpen(false)
    } catch (err) {
      setError(err.response?.data?.message || `Failed to ${editingProject ? 'update' : 'create'} project`)
    } finally {
      setLoading(false)
    }
  }

  const handleEditClick = (project) => {
    setEditingProject(project)
    setFormData({
      name: project.name,
      colorCode: project.colorCode,
      description: project.description || ''
    })
    setIsDialogOpen(true)
  }

  const handleDialogClose = () => {
    setIsDialogOpen(false)
    setEditingProject(null)
    setFormData({ name: '', colorCode: '#1473E6', description: '' })
    setError(null)
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
          <DialogTrigger isOpen={isDialogOpen} onOpenChange={(open) => {
            setIsDialogOpen(open)
            if (!open) handleDialogClose()
          }}>
            <Button variant="primary" isQuiet>
              <Add />
            </Button>
            {(close) => (
              <Dialog>
                <Heading>{editingProject ? 'Edit Project' : 'New Project'}</Heading>
                <Divider />
                <Content>
                  <Form>
                    <TextField
                      label="Name"
                      value={formData.name}
                      onChange={(value) => setFormData({ ...formData, name: value })}
                      isRequired
                    />

                    <Flex direction="row" gap="size-100" alignItems="end">
                      <TextField
                        label="Color Code"
                        value={formData.colorCode}
                        onChange={(value) => setFormData({ ...formData, colorCode: value })}
                        description="Hex color (e.g., #FF5733)"
                        isRequired
                        width="100%"
                      />
                      <View position="relative">
                        <View
                          width="size-600"
                          height="size-600"
                          borderRadius="medium"
                          UNSAFE_style={{
                            backgroundColor: formData.colorCode,
                            border: '1px solid #ccc'
                          }}
                        />
                        {colorLoading && (
                          <View
                            position="absolute"
                            top={0}
                            left={0}
                            width="100%"
                            height="100%"
                            UNSAFE_style={{
                              backgroundColor: 'rgba(255, 255, 255, 0.7)',
                              display: 'flex',
                              alignItems: 'center',
                              justifyContent: 'center',
                              borderRadius: '4px'
                            }}
                          >
                            <Text UNSAFE_style={{ fontSize: '10px' }}>...</Text>
                          </View>
                        )}
                      </View>
                    </Flex>

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
                    {loading ? (editingProject ? 'Updating...' : 'Creating...') : (editingProject ? 'Update' : 'Create')}
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
                {project.description && (
                  <TooltipTrigger delay={0}>
                    <ActionButton isQuiet UNSAFE_style={{ minWidth: 'auto', padding: '4px' }}>
                      <InfoOutline size="S" />
                    </ActionButton>
                    <Tooltip>{project.description}</Tooltip>
                  </TooltipTrigger>
                )}
              </Flex>
              <Flex direction="row" gap="size-100">
                <ActionButton
                  isQuiet
                  onPress={() => handleEditClick(project)}
                >
                  <Edit />
                </ActionButton>
                <ActionButton
                  isQuiet
                  onPress={() => handleDeleteClick(project)}
                >
                  <Delete />
                </ActionButton>
              </Flex>
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
