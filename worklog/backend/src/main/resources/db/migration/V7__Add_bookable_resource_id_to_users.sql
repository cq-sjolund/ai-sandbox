-- Add bookable_resource_id to users table for Dynamics integration
ALTER TABLE users ADD COLUMN bookable_resource_id VARCHAR(255);

CREATE INDEX idx_users_bookable_resource ON users(bookable_resource_id);

COMMENT ON COLUMN users.bookable_resource_id IS 'Microsoft Dynamics 365 Bookable Resource ID for filtering time entries';
