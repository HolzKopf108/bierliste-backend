package com.bierliste.backend.repository;

import com.bierliste.backend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findDistinctByMembers_User_IdOrderByNameAsc(Long userId);

    boolean existsByIdAndMembers_User_Id(Long groupId, Long userId);

    Optional<Group> findByIdAndMembers_User_Id(Long groupId, Long userId);
}
