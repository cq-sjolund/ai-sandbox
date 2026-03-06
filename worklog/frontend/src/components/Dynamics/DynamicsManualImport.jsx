import React, { useState } from 'react'
import {
  Dialog,
  DialogTrigger,
  Heading,
  Divider,
  Content,
  ButtonGroup,
  Button,
  Text,
  View,
  ProgressCircle,
  Flex,
  Well,
  TextArea,
  Picker,
  Item,
  TextField
} from '@adobe/react-spectrum'
import { useWorklog } from '../../contexts/WorklogContext'
import { dynamicsAPI } from '../../api/client'

export default function DynamicsManualImport({ isOpen, onClose }) {
  const { fetchEntries } = useWorklog()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)
  const [jsonData, setJsonData] = useState('')
  const [phase, setPhase] = useState('input') // 'input', 'analyzing', 'mapping', 'importing', 'done'
  const [mappings, setMappings] = useState([])
  const [projectMappings, setProjectMappings] = useState({})
  const [existingProjects, setExistingProjects] = useState([])

  const handleAnalyze = async () => {
    if (!jsonData.trim()) {
      setError('Please paste the JSON data from Dynamics')
      return
    }

    try {
      setLoading(true)
      setPhase('analyzing')
      setError(null)
      setResult(null)

      // Parse the JSON data
      let parsedData
      try {
        parsedData = JSON.parse(jsonData)
      } catch (e) {
        throw new Error('Invalid JSON format. Please copy the exact JSON from the browser console.')
      }

      const entries = parsedData.value || []

      if (entries.length === 0) {
        setResult({ imported: 0, skipped: 0, message: 'No entries found in the data.' })
        setPhase('done')
        setLoading(false)
        return
      }

      console.log(`Found ${entries.length} entries to analyze`)

      // Transform entries for analysis
      const transformedEntries = entries.map(entry => ({
        dynamicsId: entry.msdyn_timeentryid,
        date: entry.msdyn_date.split('T')[0],
        hours: (entry.msdyn_duration || 0) / 60,
        description: entry.msdyn_description || '',
        project: entry.msdyn_project?.msdyn_subject || null
      }))

      // Analyze project mappings with AI
      const analysisResponse = await dynamicsAPI.analyzeMappings({
        entries: transformedEntries
      })

      console.log('Analysis response:', analysisResponse.data)

      if (analysisResponse.data.success) {
        const mappingsData = analysisResponse.data.mappings || []
        const existingProjectNames = analysisResponse.data.existingProjects || []

        setMappings(mappingsData)
        setExistingProjects(existingProjectNames)

        // Initialize project mappings with AI suggestions
        const initialMappings = {}
        mappingsData.forEach(mapping => {
          if (mapping.suggestedProjectName) {
            initialMappings[mapping.dynamicsProjectName] = mapping.suggestedProjectName
          } else {
            // No match, default to creating new project with same name
            initialMappings[mapping.dynamicsProjectName] = mapping.dynamicsProjectName
          }
        })
        setProjectMappings(initialMappings)

        // Check if any mappings require user confirmation
        const needsConfirmation = mappingsData.some(m => m.requiresConfirmation)

        if (needsConfirmation) {
          setPhase('mapping')
        } else {
          // All high confidence, proceed directly to import
          await performImport(transformedEntries, initialMappings)
        }
      } else {
        throw new Error(analysisResponse.data.message || 'Failed to analyze mappings')
      }

    } catch (err) {
      console.error('Analysis error:', err)
      setError(err.response?.data?.message || err.message || 'Failed to analyze entries')
      setPhase('input')
    } finally {
      setLoading(false)
    }
  }

  const performImport = async (entries, mappings) => {
    try {
      setLoading(true)
      setPhase('importing')
      setError(null)

      console.log('Importing with mappings:', mappings)

      // Send entries to our backend for import with mappings
      const importResponse = await dynamicsAPI.importEntries({
        entries,
        projectMappings: mappings
      })

      console.log('Import response:', importResponse.data)

      const resultData = importResponse.data || {}
      setResult({
        success: resultData.success !== false,
        imported: resultData.imported || 0,
        skipped: resultData.skipped || 0,
        message: resultData.message || 'Import completed'
      })

      setPhase('done')

      // Reload entries to show imported data
      await fetchEntries()

      if (resultData.success !== false) {
        setTimeout(() => {
          handleClose()
        }, 3000)
      }

    } catch (err) {
      console.error('Import error:', err)
      setError(err.response?.data?.message || err.message || 'Failed to import entries')
      setPhase('mapping')
    } finally {
      setLoading(false)
    }
  }

  const handleConfirmMappings = async () => {
    // Parse entries again and perform import
    const parsedData = JSON.parse(jsonData)
    const transformedEntries = parsedData.value.map(entry => ({
      dynamicsId: entry.msdyn_timeentryid,
      date: entry.msdyn_date.split('T')[0],
      hours: (entry.msdyn_duration || 0) / 60,
      description: entry.msdyn_description || '',
      project: entry.msdyn_project?.msdyn_subject || null
    }))

    await performImport(transformedEntries, projectMappings)
  }

  const handleMappingChange = (dynamicsProject, targetProject) => {
    setProjectMappings(prev => ({
      ...prev,
      [dynamicsProject]: targetProject
    }))
  }

  const handleClose = () => {
    setError(null)
    setResult(null)
    setJsonData('')
    setPhase('input')
    setMappings([])
    setProjectMappings({})
    setExistingProjects([])
    onClose()
  }

  const startDate = new Date('2020-01-01')
  const endDate = new Date()

  const startDateStr = startDate.toISOString().split('T')[0]
  const endDateStr = endDate.toISOString().split('T')[0]

  const fetchScript = `// Copy this entire script - fetches ALL your entries with pagination
const startDate = '${startDateStr}';
const endDate = '${endDateStr}';

(async function() {
  try {
    // First get your user ID and bookable resources
    const whoamiResponse = await fetch('/api/data/v9.0/WhoAmI', {
      headers: {
        'Accept': 'application/json',
        'OData-MaxVersion': '4.0',
        'OData-Version': '4.0'
      }
    });
    const whoami = await whoamiResponse.json();
    console.log('Your User ID:', whoami.UserId);

    // Get all your bookable resources
    const resourcesResponse = await fetch(\`/api/data/v9.0/bookableresources?$filter=_userid_value eq \${whoami.UserId}&$select=bookableresourceid\`, {
      headers: {
        'Accept': 'application/json',
        'OData-MaxVersion': '4.0',
        'OData-Version': '4.0'
      }
    });
    const resources = await resourcesResponse.json();
    console.log('Your Bookable Resources:', resources.value.map(r => r.bookableresourceid));

    // Build filter for all your bookable resources
    const resourceFilters = resources.value.map(r => \`_msdyn_bookableresource_value eq \${r.bookableresourceid}\`).join(' or ');
    const filter = \`(\${resourceFilters}) and msdyn_date ge \${startDate} and msdyn_date le \${endDate}\`;

    // Fetch ALL entries with pagination
    let allEntries = [];
    let url = \`/api/data/v9.0/msdyn_timeentries?$filter=\${encodeURIComponent(filter)}&$select=msdyn_timeentryid,msdyn_date,msdyn_duration,msdyn_description&$expand=msdyn_project($select=msdyn_subject)&$orderby=msdyn_date desc&$top=5000\`;

    while (url) {
      console.log(\`Fetching page... (total so far: \${allEntries.length})\`);
      const response = await fetch(url, {
        headers: {
          'Accept': 'application/json',
          'OData-MaxVersion': '4.0',
          'OData-Version': '4.0'
        }
      });
      const data = await response.json();
      allEntries = allEntries.concat(data.value);

      // Check if there's a next page
      url = data['@odata.nextLink'] || null;
    }

    console.log('✓ Found', allEntries.length, 'entries total');
    console.log('✓ Copy the JSON below:');
    console.log(JSON.stringify({ value: allEntries }, null, 2));
  } catch (err) {
    console.error('Error:', err);
  }
})();`

  return (
    <DialogTrigger isOpen={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <div />
      {(close) => (
        <Dialog size="L">
          <Heading>Import from Microsoft Dynamics (Manual)</Heading>
          <Divider />
          <Content>
            <View>
              {(phase === 'input' || phase === 'analyzing') && (
                <>
                  <Text>
                    Import your time entries from Microsoft Dynamics 365 with AI-powered project mapping.
                  </Text>

                  <Well marginTop="size-200" marginBottom="size-200">
                    <Heading level={4}>Step 1: Get Data from Dynamics</Heading>
                    <Text marginBottom="size-100">
                      1. Open your Dynamics 365 instance in a new tab:<br/>
                    </Text>
                    <Text marginBottom="size-100">
                      2. Press <strong>F12</strong> to open Developer Tools<br/>
                      3. Go to the <strong>Console</strong> tab<br/>
                      4. Copy and paste this script, then press Enter:
                    </Text>
                    <TextArea
                      width="100%"
                      height="size-2000"
                      value={fetchScript}
                      isReadOnly
                      UNSAFE_style={{ fontFamily: 'monospace', fontSize: '11px' }}
                    />
                    <Text marginTop="size-100">
                      5. Wait for the result to appear in the console<br/>
                      6. Copy the entire JSON output (everything from &#123; to &#125;)<br/>
                      7. Paste it in the box below
                    </Text>
                  </Well>

                  <Heading level={4} marginTop="size-300">Step 2: Paste and Analyze</Heading>
                  <TextArea
                    label="Dynamics JSON Data (paste the entire JSON response)"
                    value={jsonData}
                    onChange={setJsonData}
                    width="100%"
                    height="size-2000"
                    isDisabled={loading}
                  />
                </>
              )}

              {phase === 'mapping' && (
                <>
                  <Text>
                    AI has analyzed your Dynamics projects. Please confirm or adjust the project mappings below.
                  </Text>

                  <Well marginTop="size-200" marginBottom="size-200">
                    <Text>
                      <strong>Instructions:</strong> For each Dynamics project, select which project in your worklog it should map to.
                      You can choose an existing project or create a new one by typing a new name.
                    </Text>
                  </Well>

                  <Flex direction="column" gap="size-300" marginTop="size-300">
                    {mappings.map((mapping, index) => (
                      <View key={index} backgroundColor="gray-100" padding="size-200" borderRadius="medium">
                        <Text UNSAFE_style={{ fontWeight: 'bold', marginBottom: '8px' }}>
                          {mapping.dynamicsProjectName}
                        </Text>
                        <Text UNSAFE_style={{ fontSize: '12px', color: '#666', marginBottom: '12px' }}>
                          AI Suggestion: {mapping.suggestedProjectName || 'Create new project'}
                          ({mapping.confidence} confidence)
                          {mapping.reason && <><br/>Reason: {mapping.reason}</>}
                        </Text>

                        <Flex direction="row" gap="size-200" alignItems="end">
                          <Picker
                            label="Map to Project"
                            selectedKey={projectMappings[mapping.dynamicsProjectName]}
                            onSelectionChange={(key) => handleMappingChange(mapping.dynamicsProjectName, key)}
                            width="size-3000"
                          >
                            {existingProjects.map(proj => (
                              <Item key={proj}>{proj}</Item>
                            ))}
                            <Item key={mapping.dynamicsProjectName}>
                              ✨ Create "{mapping.dynamicsProjectName}"
                            </Item>
                          </Picker>
                          <TextField
                            label="Or type new name"
                            value={projectMappings[mapping.dynamicsProjectName]}
                            onChange={(value) => handleMappingChange(mapping.dynamicsProjectName, value)}
                            width="size-3000"
                          />
                        </Flex>
                      </View>
                    ))}
                  </Flex>
                </>
              )}

              {(phase === 'analyzing' || phase === 'importing') && (
                <Flex direction="column" alignItems="center" gap="size-200" marginTop="size-300">
                  <ProgressCircle aria-label="Processing..." isIndeterminate />
                  <Text>
                    {phase === 'analyzing' ? 'Analyzing projects with AI...' : 'Importing entries...'}
                  </Text>
                </Flex>
              )}

              {error && (
                <View backgroundColor="red-400" padding="size-200" borderRadius="medium" marginTop="size-200">
                  <Text UNSAFE_style={{ color: 'white' }}>
                    ✗ {error}
                  </Text>
                </View>
              )}

              {result && phase === 'done' && (
                <View backgroundColor="green-400" padding="size-200" borderRadius="medium" marginTop="size-200">
                  <Flex direction="column" gap="size-100">
                    <Text UNSAFE_style={{ color: 'white', fontSize: '18px', fontWeight: 'bold' }}>
                      ✓ Import Complete!
                    </Text>
                    <Text UNSAFE_style={{ color: 'white' }}>
                      {result.message || `Imported: ${result.imported} entries, Skipped: ${result.skipped} (already exist)`}
                    </Text>
                  </Flex>
                </View>
              )}
            </View>
          </Content>
          <ButtonGroup>
            <Button variant="secondary" onPress={close}>
              {result ? 'Done' : 'Cancel'}
            </Button>
            {phase === 'input' && (
              <Button
                variant="cta"
                onPress={handleAnalyze}
                isDisabled={loading || !jsonData.trim()}
              >
                Analyze & Import
              </Button>
            )}
            {phase === 'mapping' && (
              <Button
                variant="cta"
                onPress={handleConfirmMappings}
                isDisabled={loading}
              >
                Confirm & Import
              </Button>
            )}
          </ButtonGroup>
        </Dialog>
      )}
    </DialogTrigger>
  )
}
