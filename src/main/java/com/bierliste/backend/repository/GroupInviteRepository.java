package com.bierliste.backend.repository;

import com.bierliste.backend.model.GroupInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, Long> {

    Optional<GroupInvite> findByToken(String token);

    boolean existsByToken(String token);

    void deleteAllByGroup_Id(Long groupId);
}
