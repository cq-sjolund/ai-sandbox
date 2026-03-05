import React from 'react'
import {
  Dialog,
  DialogTrigger,
  Heading,
  Divider,
  Content,
  ButtonGroup,
  Button,
  Flex,
  View,
  Text
} from '@adobe/react-spectrum'
import { useWorklog } from '../../contexts/WorklogContext'
import { useProjects } from '../../contexts/ProjectContext'

export default function EntryListDialog({ isOpen, onClose, selectedDate, entries, onEditEntry }) {
  const { deleteEntry } = useWorklog()
  const { projects } = useProjects()

  const handleDelete = async (entryId) => {
    if (window.confirm('Are you sure you want to delete this entry?')) {
      try {
        await deleteEntry(entryId)
        if (entries.length <= 1) {
          onClose()
        }
      } catch (err) {
        console.error('Failed to delete entry:', err)
      }
    }
  }

  const getProjectForEntry = (entry) => {
    return projects.find(p => p.id === entry.projectId)
  }

  const totalHours = entries.reduce((sum, e) => sum + parseFloat(e.hours), 0)

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={(open) => !open && onClose()}>
      <div />
      {(close) => (
        <Dialog size="L">
          <Heading>Entries for {selectedDate?.toString()}</Heading>
          <Divider />
          <Content>
            <Flex direction="column" gap="size-200">
              <Text>Total Hours: <strong>{totalHours.toFixed(1)}h</strong></Text>

              {entries.map((entry) => {
                const project = getProjectForEntry(entry)
                return (
                  <View
                    key={entry.id}
                    backgroundColor="gray-100"
                    padding="size-200"
                    borderRadius="medium"
                  >
                    <Flex direction="column" gap="size-100">
                      <Flex direction="row" justifyContent="space-between" alignItems="center">
                        <Flex direction="row" gap="size-100" alignItems="center">
                          <View
                            width="size-150"
                            height="size-150"
                            borderRadius="50%"
                            UNSAFE_style={{ backgroundColor: project?.colorCode || '#999' }}
                          />
                          <Text><strong>{entry.summary}</strong></Text>
                        </Flex>
                        <Text>{entry.hours}h</Text>
                      </Flex>

                      <Text>{project?.name || 'Unknown Project'}</Text>
                      <Text UNSAFE_style={{ fontSize: '14px', color: '#666' }}>
                        {entry.description}
                      </Text>

                      <Flex direction="row" gap="size-100" marginTop="size-100">
                        <Button
                          variant="secondary"
                          onPress={() => onEditEntry(entry)}
                          size="S"
                        >
                          Edit
                        </Button>
                        <Button
                          variant="negative"
                          onPress={() => handleDelete(entry.id)}
                          size="S"
                        >
                          Delete
                        </Button>
                      </Flex>
                    </Flex>
                  </View>
                )
              })}
            </Flex>
          </Content>
          <ButtonGroup>
            <Button variant="secondary" onPress={close}>
              Close
            </Button>
          </ButtonGroup>
        </Dialog>
      )}
    </DialogTrigger>
  )
}
