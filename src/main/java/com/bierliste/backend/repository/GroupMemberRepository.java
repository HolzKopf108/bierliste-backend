package com.bierliste.backend.repository;

import com.bierliste.backend.dto.GroupMemberDto;
import com.bierliste.backend.model.GroupMember;
import com.bierliste.backend.model.GroupRole;
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

    boolean existsByGroup_Id(Long groupId);

    boolean existsByGroup_IdAndRole(Long groupId, GroupRole role);

    Optional<GroupMember> findFirstByGroup_IdOrderByJoinedAtAscIdAsc(Long groupId);

    @Query("""
        select new com.bierliste.backend.dto.GroupMemberDto(gm.user.id, gm.user.username, gm.joinedAt, gm.role, gm.strichCount)
        from GroupMember gm
        where gm.group.id = :groupId
        order by gm.user.username
        """)
    List<GroupMemberDto> findMemberDtosByGroupId(@Param("groupId") Long groupId);
}
