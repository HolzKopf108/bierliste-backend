package com.bierliste.backend.repository;

import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    List<GroupMember> findAllByGroup_Id(Long groupId);

    List<GroupMember> findAllByUser_Id(Long userId);

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    boolean existsByGroup_IdAndUser_IdAndRole(Long groupId, Long userId, GroupRole role);

    boolean existsByGroup_Id(Long groupId);

    boolean existsByGroup_IdAndRole(Long groupId, GroupRole role);

    Optional<GroupMember> findFirstByGroup_IdOrderByJoinedAtAscIdAsc(Long groupId);

    @Query("""
        select gm.strichCount
        from GroupMember gm
        where gm.group.id = :groupId and gm.user.id = :userId
        """)
    Optional<Integer> findStrichCountByGroup_IdAndUser_Id(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update GroupMember gm
        set gm.strichCount = gm.strichCount + :amount
        where gm.group.id = :groupId and gm.user.id = :userId
        """)
    int incrementStrichCount(@Param("groupId") Long groupId, @Param("userId") Long userId, @Param("amount") int amount);

    @Query("""
        select new com.bierliste.backend.dto.GroupMemberDto(u.id, u.username, gm.joinedAt, gm.role, gm.strichCount)
        from GroupMember gm
        join gm.user u
        where gm.group.id = :groupId
        order by u.username, u.id
        """)
    List<GroupMemberDto> findMemberDtosByGroupId(@Param("groupId") Long groupId);
}
