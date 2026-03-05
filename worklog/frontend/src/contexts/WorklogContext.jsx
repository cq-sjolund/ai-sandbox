import React, { createContext, useContext, useState, useEffect } from 'react'
import { entriesAPI } from '../api/client'

const WorklogContext = createContext()

export function useWorklog() {
  const context = useContext(WorklogContext)
  if (!context) {
    throw new Error('useWorklog must be used within a WorklogProvider')
  }
  return context
}

export function WorklogProvider({ children }) {
  const [entries, setEntries] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const fetchEntries = async () => {
    try {
      setLoading(true)
      setError(null)
      const response = await entriesAPI.getAll()
      setEntries(response.data)
    } catch (err) {
      console.error('Failed to fetch entries:', err)
      setError(err.response?.data?.message || 'Failed to fetch entries')
    } finally {
      setLoading(false)
    }
  }

  const fetchEntriesByDate = async (date) => {
    try {
      const response = await entriesAPI.getByDate(date)
      return response.data
    } catch (err) {
      console.error('Failed to fetch entries by date:', err)
      throw err
    }
  }

  const fetchEntriesByDateRange = async (start, end) => {
    try {
      const response = await entriesAPI.getByDateRange(start, end)
      return response.data
    } catch (err) {
      console.error('Failed to fetch entries by date range:', err)
      throw err
    }
  }

  const createEntry = async (entry) => {
    try {
      const response = await entriesAPI.create(entry)
      setEntries([...entries, response.data])
      return response.data
    } catch (err) {
      console.error('Failed to create entry:', err)
      throw err
    }
  }

  const updateEntry = async (id, entry) => {
    try {
      const response = await entriesAPI.update(id, entry)
      setEntries(entries.map(e => e.id === id ? response.data : e))
      return response.data
    } catch (err) {
      console.error('Failed to update entry:', err)
      throw err
    }
  }

  const deleteEntry = async (id) => {
    try {
      await entriesAPI.delete(id)
      setEntries(entries.filter(e => e.id !== id))
    } catch (err) {
      console.error('Failed to delete entry:', err)
      throw err
    }
  }

  const getEntriesByDate = (date) => {
    return entries.filter(e => e.entryDate === date)
  }

  useEffect(() => {
    fetchEntries()
  }, [])

  const value = {
    entries,
    loading,
    error,
    fetchEntries,
    fetchEntriesByDate,
    fetchEntriesByDateRange,
    createEntry,
    updateEntry,
    deleteEntry,
    getEntriesByDate,
  }

  return (
    <WorklogContext.Provider value={value}>
      {children}
    </WorklogContext.Provider>
  )
}
