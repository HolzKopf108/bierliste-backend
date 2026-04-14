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
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = current_schema()
          AND table_name = 'group_activities'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS actor_display_name_snapshot varchar(50)',
            current_schema(),
            'group_activities'
        );
        EXECUTE format(
            'ALTER TABLE %I.%I ADD COLUMN IF NOT EXISTS target_display_name_snapshot varchar(50)',
            current_schema(),
            'group_activities'
        );
        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN actor_user_id DROP NOT NULL',
            current_schema(),
            'group_activities'
        );

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'group_activities'
              AND column_name = 'actor_username_snapshot'
        ) THEN
            EXECUTE format(
                'UPDATE %I.%I SET actor_display_name_snapshot = actor_username_snapshot WHERE actor_display_name_snapshot IS NULL AND actor_username_snapshot IS NOT NULL',
                current_schema(),
                'group_activities'
            );
        END IF;

        EXECUTE format(
            'UPDATE %I.%I SET actor_display_name_snapshot = %L WHERE actor_display_name_snapshot IS NULL',
            current_schema(),
            'group_activities',
            'Unbekanntes Mitglied'
        );

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = current_schema()
              AND table_name = 'group_activities'
              AND column_name = 'target_username_snapshot'
        ) THEN
            EXECUTE format(
                'UPDATE %I.%I SET target_display_name_snapshot = target_username_snapshot WHERE target_display_name_snapshot IS NULL AND target_username_snapshot IS NOT NULL',
                current_schema(),
                'group_activities'
            );
        END IF;

        EXECUTE format(
            'UPDATE %I.%I SET target_display_name_snapshot = %L WHERE target_display_name_snapshot IS NULL AND target_user_id IS NOT NULL',
            current_schema(),
            'group_activities',
            'Unbekanntes Mitglied'
        );

        EXECUTE format(
            'ALTER TABLE %I.%I ALTER COLUMN actor_display_name_snapshot SET NOT NULL',
            current_schema(),
            'group_activities'
        );
    END IF;
END
$$
@@
ALTER TABLE IF EXISTS groups ALTER COLUMN created_by_user_id DROP NOT NULL
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
          AND t.relname = 'group_activities'
          AND c.contype = 'c'
          AND (
              pg_get_constraintdef(c.oid) ILIKE '%actor_user_id > 0%'
              or pg_get_constraintdef(c.oid) ILIKE '%target_username_snapshot%'
              or pg_get_constraintdef(c.oid) ILIKE '%target_display_name_snapshot%'
          )
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.%I DROP CONSTRAINT %I',
            current_schema(),
            'group_activities',
            current_constraint_name
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'group_activities'
          AND c.conname = 'ck_group_activities_actor_target_state'
    ) THEN
        EXECUTE format(
            'ALTER TABLE %I.%I ADD CONSTRAINT %I CHECK (group_id > 0 and meta_version >= 1 and (actor_user_id is null or actor_user_id > 0) and (target_user_id is null or target_user_id > 0) and ((target_user_id is null and target_display_name_snapshot is null) or target_display_name_snapshot is not null))',
            current_schema(),
            'group_activities',
            'ck_group_activities_actor_target_state'
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
