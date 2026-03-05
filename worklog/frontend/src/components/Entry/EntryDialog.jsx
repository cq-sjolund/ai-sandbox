import React, { useState, useEffect } from 'react'
import {
  Dialog,
  DialogTrigger,
  Heading,
  Divider,
  Content,
  ButtonGroup,
  Button,
  Form,
  TextField,
  TextArea,
  Picker,
  Item,
  NumberField,
  Flex,
  Text,
  View
} from '@adobe/react-spectrum'
import { DatePicker } from '@react-spectrum/datepicker'
import { useWorklog } from '../../contexts/WorklogContext'
import { useProjects } from '../../contexts/ProjectContext'
import { aiAPI } from '../../api/client'
import { parseDate } from '@internationalized/date'

export default function EntryDialog({ isOpen, onClose, selectedDate, editingEntry }) {
  const { createEntry, updateEntry } = useWorklog()
  const { projects } = useProjects()

  const [formData, setFormData] = useState({
    entryDate: selectedDate?.toString() || '',
    summary: '',
    description: '',
    hours: 1.0,
    projectId: null
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [aiSuggestion, setAiSuggestion] = useState('')
  const [suggestionLoading, setSuggestionLoading] = useState(false)

  useEffect(() => {
    if (isOpen) {
      if (editingEntry) {
        const projectId = editingEntry.projectId || editingEntry.project?.id
        setFormData({
          entryDate: editingEntry.entryDate,
          summary: editingEntry.summary,
          description: editingEntry.description,
          hours: parseFloat(editingEntry.hours),
          projectId: projectId ? Number(projectId) : null
        })
      } else if (selectedDate) {
        setFormData({
          entryDate: selectedDate.toString(),
          summary: '',
          description: '',
          hours: 1.0,
          projectId: null
        })
      }
    }
  }, [editingEntry, selectedDate, isOpen])

  // AI auto-complete for description
  useEffect(() => {
    // Only suggest if we have summary, project, and some description text
    if (!formData.summary || !formData.projectId || !formData.description || formData.description.length < 10) {
      setAiSuggestion('')
      return
    }

    const timeoutId = setTimeout(async () => {
      try {
        setSuggestionLoading(true)
        const project = projects.find(p => p.id === formData.projectId)
        const projectName = project?.name || ''

        console.log('Requesting AI completion with:', {
          description: formData.description,
          summary: formData.summary,
          projectName
        })

        const response = await aiAPI.completeDescription(
          formData.description,
          formData.summary,
          projectName
        )

        console.log('AI completion response:', response.data)

        if (response.data.completion && response.data.completion.trim().length > 0) {
          console.log('Setting AI suggestion:', response.data.completion)
          setAiSuggestion(response.data.completion)
        } else {
          console.log('No completion in response or empty completion')
          setAiSuggestion('')
        }
      } catch (err) {
        console.error('Failed to get AI suggestion:', err)
      } finally {
        setSuggestionLoading(false)
      }
    }, 1500) // Wait 1.5 seconds after user stops typing

    return () => clearTimeout(timeoutId)
  }, [formData.description, formData.summary, formData.projectId, projects])

  const handleAcceptSuggestion = () => {
    if (aiSuggestion) {
      setFormData({ ...formData, description: formData.description + ' ' + aiSuggestion })
      setAiSuggestion('')
    }
  }

  const handleSubmit = async () => {
    try {
      setLoading(true)
      setError(null)

      if (!formData.summary || !formData.description || !formData.projectId || formData.hours <= 0) {
        setError('Please fill in all required fields')
        return
      }

      const entryData = {
        ...formData,
        hours: formData.hours.toString()
      }

      if (editingEntry) {
        await updateEntry(editingEntry.id, entryData)
      } else {
        await createEntry(entryData)
      }

      // Clear form and close
      setFormData({
        entryDate: selectedDate?.toString() || '',
        summary: '',
        description: '',
        hours: 1.0,
        projectId: null
      })
      setError(null)
      onClose()
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save entry')
    } finally {
      setLoading(false)
    }
  }

  const handleClose = () => {
    setError(null)
    onClose()
  }

  const handleDateChange = (date) => {
    setFormData({ ...formData, entryDate: date.toString() })
  }

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <div />
      {(close) => (
        <Dialog>
          <Heading>{editingEntry ? 'Edit Entry' : 'New Worklog Entry'}</Heading>
          <Divider />
          <Content>
            <Form>
              <DatePicker
                label="Date"
                value={formData.entryDate ? parseDate(formData.entryDate) : null}
                onChange={handleDateChange}
                isRequired
              />

              <TextField
                label="Summary"
                value={formData.summary}
                onChange={(value) => setFormData({ ...formData, summary: value })}
                maxLength={255}
                isRequired
              />

              <NumberField
                label="Hours"
                value={formData.hours}
                onChange={(value) => setFormData({ ...formData, hours: value })}
                minValue={0.0}
                maxValue={24}
                step={0.5}
                isRequired
              />

              <Picker
                label="Project"
                selectedKey={formData.projectId ? String(formData.projectId) : null}
                onSelectionChange={(key) => setFormData({ ...formData, projectId: Number(key) })}
                placeholder="Select a project"
                isRequired
              >
                {projects.map(project => (
                  <Item key={String(project.id)} textValue={project.name}>
                    <Flex direction="row" gap="size-100" alignItems="center" UNSAFE_style={{ width: '100%' }}>
                      <View
                        width="size-150"
                        height="size-150"
                        borderRadius="50%"
                        UNSAFE_style={{ backgroundColor: project.colorCode, flexShrink: 0 }}
                      />
                      <Text UNSAFE_style={{ whiteSpace: 'nowrap' }}>{project.name}</Text>
                    </Flex>
                  </Item>
                ))}
              </Picker>

              <View width="100%">
                <TextArea
                  label="Description"
                  value={formData.description}
                  onChange={(value) => {
                    setFormData({ ...formData, description: value })
                    setAiSuggestion('') // Clear suggestion when user types
                  }}
                  height="size-1200"
                  isRequired
                  width="100%"
                />

                {suggestionLoading && (
                  <Text UNSAFE_style={{ fontSize: '12px', color: '#999', marginTop: '4px' }}>
                    AI is thinking...
                  </Text>
                )}

                {aiSuggestion && !suggestionLoading && (
                  <View marginTop="size-100">
                    <Flex direction="column" gap="size-100">
                      <Text UNSAFE_style={{ fontSize: '13px', color: '#666', fontStyle: 'italic', padding: '8px', backgroundColor: '#f5f5f5', borderRadius: '4px' }}>
                        💡 AI Suggestion: {aiSuggestion}
                      </Text>
                      <Button variant="secondary" onPress={handleAcceptSuggestion} isQuiet>
                        Accept Suggestion
                      </Button>
                    </Flex>
                  </View>
                )}
              </View>

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
              {loading ? 'Saving...' : 'Save'}
            </Button>
          </ButtonGroup>
        </Dialog>
      )}
    </DialogTrigger>
  )
}
