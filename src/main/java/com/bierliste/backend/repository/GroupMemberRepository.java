package com.bierliste.backend.repository;

import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    Optional<GroupMember> findByGroup_IdAndUser_IdAndActiveTrue(Long groupId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select gm
        from GroupMember gm
        where gm.group.id = :groupId and gm.user.id = :userId
        """)
    Optional<GroupMember> findByGroup_IdAndUser_IdForUpdate(
        @Param("groupId") Long groupId,
        @Param("userId") Long userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select gm
        from GroupMember gm
        where gm.group.id = :groupId and gm.user.id = :userId and gm.active = true
        """)
    Optional<GroupMember> findByGroup_IdAndUser_IdAndActiveTrueForUpdate(
        @Param("groupId") Long groupId,
        @Param("userId") Long userId
    );

    List<GroupMember> findAllByGroup_Id(Long groupId);

    List<GroupMember> findAllByGroup_IdAndActiveTrue(Long groupId);

    List<GroupMember> findAllByUser_Id(Long userId);

    List<GroupMember> findAllByUser_IdAndActiveTrue(Long userId);

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    boolean existsByGroup_IdAndUser_IdAndActiveTrue(Long groupId, Long userId);

    boolean existsByGroup_IdAndUser_IdAndRole(Long groupId, Long userId, GroupRole role);

    boolean existsByGroup_IdAndUser_IdAndRoleAndActiveTrue(Long groupId, Long userId, GroupRole role);

    boolean existsByGroup_Id(Long groupId);

    boolean existsByGroup_IdAndActiveTrue(Long groupId);

    boolean existsByGroup_IdAndRole(Long groupId, GroupRole role);

    boolean existsByGroup_IdAndRoleAndActiveTrue(Long groupId, GroupRole role);

    long countByGroup_IdAndRole(Long groupId, GroupRole role);

    long countByGroup_IdAndRoleAndActiveTrue(Long groupId, GroupRole role);

    Optional<GroupMember> findFirstByGroup_IdOrderByJoinedAtAscIdAsc(Long groupId);

    Optional<GroupMember> findFirstByGroup_IdAndActiveTrueOrderByJoinedAtAscIdAsc(Long groupId);

    @Query("""
        select gm.strichCount
        from GroupMember gm
        where gm.group.id = :groupId and gm.user.id = :userId
        """)
    Optional<Integer> findStrichCountByGroup_IdAndUser_Id(
        @Param("groupId") Long groupId,
        @Param("userId") Long userId
    );

    @Query("""
        select gm.strichCount
        from GroupMember gm
        where gm.group.id = :groupId and gm.user.id = :userId and gm.active = true
        """)
    Optional<Integer> findActiveStrichCountByGroup_IdAndUser_Id(
        @Param("groupId") Long groupId,
        @Param("userId") Long userId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update GroupMember gm
        set gm.strichCount = gm.strichCount + :amount
        where gm.group.id = :groupId and gm.user.id = :userId and gm.active = true
        """)
    int incrementActiveStrichCount(
        @Param("groupId") Long groupId,
        @Param("userId") Long userId,
        @Param("amount") int amount
    );

    @Query("""
        select new com.bierliste.backend.dto.GroupMemberDto(u.id, u.username, gm.joinedAt, gm.role, gm.strichCount)
        from GroupMember gm
        join gm.user u
        where gm.group.id = :groupId and gm.active = true
        order by u.username, u.id
        """)
    List<GroupMemberDto> findActiveMemberDtosByGroupId(@Param("groupId") Long groupId);
}
