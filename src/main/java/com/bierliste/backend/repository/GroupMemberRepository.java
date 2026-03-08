package com.bierliste.backend.repository;

import com.bierliste.backend.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    Optional<GroupMember> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    List<GroupMember> findAllByGroup_Id(Long groupId);

    List<GroupMember> findAllByUser_Id(Long userId);

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);
}
