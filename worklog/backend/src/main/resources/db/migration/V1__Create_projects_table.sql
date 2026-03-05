-- Create projects table
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    color_code VARCHAR(7) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create index on name for faster lookups
CREATE INDEX idx_projects_name ON projects(name);

-- Add comment for documentation
COMMENT ON TABLE projects IS 'Stores client projects and internal activities with associated color codes for visual identification';
COMMENT ON COLUMN projects.color_code IS 'Hex color code for visual identification (e.g., #FF5733)';
