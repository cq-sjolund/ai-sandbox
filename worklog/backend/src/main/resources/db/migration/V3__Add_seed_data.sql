-- Insert seed projects
INSERT INTO projects (name, color_code, description) VALUES
    ('Enablement', '#1473E6', 'Internal training, learning, and professional development activities'),
    ('Client A', '#E34850', 'Consulting work for Client A - software development and technical advisory'),
    ('Client B', '#44B556', 'Consulting work for Client B - system architecture and implementation');

-- Add a sample worklog entry for demonstration
INSERT INTO worklog_entries (entry_date, summary, description, hours, project_id)
SELECT
    CURRENT_DATE - INTERVAL '1 day',
    'Initial project setup',
    'Set up worklog application with Docker, PostgreSQL, and Spring Boot. Configured database migrations and created initial project structure.',
    4.5,
    (SELECT id FROM projects WHERE name = 'Enablement')
WHERE EXISTS (SELECT 1 FROM projects WHERE name = 'Enablement');
