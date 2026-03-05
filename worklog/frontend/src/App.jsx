import React, { useState } from 'react'
import { Provider, defaultTheme, Grid, View, Heading, Divider } from '@adobe/react-spectrum'
import { WorklogProvider } from './contexts/WorklogContext'
import { ProjectProvider } from './contexts/ProjectContext'
import WorklogCalendar from './components/Calendar/WorklogCalendar'
import AISummaryPanel from './components/AI/AISummaryPanel'
import ProjectManager from './components/Projects/ProjectManager'

function App() {
  const [selectedDate, setSelectedDate] = useState(null)

  return (
    <Provider theme={defaultTheme} colorScheme="light">
      <ProjectProvider>
        <WorklogProvider>
          <View padding="size-400" backgroundColor="gray-50" minHeight="100vh">
            <Grid
              areas={['header', 'main']}
              columns={['1fr']}
              rows={['auto', '1fr']}
              gap="size-300"
            >
              {/* Header */}
              <View gridArea="header">
                <Heading level={1}>Consultant Worklog</Heading>
                <Divider size="M" marginTop="size-100" />
              </View>

              {/* Main Content */}
              <View gridArea="main">
                <Grid
                  areas={['calendar sidebar']}
                  columns={['2fr', '1fr']}
                  gap="size-300"
                  height="100%"
                >
                  {/* Calendar Section */}
                  <View gridArea="calendar" backgroundColor="gray-100" padding="size-300" borderRadius="medium">
                    <WorklogCalendar
                      selectedDate={selectedDate}
                      onDateSelect={setSelectedDate}
                    />
                  </View>

                  {/* Sidebar */}
                  <View gridArea="sidebar">
                    <Grid
                      areas={['projects', 'ai']}
                      rows={['auto', '1fr']}
                      gap="size-300"
                      height="100%"
                    >
                      {/* Projects Section */}
                      <View gridArea="projects" backgroundColor="gray-100" padding="size-300" borderRadius="medium">
                        <ProjectManager />
                      </View>

                      {/* AI Summary Section */}
                      <View gridArea="ai" backgroundColor="gray-100" padding="size-300" borderRadius="medium">
                        <AISummaryPanel />
                      </View>
                    </Grid>
                  </View>
                </Grid>
              </View>
            </Grid>
          </View>
        </WorklogProvider>
      </ProjectProvider>
    </Provider>
  )
}

export default App
