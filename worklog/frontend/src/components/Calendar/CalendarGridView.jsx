import React from 'react'
import { View, Flex, Text, Heading } from '@adobe/react-spectrum'
import { useProjects } from '../../contexts/ProjectContext'

export default function CalendarGridView({ entries, currentMonth, onDateClick }) {
  const { projects } = useProjects()

  // Get first day of month and number of days
  const year = currentMonth.year
  const month = currentMonth.month
  const firstDay = new Date(year, month - 1, 1)
  const lastDay = new Date(year, month, 0)
  const daysInMonth = lastDay.getDate()
  const startDayOfWeek = firstDay.getDay() // 0 = Sunday

  // Build calendar grid
  const calendarDays = []

  // Add empty cells for days before month starts
  for (let i = 0; i < startDayOfWeek; i++) {
    calendarDays.push(null)
  }

  // Add all days of the month
  for (let day = 1; day <= daysInMonth; day++) {
    calendarDays.push(day)
  }

  // Get entries for a specific date
  const getEntriesForDate = (day) => {
    if (!day) return []
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    return entries.filter(e => e.entryDate === dateStr)
  }

  // Get project color
  const getProjectColor = (projectId) => {
    const project = projects.find(p => p.id === projectId)
    return project?.colorCode || '#999999'
  }

  return (
    <View>
      <Heading level={3} marginBottom="size-200">
        {new Date(year, month - 1).toLocaleDateString('en-US', { month: 'long', year: 'numeric' })}
      </Heading>

      {/* Day headers */}
      <Flex direction="row" gap="size-0">
        {['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'].map(day => (
          <View
            key={day}
            width="14.28%"
            padding="size-100"
            borderColor="gray-300"
            borderWidth="thin"
            backgroundColor="gray-100"
          >
            <Text UNSAFE_style={{ fontWeight: 'bold', textAlign: 'center' }}>{day}</Text>
          </View>
        ))}
      </Flex>

      {/* Calendar grid */}
      <Flex direction="row" wrap="wrap" gap="size-0">
        {calendarDays.map((day, index) => {
          const dayEntries = day ? getEntriesForDate(day) : []
          const totalHours = dayEntries.reduce((sum, e) => sum + parseFloat(e.hours), 0)
          const isToday = day &&
            new Date().getFullYear() === year &&
            new Date().getMonth() + 1 === month &&
            new Date().getDate() === day

          return (
            <View
              key={index}
              width="14.28%"
              minHeight="size-1600"
              padding="size-100"
              borderColor="gray-300"
              borderWidth="thin"
              backgroundColor={!day ? 'gray-50' : isToday ? 'blue-100' : 'gray-100'}
              UNSAFE_style={{
                cursor: day ? 'pointer' : 'default',
                position: 'relative',
                overflow: 'hidden'
              }}
              onClick={() => day && onDateClick(year, month, day)}
            >
              {day && (
                <>
                  <Text UNSAFE_style={{ fontWeight: isToday ? 'bold' : 'normal', fontSize: '14px' }}>
                    {day}
                  </Text>

                  {dayEntries.length > 0 && (
                    <View marginTop="size-50">
                      {dayEntries.slice(0, 2).map((entry, i) => (
                        <View key={i} marginBottom="size-50">
                          <Flex direction="row" gap="size-50" alignItems="center">
                            <View
                              width="size-75"
                              height="size-75"
                              borderRadius="50%"
                              UNSAFE_style={{
                                backgroundColor: getProjectColor(entry.projectId),
                                flexShrink: 0
                              }}
                            />
                            <Text
                              UNSAFE_style={{
                                fontSize: '10px',
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                                lineHeight: '1.2'
                              }}
                              title={`${entry.summary} (${entry.hours}h)`}
                            >
                              {entry.summary}
                            </Text>
                          </Flex>
                        </View>
                      ))}
                      {dayEntries.length > 2 && (
                        <Text UNSAFE_style={{ fontSize: '10px', color: '#666' }}>
                          +{dayEntries.length - 2} more
                        </Text>
                      )}
                      <Text UNSAFE_style={{ fontSize: '11px', marginTop: '4px', fontWeight: 'bold' }}>
                        {totalHours.toFixed(1)}h
                      </Text>
                    </View>
                  )}
                </>
              )}
            </View>
          )
        })}
      </Flex>
    </View>
  )
}
