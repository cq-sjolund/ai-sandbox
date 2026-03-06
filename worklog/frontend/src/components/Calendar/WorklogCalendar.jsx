import React, { useState } from 'react'
import { View, Heading, Button, Flex, Text, Well, Grid, ActionButton, ButtonGroup, Divider } from '@adobe/react-spectrum'
import { DatePicker } from '@react-spectrum/datepicker'
import { parseDate, today, getLocalTimeZone } from '@internationalized/date'
import { useWorklog } from '../../contexts/WorklogContext'
import { useProjects } from '../../contexts/ProjectContext'
import EntryDialog from '../Entry/EntryDialog'
import EntryListDialog from '../Entry/EntryListDialog'
import CalendarGridView from './CalendarGridView'

export default function WorklogCalendar() {
  const { entries, deleteEntry, fetchEntries } = useWorklog()
  const { projects } = useProjects()
  const [selectedDate, setSelectedDate] = useState(today(getLocalTimeZone()))
  const [isDialogOpen, setIsDialogOpen] = useState(false)
  const [isListDialogOpen, setIsListDialogOpen] = useState(false)
  const [editingEntry, setEditingEntry] = useState(null)
  const [viewMode, setViewMode] = useState('list') // 'list' or 'calendar'
  const [listViewMonth, setListViewMonth] = useState(today(getLocalTimeZone()))

  const handleDateSelect = (date) => {
    if (!date) return
    setSelectedDate(date)
    const dateStr = date.toString()
    const dayEntries = entries.filter(e => e.entryDate === dateStr)

    if (dayEntries.length > 0) {
      setIsListDialogOpen(true)
    } else {
      setEditingEntry(null)
      setIsDialogOpen(true)
    }
  }

  const handleCreateEntry = () => {
    setEditingEntry(null)
    setIsDialogOpen(true)
  }

  const handleEditEntry = (entry) => {
    setEditingEntry(entry)
    setIsListDialogOpen(false)
    setIsDialogOpen(true)
  }

  const handleCalendarDateClick = (year, month, day) => {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    try {
      const date = parseDate(dateStr)
      setSelectedDate(date)
      const dayEntries = entries.filter(e => e.entryDate === dateStr)

      if (dayEntries.length > 0) {
        setIsListDialogOpen(true)
      } else {
        setEditingEntry(null)
        setIsDialogOpen(true)
      }
    } catch (e) {
      console.error('Invalid date:', dateStr)
    }
  }

  const getEntriesForDate = (date) => {
    const dateStr = date.toString()
    return entries.filter(e => e.entryDate === dateStr)
  }

  // Get current month from selectedDate for calendar view
  const currentMonth = {
    year: selectedDate.year,
    month: selectedDate.month
  }

  const handlePreviousMonth = () => {
    const newDate = selectedDate.subtract({ months: 1 })
    setSelectedDate(newDate)
  }

  const handleNextMonth = () => {
    const newDate = selectedDate.add({ months: 1 })
    setSelectedDate(newDate)
  }

  const handleToday = () => {
    setSelectedDate(today(getLocalTimeZone()))
  }

  const handleDeleteEntry = async (entryId) => {
    if (window.confirm('Are you sure you want to delete this entry?')) {
      try {
        await deleteEntry(entryId)
      } catch (err) {
        console.error('Failed to delete entry:', err)
        alert('Failed to delete entry. Please try again.')
      }
    }
  }

  const handleEditClick = (entry) => {
    setEditingEntry(entry)
    setIsDialogOpen(true)
  }

  // Group entries by date for display
  const entriesByDate = entries.reduce((acc, entry) => {
    if (!acc[entry.entryDate]) {
      acc[entry.entryDate] = []
    }
    acc[entry.entryDate].push(entry)
    return acc
  }, {})

  // Filter entries for the current list view month
  const listViewYear = listViewMonth.year
  const listViewMonthNum = listViewMonth.month
  const monthStart = `${listViewYear}-${String(listViewMonthNum).padStart(2, '0')}-01`
  const monthEnd = `${listViewYear}-${String(listViewMonthNum).padStart(2, '0')}-31`

  const recentDates = Object.keys(entriesByDate)
    .filter(dateStr => dateStr >= monthStart && dateStr <= monthEnd)
    .sort((a, b) => b.localeCompare(a))

  const handlePreviousListMonth = () => {
    const newDate = listViewMonth.subtract({ months: 1 })
    setListViewMonth(newDate)
  }

  const handleNextListMonth = () => {
    const newDate = listViewMonth.add({ months: 1 })
    setListViewMonth(newDate)
  }

  const handleTodayList = () => {
    setListViewMonth(today(getLocalTimeZone()))
  }

  // Format month/year for display
  const monthNames = ['January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December']
  const listViewMonthName = `${monthNames[listViewMonthNum - 1]} ${listViewYear}`

  return (
    <View>
      <Flex direction="column" gap="size-300">
        <Flex direction="row" justifyContent="space-between" alignItems="center">
          <Heading level={2}>Worklog Calendar</Heading>
          <Flex direction="row" gap="size-200">
            <ButtonGroup>
              <Button
                variant={viewMode === 'list' ? 'primary' : 'secondary'}
                onPress={() => setViewMode('list')}
              >
                List View
              </Button>
              <Button
                variant={viewMode === 'calendar' ? 'primary' : 'secondary'}
                onPress={() => setViewMode('calendar')}
              >
                Calendar View
              </Button>
            </ButtonGroup>
            <Button variant="cta" onPress={handleCreateEntry}>
              Add Entry
            </Button>
          </Flex>
        </Flex>

        {viewMode === 'list' && (
          <DatePicker
            label="Select Date"
            value={selectedDate}
            onChange={handleDateSelect}
            width="100%"
          />
        )}

        {viewMode === 'list' ? (
          <View>
            <Flex direction="row" justifyContent="space-between" alignItems="center" marginBottom="size-200">
              <Heading level={3}>Entries for {listViewMonthName}</Heading>
              <Flex direction="row" gap="size-100">
                <Button variant="secondary" onPress={handlePreviousListMonth}>
                  Previous Month
                </Button>
                <Button variant="secondary" onPress={handleTodayList}>
                  Current Month
                </Button>
                <Button variant="secondary" onPress={handleNextListMonth}>
                  Next Month
                </Button>
              </Flex>
            </Flex>
            <Flex direction="column" gap="size-100">
              {recentDates.length === 0 && (
                <Well>
                  <Text>No worklog entries for {listViewMonthName}. Click "Add Entry" to get started!</Text>
                </Well>
              )}

              {recentDates.map(dateStr => {
                const dayEntries = entriesByDate[dateStr]
                const totalHours = dayEntries.reduce((sum, e) => sum + parseFloat(e.hours), 0)

                return (
                  <View key={dateStr} marginBottom="size-200">
                    <Flex direction="row" justifyContent="space-between" alignItems="center" marginBottom="size-100">
                      <Text><strong>{dateStr}</strong></Text>
                      <Text><strong>{totalHours.toFixed(1)}h total</strong></Text>
                    </Flex>

                    {dayEntries.map((entry, idx) => {
                      const project = projects.find(p => p.id === entry.projectId)
                      return (
                        <View
                          key={entry.id}
                          backgroundColor="gray-100"
                          padding="size-200"
                          borderRadius="medium"
                          marginBottom="size-100"
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
                            <Text UNSAFE_style={{ fontSize: '13px', color: '#666' }}>
                              {project?.name || 'Unknown Project'}
                            </Text>
                            <Text UNSAFE_style={{ fontSize: '13px', color: '#555' }}>
                              {entry.description}
                            </Text>
                            <Divider size="S" marginTop="size-100" marginBottom="size-100" />
                            <Flex direction="row" gap="size-100" wrap>
                              <Button
                                variant="secondary"
                                size="S"
                                onPress={() => handleEditClick(entry)}
                              >
                                Edit
                              </Button>
                              <Button
                                variant="negative"
                                size="S"
                                onPress={() => handleDeleteEntry(entry.id)}
                              >
                                Delete
                              </Button>
                            </Flex>
                          </Flex>
                        </View>
                      )
                    })}
                  </View>
                )
              })}
            </Flex>
          </View>
        ) : (
          <View>
            <Flex direction="row" justifyContent="space-between" alignItems="center" marginBottom="size-200">
              <Button variant="secondary" onPress={handlePreviousMonth}>
                Previous
              </Button>
              <Button variant="secondary" onPress={handleToday}>
                Today
              </Button>
              <Button variant="secondary" onPress={handleNextMonth}>
                Next
              </Button>
            </Flex>
            <CalendarGridView
              entries={entries}
              currentMonth={currentMonth}
              onDateClick={handleCalendarDateClick}
              onEntryClick={handleEditClick}
            />
          </View>
        )}
      </Flex>

      <EntryDialog
        isOpen={isDialogOpen}
        onClose={() => {
          setIsDialogOpen(false)
          setEditingEntry(null)
        }}
        selectedDate={selectedDate}
        editingEntry={editingEntry}
      />

      <EntryListDialog
        isOpen={isListDialogOpen}
        onClose={() => setIsListDialogOpen(false)}
        selectedDate={selectedDate}
        entries={getEntriesForDate(selectedDate)}
        onEditEntry={handleEditEntry}
      />
    </View>
  )
}
