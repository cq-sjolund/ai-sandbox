import React, { createContext, useContext, useState, useEffect } from 'react'
import { projectsAPI } from '../api/client'

const ProjectContext = createContext()

export function useProjects() {
  const context = useContext(ProjectContext)
  if (!context) {
    throw new Error('useProjects must be used within a ProjectProvider')
  }
  return context
}

export function ProjectProvider({ children }) {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchProjects = async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await projectsAPI.getAll()
      setProjects(response.data)
    } catch (err) {
      console.error('Failed to fetch projects:', err)
      setError(err.response?.data?.message || 'Failed to fetch projects')
    } finally {
      setLoading(false)
    }
  }

  const createProject = async (project) => {
    try {
      const response = await projectsAPI.create(project)
      setProjects([...projects, response.data])
      return response.data
    } catch (err) {
      console.error('Failed to create project:', err)
      throw err
    }
  }

  const updateProject = async (id, project) => {
    try {
      const response = await projectsAPI.update(id, project)
      setProjects(projects.map(p => p.id === id ? response.data : p))
      return response.data
    } catch (err) {
      console.error('Failed to update project:', err)
      throw err
    }
  }

  const deleteProject = async (id, deleteEntries = false) => {
    try {
      await projectsAPI.delete(id, deleteEntries)
      setProjects(projects.filter(p => p.id !== id))
    } catch (err) {
      console.error('Failed to delete project:', err)
      throw err
    }
  }

  const countProjectEntries = async (id) => {
    try {
      const response = await projectsAPI.countEntries(id)
      return response.data
    } catch (err) {
      console.error('Failed to count project entries:', err)
      throw err
    }
  }

  const getProjectById = (id) => {
    return projects.find(p => p.id === id)
  }

  useEffect(() => {
    fetchProjects()
  }, [])

  const value = {
    projects,
    loading,
    error,
    fetchProjects,
    createProject,
    updateProject,
    deleteProject,
    countProjectEntries,
    getProjectById,
  }

  return (
    <ProjectContext.Provider value={value}>
      {children}
    </ProjectContext.Provider>
  )
}
