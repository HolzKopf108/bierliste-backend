package com.bierliste.backend.repository;

import com.bierliste.backend.model.GroupActivity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupActivityRepository extends JpaRepository<GroupActivity, Long> {

    List<GroupActivity> findByGroupIdOrderByTimestampDescIdDesc(Long groupId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            update group_activities
            set actor_user_id = case when actor_user_id = :userId then null else actor_user_id end,
                actor_display_name_snapshot = case
                    when actor_user_id = :userId then :displayNameSnapshot
                    else actor_display_name_snapshot
                end,
                target_user_id = case when target_user_id = :userId then null else target_user_id end,
                target_display_name_snapshot = case
                    when target_user_id = :userId then :displayNameSnapshot
                    else target_display_name_snapshot
                end
            where group_id = :groupId
              and (actor_user_id = :userId or target_user_id = :userId)
            """,
        nativeQuery = true
    )
    int anonymizeUserInGroupHistory(
        @Param("groupId") Long groupId,
        @Param("userId") Long userId,
        @Param("displayNameSnapshot") String displayNameSnapshot
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            update group_activities
            set actor_user_id = case when actor_user_id = :userId then null else actor_user_id end,
                actor_display_name_snapshot = case
                    when actor_user_id = :userId then :displayNameSnapshot
                    else actor_display_name_snapshot
                end,
                target_user_id = case when target_user_id = :userId then null else target_user_id end,
                target_display_name_snapshot = case
                    when target_user_id = :userId then :displayNameSnapshot
                    else target_display_name_snapshot
                end
            where actor_user_id = :userId or target_user_id = :userId
            """,
        nativeQuery = true
    )
    int anonymizeUserInAllGroupHistories(
        @Param("userId") Long userId,
        @Param("displayNameSnapshot") String displayNameSnapshot
    );

    @Query("""
        select ga
        from GroupActivity ga
        where ga.groupId = :groupId
          and (
              ga.timestamp < :cursorTimestamp
              or (ga.timestamp = :cursorTimestamp and ga.id < :cursorId)
          )
        order by ga.timestamp desc, ga.id desc
        """)
    List<GroupActivity> findPageByGroupIdBeforeCursor(
        @Param("groupId") Long groupId,
        @Param("cursorTimestamp") Instant cursorTimestamp,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );

    List<GroupActivity> findAllByGroupIdOrderByTimestampDescIdDesc(Long groupId);
}
