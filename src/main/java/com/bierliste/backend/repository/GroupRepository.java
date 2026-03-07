package com.bierliste.backend.repository;

import com.bierliste.backend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupRepository extends JpaRepository<Group, Long> {
}