import React, { useState } from 'react'
import { Grid, View, Heading, Divider, Flex, Button, Text } from '@adobe/react-spectrum'
import { useAuth } from '../../contexts/AuthContext'
import { useNavigate } from 'react-router-dom'
import { WorklogProvider } from '../../contexts/WorklogContext'
import { ProjectProvider } from '../../contexts/ProjectContext'
import WorklogCalendar from '../Calendar/WorklogCalendar'
import AISummaryPanel from '../AI/AISummaryPanel'
import ProjectManager from '../Projects/ProjectManager'
import DynamicsManualImport from '../Dynamics/DynamicsManualImport'

function Dashboard() {
  const [selectedDate, setSelectedDate] = useState(null)
  const [showDynamicsImport, setShowDynamicsImport] = useState(false)
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
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
              <Flex direction="row" justifyContent="space-between" alignItems="center">
                <Heading level={1} UNSAFE_style={{ margin: 0 }}>Consultant Worklog</Heading>
                <Flex direction="row" gap="size-300" alignItems="center">
                  <Text>Welcome, {user?.username}</Text>
                  <Button variant="accent" onPress={() => setShowDynamicsImport(true)}>
                    Import from Dynamics
                  </Button>
                  {user?.role === 'ADMIN' && (
                    <Button variant="secondary" onPress={() => navigate('/admin/users')}>
                      Manage Users
                    </Button>
                  )}
                  <Button variant="primary" onPress={handleLogout}>
                    Logout
                  </Button>
                </Flex>
              </Flex>
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

        {/* Dynamics Import Dialog */}
        <DynamicsManualImport
          isOpen={showDynamicsImport}
          onClose={() => setShowDynamicsImport(false)}
        />
      </WorklogProvider>
    </ProjectProvider>
  )
}

export default Dashboard
