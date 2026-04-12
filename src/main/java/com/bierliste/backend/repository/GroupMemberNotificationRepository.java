package com.bierliste.backend.repository;

import com.bierliste.backend.model.GroupMemberNotification;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberNotificationRepository extends JpaRepository<GroupMemberNotification, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select notification
        from GroupMemberNotification notification
        where notification.id = :notificationId and notification.groupId = :groupId
        """)
    Optional<GroupMemberNotification> findByIdAndGroupIdForUpdate(
        @Param("notificationId") Long notificationId,
        @Param("groupId") Long groupId
    );

    List<GroupMemberNotification> findAllByGroupIdAndTargetUserIdAndConfirmedAtIsNullOrderByCreatedAtDescIdDesc(
        Long groupId,
        Long targetUserId
    );

    @Query("""
        select notification
        from GroupMemberNotification notification
        where notification.groupId = :groupId and notification.targetUserId in :targetUserIds
        order by notification.targetUserId asc, notification.createdAt desc, notification.id desc
        """)
    List<GroupMemberNotification> findAllLatestCandidatesByGroupIdAndTargetUserIdIn(
        @Param("groupId") Long groupId,
        @Param("targetUserIds") Collection<Long> targetUserIds
    );

    @Query(
        value = """
            select member_user.user_id as targetUserId,
                   case when count(distinct token.id) > 0 then true else false end as canReceiveNotification,
                   latest.created_at as lastNotificationSentAt,
                   latest.confirmed_at as lastNotificationConfirmedAt
            from group_members member_user
            left join android_push_tokens token on token.user_id = member_user.user_id
            left join group_member_notifications latest
                on latest.id = (
                    select max(inner_notification.id)
                    from group_member_notifications inner_notification
                    where inner_notification.group_id = :groupId
                      and inner_notification.target_user_id = member_user.user_id
                )
            where member_user.group_id = :groupId
              and member_user.user_id in (:targetUserIds)
            group by member_user.user_id, latest.created_at, latest.confirmed_at
            """,
        nativeQuery = true
    )
    List<Object[]> findRawStatusesByGroupIdAndTargetUserIds(
        @Param("groupId") Long groupId,
        @Param("targetUserIds") Collection<Long> targetUserIds
    );

}
