package com.bierliste.backend.repository;

import com.bierliste.backend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {

    List<Group> findDistinctByMembers_User_IdOrderByNameAsc(Long userId);
}
