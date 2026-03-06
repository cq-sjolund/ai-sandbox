-- Add Dynamics integration fields to worklog_entries
ALTER TABLE worklog_entries ADD COLUMN dynamics_id VARCHAR(255);
ALTER TABLE worklog_entries ADD COLUMN last_synced_at TIMESTAMP;
ALTER TABLE worklog_entries ADD COLUMN sync_status VARCHAR(50);

CREATE INDEX idx_worklog_entries_dynamics_id ON worklog_entries(dynamics_id);

-- Create dynamics_config table to store user's Dynamics settings
CREATE TABLE dynamics_config (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    organization_url VARCHAR(500) NOT NULL,
    access_token TEXT NOT NULL,
    bookable_resource_id VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_dynamics_config_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_dynamics_config_user UNIQUE (user_id)
);

CREATE INDEX idx_dynamics_config_user ON dynamics_config(user_id);

COMMENT ON COLUMN worklog_entries.dynamics_id IS 'Microsoft Dynamics msdyn_timeentryid for synced entries';
COMMENT ON COLUMN worklog_entries.last_synced_at IS 'Last time this entry was synced with Dynamics';
COMMENT ON COLUMN worklog_entries.sync_status IS 'Sync status: PENDING, SYNCED, FAILED, NOT_SYNCED';
COMMENT ON TABLE dynamics_config IS 'Stores Microsoft Dynamics 365 configuration per user';
COMMENT ON COLUMN dynamics_config.bookable_resource_id IS 'User''s bookable resource ID in Dynamics';
