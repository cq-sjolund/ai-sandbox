-- Create worklog_entries table
CREATE TABLE worklog_entries (
    id BIGSERIAL PRIMARY KEY,
    entry_date DATE NOT NULL,
    summary VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    hours DECIMAL(5,2) NOT NULL,
    project_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_project FOREIGN KEY (project_id)
        REFERENCES projects(id) ON DELETE RESTRICT,
    CONSTRAINT hours_positive CHECK (hours > 0)
);

-- Create indexes for common query patterns
CREATE INDEX idx_entries_date ON worklog_entries(entry_date);
CREATE INDEX idx_entries_project ON worklog_entries(project_id);
CREATE INDEX idx_entries_date_project ON worklog_entries(entry_date, project_id);

-- Add comments for documentation
COMMENT ON TABLE worklog_entries IS 'Stores daily worklog entries with time tracking and project association';
COMMENT ON COLUMN worklog_entries.hours IS 'Hours spent on the task (supports decimals like 0.5, 3.5, 8.0)';
COMMENT ON COLUMN worklog_entries.summary IS 'Brief summary of work done (max 255 characters)';
COMMENT ON COLUMN worklog_entries.description IS 'Detailed description of work performed';
