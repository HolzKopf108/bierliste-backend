package com.bierliste.backend.repository;

import com.bierliste.backend.model.CounterIncrementRequest;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CounterIncrementRequestRepository extends JpaRepository<CounterIncrementRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select cir
        from CounterIncrementRequest cir
        where cir.id = :incrementRequestId and cir.groupId = :groupId
        """)
    Optional<CounterIncrementRequest> findByIdAndGroupIdForUpdate(
        @Param("incrementRequestId") Long incrementRequestId,
        @Param("groupId") Long groupId
    );
}
