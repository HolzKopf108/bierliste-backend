ALTER TABLE IF EXISTS user_settings DROP COLUMN IF EXISTS auto_sync_enabled;

ALTER TABLE IF EXISTS groups ADD COLUMN IF NOT EXISTS price_per_strich DECIMAL(10, 2);
UPDATE groups
SET price_per_strich = 1.00
WHERE price_per_strich IS NULL;
ALTER TABLE IF EXISTS groups ALTER COLUMN price_per_strich SET DEFAULT 1.00;
ALTER TABLE IF EXISTS groups ALTER COLUMN price_per_strich SET NOT NULL;
ALTER TABLE IF EXISTS groups DROP CONSTRAINT IF EXISTS ck_groups_price_per_strich;
ALTER TABLE IF EXISTS groups
    ADD CONSTRAINT ck_groups_price_per_strich CHECK (price_per_strich >= 0);

ALTER TABLE IF EXISTS groups ADD COLUMN IF NOT EXISTS only_warts_can_book_for_others BOOLEAN;
UPDATE groups
SET only_warts_can_book_for_others = TRUE
WHERE only_warts_can_book_for_others IS NULL;
ALTER TABLE IF EXISTS groups ALTER COLUMN only_warts_can_book_for_others SET DEFAULT TRUE;
ALTER TABLE IF EXISTS groups ALTER COLUMN only_warts_can_book_for_others SET NOT NULL;

ALTER TABLE IF EXISTS group_members ADD COLUMN IF NOT EXISTS role VARCHAR(30);
UPDATE group_members
SET role = 'MEMBER'
WHERE role IS NULL OR role NOT IN ('MEMBER', 'ADMIN');
ALTER TABLE IF EXISTS group_members ALTER COLUMN role SET DEFAULT 'MEMBER';
ALTER TABLE IF EXISTS group_members ALTER COLUMN role SET NOT NULL;
ALTER TABLE IF EXISTS group_members DROP CONSTRAINT IF EXISTS ck_group_members_role;
ALTER TABLE IF EXISTS group_members
    ADD CONSTRAINT ck_group_members_role CHECK (role IN ('MEMBER', 'ADMIN'));
