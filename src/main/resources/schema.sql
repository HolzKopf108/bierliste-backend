ALTER TABLE IF EXISTS user_settings DROP COLUMN IF EXISTS auto_sync_enabled;
UPDATE groups
SET invite_permission = 'ONLY_WARTS'
WHERE invite_permission IS NULL;
