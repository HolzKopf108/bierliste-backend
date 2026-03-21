package com.bierliste.backend.repository;

import com.bierliste.backend.model.GroupActivity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupActivityRepository extends JpaRepository<GroupActivity, Long> {

    @Query("""
        select ga
        from GroupActivity ga
        where ga.groupId = :groupId
          and (
              :cursorTimestamp is null
              or ga.timestamp < :cursorTimestamp
              or (ga.timestamp = :cursorTimestamp and ga.id < :cursorId)
          )
        order by ga.timestamp desc, ga.id desc
        """)
    List<GroupActivity> findPageByGroupId(
        @Param("groupId") Long groupId,
        @Param("cursorTimestamp") Instant cursorTimestamp,
        @Param("cursorId") Long cursorId,
        Pageable pageable
    );

    List<GroupActivity> findAllByGroupIdOrderByTimestampDescIdDesc(Long groupId);
}
