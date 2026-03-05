import React, { useState } from 'react'
import {
  View,
  Heading,
  Button,
  Flex,
  Text,
  ProgressCircle,
  Well,
  ButtonGroup,
  Picker,
  Item,
  Tabs,
  TabList,
  TabPanels,
  TextArea
} from '@adobe/react-spectrum'
import { DatePicker } from '@react-spectrum/datepicker'
import { today, getLocalTimeZone } from '@internationalized/date'
import { aiAPI } from '../../api/client'
import { useProjects } from '../../contexts/ProjectContext'

export default function AISummaryPanel() {
  const { projects } = useProjects()
  const [dateRangeStart, setDateRangeStart] = useState(
    today(getLocalTimeZone()).subtract({ weeks: 1 })
  )
  const [dateRangeEnd, setDateRangeEnd] = useState(today(getLocalTimeZone()))
  const [selectedProjects, setSelectedProjects] = useState(new Set(['all']))
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  // Ask AI state
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState(null)
  const [askLoading, setAskLoading] = useState(false)
  const [askError, setAskError] = useState(null)

  const handleGenerateSummary = async () => {
    try {
      setLoading(true)
      setError(null)
      setSummary(null)

      // Convert selected projects to array of IDs
      const projectIds = selectedProjects.has('all')
        ? null
        : Array.from(selectedProjects).map(id => Number(id))

      const response = await aiAPI.generateSummary({
        dateRangeStart: dateRangeStart.toString(),
        dateRangeEnd: dateRangeEnd.toString(),
        projectIds: projectIds,
        customPrompt: null
      })

      setSummary(response.data.summary)
    } catch (err) {
      console.error('Failed to generate summary:', err)
      setError(err.response?.data?.message || 'Failed to generate AI summary')
    } finally {
      setLoading(false)
    }
  }

  const handleQuickRange = (type) => {
    const now = today(getLocalTimeZone())
    switch (type) {
      case 'week':
        setDateRangeStart(now.subtract({ weeks: 1 }))
        setDateRangeEnd(now)
        break
      case 'month':
        setDateRangeStart(now.subtract({ months: 1 }))
        setDateRangeEnd(now)
        break
      case 'quarter':
        setDateRangeStart(now.subtract({ months: 3 }))
        setDateRangeEnd(now)
        break
    }
  }

  const handleAskQuestion = async () => {
    if (!question.trim()) {
      setAskError('Please enter a question')
      return
    }

    try {
      setAskLoading(true)
      setAskError(null)
      setAnswer(null)

      const response = await aiAPI.askQuestion(question)
      setAnswer(response.data.answer)
    } catch (err) {
      console.error('Failed to get answer:', err)
      setAskError(err.response?.data?.message || 'Failed to get AI answer')
    } finally {
      setAskLoading(false)
    }
  }

  return (
    <View>
      <Flex direction="column" gap="size-200">
        <Heading level={3}>AI Assistant</Heading>

        <Tabs aria-label="AI Assistant Tabs" defaultSelectedKey="summary">
          <TabList>
            <Item key="summary">AI Summary</Item>
            <Item key="ask">Ask AI</Item>
          </TabList>
          <TabPanels>
            <Item key="summary">
              <Flex direction="column" gap="size-200" marginTop="size-200">
                <ButtonGroup>
                  <Button variant="secondary" onPress={() => handleQuickRange('week')} size="S">
                    Last Week
                  </Button>
                  <Button variant="secondary" onPress={() => handleQuickRange('month')} size="S">
                    Last Month
                  </Button>
                  <Button variant="secondary" onPress={() => handleQuickRange('quarter')} size="S">
                    Last Quarter
                  </Button>
                </ButtonGroup>

                <DatePicker
                  label="Start Date"
                  value={dateRangeStart}
                  onChange={setDateRangeStart}
                />

                <DatePicker
                  label="End Date"
                  value={dateRangeEnd}
                  onChange={setDateRangeEnd}
                />

                <Picker
                  label="Projects"
                  selectedKey={selectedProjects.has('all') ? 'all' : Array.from(selectedProjects)[0]}
                  onSelectionChange={(key) => {
                    if (key === 'all') {
                      setSelectedProjects(new Set(['all']))
                    } else {
                      setSelectedProjects(new Set([key]))
                    }
                  }}
                >
                  <Item key="all">All Projects</Item>
                  {projects.map(project => (
                    <Item key={project.id}>{project.name}</Item>
                  ))}
                </Picker>

                <Button
                  variant="cta"
                  onPress={handleGenerateSummary}
                  isDisabled={loading}
                >
                  {loading ? 'Generating...' : 'Generate Summary'}
                </Button>

                {loading && (
                  <Flex justifyContent="center" marginTop="size-200">
                    <ProgressCircle aria-label="Loading summary" isIndeterminate />
                  </Flex>
                )}

                {error && (
                  <Well>
                    <Text UNSAFE_style={{ color: 'red' }}>{error}</Text>
                  </Well>
                )}

                {summary && !loading && (
                  <Well>
                    <Text UNSAFE_style={{ whiteSpace: 'pre-wrap' }}>{summary}</Text>
                  </Well>
                )}
              </Flex>
            </Item>

            <Item key="ask">
              <Flex direction="column" gap="size-200" marginTop="size-200">
                <Text>
                  Ask questions about your worklog data. For example: "How many hours did I work on Project A this month?" or "What was my most productive day last week?"
                </Text>

                <TextArea
                  label="Your Question"
                  value={question}
                  onChange={setQuestion}
                  placeholder="e.g., How many hours did I work in project A?"
                  height="size-1200"
                  width="100%"
                />

                <Button
                  variant="cta"
                  onPress={handleAskQuestion}
                  isDisabled={askLoading || !question.trim()}
                >
                  {askLoading ? 'Asking AI...' : 'Ask Question'}
                </Button>

                {askLoading && (
                  <Flex justifyContent="center" marginTop="size-200">
                    <ProgressCircle aria-label="Getting answer" isIndeterminate />
                  </Flex>
                )}

                {askError && (
                  <Well>
                    <Text UNSAFE_style={{ color: 'red' }}>{askError}</Text>
                  </Well>
                )}

                {answer && !askLoading && (
                  <Well>
                    <Text UNSAFE_style={{ whiteSpace: 'pre-wrap' }}>{answer}</Text>
                  </Well>
                )}
              </Flex>
            </Item>
          </TabPanels>
        </Tabs>
      </Flex>
    </View>
  )
}
