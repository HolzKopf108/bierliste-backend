ALTER TABLE IF EXISTS user_settings DROP COLUMN IF EXISTS auto_sync_enabled
@@
DO $$
DECLARE
    current_constraint_name text;
BEGIN
    FOR current_constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'group_members'
          AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) ILIKE '%strich_count >= 0%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            current_schema(),
            'group_members',
            current_constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'group_members'
          AND c.conname = 'ck_group_members_role'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %I.%I ADD CONSTRAINT %I CHECK (role in (''MEMBER'', ''ADMIN''))',
            current_schema(),
            'group_members',
            'ck_group_members_role'
        );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'group_members'
          AND c.conname = 'ck_group_members_active_state'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %I.%I ADD CONSTRAINT %I CHECK (((active = true and left_at is null) or (active = false and left_at is not null)))',
            current_schema(),
            'group_members',
            'ck_group_members_active_state'
        );
    END IF;
END
$$
@@
DO $$
DECLARE
    current_constraint_name text;
BEGIN
    FOR current_constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'counter_increment_requests'
          AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) ILIKE '%count_after_undo >= 0%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            current_schema(),
            'counter_increment_requests',
            current_constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'counter_increment_requests'
          AND c.conname = 'ck_counter_increment_requests_state'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %I.%I ADD CONSTRAINT %I CHECK (group_id > 0 and actor_user_id > 0 and target_user_id > 0 and amount >= 1 and increment_activity_id > 0 and undo_expires_at >= created_at and (undo_activity_id is null or undo_activity_id > 0) and ((undone_at is null and undo_activity_id is null and count_after_undo is null) or (undone_at is not null and undo_activity_id is not null and count_after_undo is not null)))',
            current_schema(),
            'counter_increment_requests',
            'ck_counter_increment_requests_state'
        );
    END IF;
END
$$
@@
