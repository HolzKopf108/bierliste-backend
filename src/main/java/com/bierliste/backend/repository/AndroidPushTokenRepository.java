package com.bierliste.backend.repository;

import com.bierliste.backend.model.AndroidPushToken;
import com.bierliste.backend.model.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AndroidPushTokenRepository extends JpaRepository<AndroidPushToken, Long> {

    Optional<AndroidPushToken> findByToken(String token);

    List<AndroidPushToken> findAllByUser_Id(Long userId);

    void deleteByUser(User user);

    long deleteByUser_IdAndToken(Long userId, String token);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from AndroidPushToken token where token.token in :tokens")
    void deleteAllByTokenIn(@Param("tokens") Collection<String> tokens);

    @Query("""
        select distinct token.user.id
        from AndroidPushToken token
        where token.user.id in :userIds
        """)
    List<Long> findDistinctUserIdsWithTokens(@Param("userIds") Collection<Long> userIds);
}
