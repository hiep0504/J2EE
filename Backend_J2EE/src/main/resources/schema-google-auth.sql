-- Google login migration for account table
-- Run manually on MySQL if your table existed before adding fields in entity.

ALTER TABLE account
    ADD COLUMN IF NOT EXISTS login_type ENUM('local', 'google') NOT NULL DEFAULT 'local',
    ADD COLUMN IF NOT EXISTS google_id VARCHAR(100) NULL,
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500) NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_account_google_id ON account (google_id);

UPDATE account
SET login_type = 'local'
WHERE login_type IS NULL;
