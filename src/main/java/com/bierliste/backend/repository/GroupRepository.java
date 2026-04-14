package com.bierliste.backend.repository;

import com.bierliste.backend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "update groups set created_by_user_id = null where created_by_user_id = :userId", nativeQuery = true)
    int clearCreatedByUserId(@Param("userId") Long userId);

    @Query("""
        select distinct g
        from Group g
        join g.members gm
        where gm.user.id = :userId and gm.active = true
        order by g.name
        """)
    List<Group> findDistinctByMembers_User_IdOrderByNameAsc(Long userId);

    @Query("""
        select count(g) > 0
        from Group g
        join g.members gm
        where g.id = :groupId and gm.user.id = :userId and gm.active = true
        """)
    boolean existsByIdAndMembers_User_Id(@Param("groupId") Long groupId, @Param("userId") Long userId);

    @Query("""
        select g
        from Group g
        join g.members gm
        where g.id = :groupId and gm.user.id = :userId and gm.active = true
        """)
    Optional<Group> findByIdAndMembers_User_Id(@Param("groupId") Long groupId, @Param("userId") Long userId);
}
