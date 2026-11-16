package com.bierliste.backend.repository;

import com.bierliste.backend.model.ProcessedClientEvent;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedClientEventRepository extends JpaRepository<ProcessedClientEvent, UUID> {

    long countByUserId(Long userId);

    long deleteByProcessedAtBefore(Instant processedAt);
}
