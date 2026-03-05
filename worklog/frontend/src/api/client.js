import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
})

// Request interceptor
apiClient.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`)
    return config
  },
  (error) => {
    console.error('API Request Error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor
apiClient.interceptors.response.use(
  (response) => {
    console.log(`API Response: ${response.config.url} - ${response.status}`)
    return response
  },
  (error) => {
    console.error('API Response Error:', error.response?.data || error.message)
    return Promise.reject(error)
  }
)

// Projects API
export const projectsAPI = {
  getAll: () => apiClient.get('/projects'),
  getById: (id) => apiClient.get(`/projects/${id}`),
  create: (project) => apiClient.post('/projects', project),
  update: (id, project) => apiClient.put(`/projects/${id}`, project),
  delete: (id, deleteEntries = false) => apiClient.delete(`/projects/${id}?deleteEntries=${deleteEntries}`),
  countEntries: (id) => apiClient.get(`/projects/${id}/entries/count`),
}

// Worklog Entries API
export const entriesAPI = {
  getAll: () => apiClient.get('/entries'),
  getById: (id) => apiClient.get(`/entries/${id}`),
  getByDate: (date) => apiClient.get(`/entries/date/${date}`),
  getByDateRange: (start, end) => apiClient.get(`/entries/range?start=${start}&end=${end}`),
  create: (entry) => apiClient.post('/entries', entry),
  update: (id, entry) => apiClient.put(`/entries/${id}`, entry),
  delete: (id) => apiClient.delete(`/entries/${id}`),
}

// AI Summary API
export const aiAPI = {
  generateSummary: (request) => apiClient.post('/ai/summary', request),
}

export default apiClient
