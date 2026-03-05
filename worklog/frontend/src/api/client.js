import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 30000,
})

// Request interceptor - Add JWT token
apiClient.interceptors.request.use(
  (config) => {
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`)

    // Add JWT token to Authorization header if available
    const token = localStorage.getItem('jwt_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    return config
  },
  (error) => {
    console.error('API Request Error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor - Handle 401 unauthorized
apiClient.interceptors.response.use(
  (response) => {
    console.log(`API Response: ${response.config.url} - ${response.status}`)
    return response
  },
  (error) => {
    console.error('API Response Error:', error.response?.data || error.message)

    // Handle 401 Unauthorized - token expired or invalid
    if (error.response?.status === 401) {
      localStorage.removeItem('jwt_token')
      // Let AuthContext and ProtectedRoute handle redirect via React Router
    }

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
  suggestColor: (projectName) => apiClient.post('/ai/suggest-color', { projectName }),
  completeDescription: (currentText, summary, projectName) =>
    apiClient.post('/ai/complete-description', { currentText, summary, projectName }),
  askQuestion: (question) => apiClient.post('/ai/ask', { question }),
}

// Auth API
export const authAPI = {
  login: (username, password) => apiClient.post('/auth/login', { username, password }),
  getCurrentUser: () => apiClient.get('/auth/me'),
}

// Users API (Admin only)
export const usersAPI = {
  getAll: () => apiClient.get('/users'),
  getById: (id) => apiClient.get(`/users/${id}`),
  create: (userData) => apiClient.post('/users', userData),
  delete: (id) => apiClient.delete(`/users/${id}`),
}

export default apiClient
