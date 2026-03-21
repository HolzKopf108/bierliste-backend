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

ALTER TABLE IF EXISTS groups ADD COLUMN IF NOT EXISTS allow_arbitrary_money_settlements BOOLEAN;
UPDATE groups
SET allow_arbitrary_money_settlements = FALSE
WHERE allow_arbitrary_money_settlements IS NULL;
ALTER TABLE IF EXISTS groups ALTER COLUMN allow_arbitrary_money_settlements SET DEFAULT FALSE;
ALTER TABLE IF EXISTS groups ALTER COLUMN allow_arbitrary_money_settlements SET NOT NULL;

ALTER TABLE IF EXISTS group_members ADD COLUMN IF NOT EXISTS role VARCHAR(30);
UPDATE group_members
SET role = 'MEMBER'
WHERE role IS NULL OR role NOT IN ('MEMBER', 'ADMIN');
ALTER TABLE IF EXISTS group_members ALTER COLUMN role SET DEFAULT 'MEMBER';
ALTER TABLE IF EXISTS group_members ALTER COLUMN role SET NOT NULL;
ALTER TABLE IF EXISTS group_members DROP CONSTRAINT IF EXISTS ck_group_members_role;
ALTER TABLE IF EXISTS group_members
    ADD CONSTRAINT ck_group_members_role CHECK (role IN ('MEMBER', 'ADMIN'));

CREATE TABLE IF NOT EXISTS settlements (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    actor_user_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    money_amount DECIMAL(10, 2),
    striche_amount INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_settlements_group ON settlements(group_id);
CREATE INDEX IF NOT EXISTS idx_settlements_target_user ON settlements(target_user_id);
CREATE INDEX IF NOT EXISTS idx_settlements_actor_user ON settlements(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_settlements_created_at ON settlements(created_at);

ALTER TABLE IF EXISTS settlements DROP CONSTRAINT IF EXISTS ck_settlements_type;
ALTER TABLE IF EXISTS settlements
    ADD CONSTRAINT ck_settlements_type CHECK (type IN ('MONEY', 'STRICHE'));

ALTER TABLE IF EXISTS settlements DROP CONSTRAINT IF EXISTS ck_settlements_money_amount;
ALTER TABLE IF EXISTS settlements
    ADD CONSTRAINT ck_settlements_money_amount CHECK (money_amount IS NULL OR money_amount > 0);

ALTER TABLE IF EXISTS settlements DROP CONSTRAINT IF EXISTS ck_settlements_striche_amount;
ALTER TABLE IF EXISTS settlements
    ADD CONSTRAINT ck_settlements_striche_amount CHECK (striche_amount IS NULL OR striche_amount >= 1);

ALTER TABLE IF EXISTS settlements DROP CONSTRAINT IF EXISTS ck_settlements_amount_combination;
ALTER TABLE IF EXISTS settlements
    ADD CONSTRAINT ck_settlements_amount_combination CHECK (
        (type = 'MONEY' AND money_amount IS NOT NULL AND striche_amount IS NULL)
        OR
        (type = 'STRICHE' AND striche_amount IS NOT NULL AND money_amount IS NULL)
    );

CREATE TABLE IF NOT EXISTS group_activities (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    activity_timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_user_id BIGINT NOT NULL,
    actor_username_snapshot VARCHAR(50) NOT NULL,
    target_user_id BIGINT,
    target_username_snapshot VARCHAR(50),
    type VARCHAR(40) NOT NULL,
    meta_version INTEGER NOT NULL DEFAULT 1,
    meta_json TEXT NOT NULL DEFAULT '{}'
);

CREATE INDEX IF NOT EXISTS idx_group_activities_group_timestamp_id
    ON group_activities(group_id, activity_timestamp, id);
CREATE INDEX IF NOT EXISTS idx_group_activities_actor_user ON group_activities(actor_user_id);
CREATE INDEX IF NOT EXISTS idx_group_activities_target_user ON group_activities(target_user_id);

ALTER TABLE IF EXISTS group_activities DROP CONSTRAINT IF EXISTS ck_group_activities_group_id;
ALTER TABLE IF EXISTS group_activities
    ADD CONSTRAINT ck_group_activities_group_id CHECK (group_id > 0);

ALTER TABLE IF EXISTS group_activities DROP CONSTRAINT IF EXISTS ck_group_activities_actor_user_id;
ALTER TABLE IF EXISTS group_activities
    ADD CONSTRAINT ck_group_activities_actor_user_id CHECK (actor_user_id > 0);

ALTER TABLE IF EXISTS group_activities DROP CONSTRAINT IF EXISTS ck_group_activities_target_user_id;
ALTER TABLE IF EXISTS group_activities
    ADD CONSTRAINT ck_group_activities_target_user_id CHECK (target_user_id IS NULL OR target_user_id > 0);

ALTER TABLE IF EXISTS group_activities DROP CONSTRAINT IF EXISTS ck_group_activities_meta_version;
ALTER TABLE IF EXISTS group_activities
    ADD CONSTRAINT ck_group_activities_meta_version CHECK (meta_version >= 1);

ALTER TABLE IF EXISTS group_activities DROP CONSTRAINT IF EXISTS ck_group_activities_type;
ALTER TABLE IF EXISTS group_activities
    ADD CONSTRAINT ck_group_activities_type CHECK (
        type IN (
            'STRICH_INCREMENTED',
            'STRICHE_DEDUCTED',
            'MONEY_DEDUCTED',
            'USER_JOINED_GROUP',
            'USER_LEFT_GROUP',
            'ROLE_GRANTED_WART',
            'ROLE_REVOKED_WART',
            'GROUP_SETTINGS_CHANGED',
            'USER_REMOVED_FROM_GROUP',
            'INVITE_CREATED',
            'INVITE_USED'
        )
    );

ALTER TABLE IF EXISTS group_activities DROP CONSTRAINT IF EXISTS ck_group_activities_target_snapshot;
ALTER TABLE IF EXISTS group_activities
    ADD CONSTRAINT ck_group_activities_target_snapshot CHECK (
        (target_user_id IS NULL AND target_username_snapshot IS NULL)
        OR
        (target_user_id IS NOT NULL AND target_username_snapshot IS NOT NULL)
    );
