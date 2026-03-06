// Dynamics Bookmarklet Sync Script
// This script runs in the Dynamics tab and syncs with your worklog app

(async function() {
  const WORKLOG_API = 'http://localhost:3001/api';

  // Get JWT token from your worklog app (you'll need to copy this)
  let JWT_TOKEN = prompt('Enter your Worklog JWT token (from browser storage):');

  if (!JWT_TOKEN) {
    alert('Token required. Please get your JWT token from your worklog app.');
    return;
  }

  // Remove quotes if user copied them
  JWT_TOKEN = JWT_TOKEN.replace(/^["']|["']$/g, '').trim();

  console.log('Using token (first 20 chars):', JWT_TOKEN.substring(0, 20) + '...');

  const action = confirm('Click OK to IMPORT from Dynamics, Cancel to EXPORT to Dynamics');

  if (action) {
    await importFromDynamics(JWT_TOKEN);
  } else {
    await exportToDynamics(JWT_TOKEN);
  }

  async function importFromDynamics(token) {
    try {
      console.log('Importing from Dynamics...');

      // Get user's bookable resource ID from worklog app
      console.log('Fetching user profile...');
      const userResponse = await fetch(`${WORKLOG_API}/auth/me`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!userResponse.ok) {
        throw new Error('Failed to fetch user profile. Check your JWT token.');
      }

      const userData = await userResponse.json();
      console.log('User:', userData.username);

      if (!userData.bookableResourceId) {
        alert('Please set your Bookable Resource ID in your Worklog profile first!\n\nGo to Settings → Update Profile to add your Resource ID.');
        return;
      }

      console.log('Using Bookable Resource ID:', userData.bookableResourceId);

      // Get date range from user
      const startDate = prompt('Start date (YYYY-MM-DD):', getFirstDayOfMonth());
      const endDate = prompt('End date (YYYY-MM-DD):', getTodayDate());

      if (!startDate || !endDate) {
        alert('Date range required');
        return;
      }

      // Fetch time entries from Dynamics using current session cookies
      // Filter by date range AND bookable resource
      const filter = `msdyn_date ge ${startDate} and msdyn_date le ${endDate} and _msdyn_bookableresource_value eq ${userData.bookableResourceId}`;
      const dynamicsUrl = `${window.location.origin}/api/data/v9.0/msdyn_timeentries?$filter=${encodeURIComponent(filter)}&$select=msdyn_timeentryid,msdyn_date,msdyn_duration,msdyn_description,_msdyn_project_value`;

      console.log('Fetching from Dynamics:', dynamicsUrl);

      const dynamicsResponse = await fetch(dynamicsUrl);

      if (!dynamicsResponse.ok) {
        throw new Error(`Dynamics API error: ${dynamicsResponse.status}`);
      }

      const dynamicsData = await dynamicsResponse.json();
      const entries = dynamicsData.value || [];

      console.log(`Found ${entries.length} entries in Dynamics`);

      if (entries.length === 0) {
        alert('No time entries found in Dynamics for this date range');
        return;
      }

      // Send entries to worklog app
      const imported = [];
      const skipped = [];

      for (const entry of entries) {
        // Normalize date to YYYY-MM-DD format
        const dateStr = entry.msdyn_date.split('T')[0]; // Remove time component

        const worklogEntry = {
          entryDate: dateStr,
          hours: (entry.msdyn_duration || 0) / 60, // Convert minutes to hours
          summary: extractSummary(entry.msdyn_description),
          description: entry.msdyn_description || '',
          projectId: 1, // Default project - user will need to update
          dynamicsId: entry.msdyn_timeentryid
        };

        try {
          // Check if already exists
          const checkResponse = await fetch(`${WORKLOG_API}/entries/date/${worklogEntry.entryDate}`, {
            headers: {
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'application/json'
            }
          });

          if (!checkResponse.ok) {
            console.error('Failed to check existing entries:', checkResponse.status, await checkResponse.text());
            throw new Error(`Authentication failed (${checkResponse.status}). Check your JWT token.`);
          }

          const existingEntries = await checkResponse.json();

          if (!Array.isArray(existingEntries)) {
            console.error('Invalid response format:', existingEntries);
            throw new Error('Invalid API response format');
          }

          const exists = existingEntries.some(e =>
            e.dynamicsId === worklogEntry.dynamicsId ||
            (e.entryDate === worklogEntry.entryDate && Math.abs(e.hours - worklogEntry.hours) < 0.1)
          );

          if (!exists) {
            // Create new entry
            const createResponse = await fetch(`${WORKLOG_API}/entries`, {
              method: 'POST',
              headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
              },
              body: JSON.stringify(worklogEntry)
            });

            if (createResponse.ok) {
              imported.push(worklogEntry);
              console.log('Imported:', worklogEntry.entryDate);
            } else {
              console.error('Failed to import:', worklogEntry.entryDate, await createResponse.text());
            }
          } else {
            skipped.push(worklogEntry);
            console.log('Skipped (exists):', worklogEntry.entryDate);
          }
        } catch (err) {
          console.error('Error processing entry:', err);
        }
      }

      alert(`Import complete!\n\nImported: ${imported.length}\nSkipped: ${skipped.length}\n\nNote: All entries were assigned to default project. Please update project assignments in your worklog app.`);

    } catch (error) {
      console.error('Import error:', error);
      alert('Import failed: ' + error.message);
    }
  }

  async function exportToDynamics(token) {
    try {
      console.log('Exporting to Dynamics...');

      // Get date range
      const startDate = prompt('Start date (YYYY-MM-DD):', getFirstDayOfMonth());
      const endDate = prompt('End date (YYYY-MM-DD):', getTodayDate());

      if (!startDate || !endDate) {
        alert('Date range required');
        return;
      }

      // Fetch entries from worklog app
      const worklogResponse = await fetch(`${WORKLOG_API}/entries/range?start=${startDate}&end=${endDate}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      });

      if (!worklogResponse.ok) {
        throw new Error(`Worklog API error: ${worklogResponse.status}`);
      }

      const entries = await worklogResponse.json();

      console.log(`Found ${entries.length} entries in worklog`);

      if (entries.length === 0) {
        alert('No entries found in your worklog for this date range');
        return;
      }

      // Export entries to Dynamics
      const exported = [];
      const failed = [];

      for (const entry of entries) {
        try {
          const dynamicsEntry = {
            msdyn_date: entry.entryDate,
            msdyn_duration: Math.round(parseFloat(entry.hours) * 60), // Hours to minutes
            msdyn_description: `${entry.summary}\n\n${entry.description}`,
            msdyn_type: 192350000, // Work
            msdyn_entrystatus: 192350000 // Draft
          };

          let response;

          if (entry.dynamicsId) {
            // Update existing
            const updateUrl = `${window.location.origin}/api/data/v9.0/msdyn_timeentries(${entry.dynamicsId})`;
            response = await fetch(updateUrl, {
              method: 'PATCH',
              headers: {
                'Content-Type': 'application/json',
                'OData-MaxVersion': '4.0',
                'OData-Version': '4.0'
              },
              body: JSON.stringify(dynamicsEntry)
            });
          } else {
            // Create new
            const createUrl = `${window.location.origin}/api/data/v9.0/msdyn_timeentries`;
            response = await fetch(createUrl, {
              method: 'POST',
              headers: {
                'Content-Type': 'application/json',
                'OData-MaxVersion': '4.0',
                'OData-Version': '4.0',
                'Prefer': 'return=representation'
              },
              body: JSON.stringify(dynamicsEntry)
            });
          }

          if (response.ok) {
            exported.push(entry);
            console.log('Exported:', entry.entryDate);
          } else {
            failed.push(entry);
            console.error('Failed to export:', entry.entryDate, await response.text());
          }
        } catch (err) {
          failed.push(entry);
          console.error('Error exporting entry:', err);
        }
      }

      alert(`Export complete!\n\nExported: ${exported.length}\nFailed: ${failed.length}`);

    } catch (error) {
      console.error('Export error:', error);
      alert('Export failed: ' + error.message);
    }
  }

  function extractSummary(description) {
    if (!description) return 'Imported from Dynamics';
    const lines = description.split('\n');
    return lines[0].substring(0, 255);
  }

  function getFirstDayOfMonth() {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-01`;
  }

  function getTodayDate() {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }

})();
