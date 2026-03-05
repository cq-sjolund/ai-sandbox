-- Add user_id column to projects
ALTER TABLE projects ADD COLUMN user_id BIGINT;

-- Assign all existing projects to admin user (id=1)
UPDATE projects SET user_id = 1;

-- Make user_id NOT NULL and add foreign key
ALTER TABLE projects ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE projects ADD CONSTRAINT fk_project_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- Add index for efficient user-based queries
CREATE INDEX idx_projects_user ON projects(user_id);
