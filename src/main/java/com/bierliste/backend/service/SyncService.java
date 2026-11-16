package com.bierliste.backend.service;

import com.bierliste.backend.dto.SyncClientEventDto;
import com.bierliste.backend.dto.SyncEventResultDto;
import com.bierliste.backend.dto.SyncEventsRequestDto;
import com.bierliste.backend.dto.SyncEventsResponseDto;
import com.bierliste.backend.dto.SyncFailureResolution;
import com.bierliste.backend.model.ProcessedClientEvent;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.ProcessedClientEventRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final SyncEventProcessor syncEventProcessor;
    private final GroupAuthorizationService groupAuthorizationService;
    private final ProcessedClientEventRepository processedClientEventRepository;

    public SyncService(
        SyncEventProcessor syncEventProcessor,
        GroupAuthorizationService groupAuthorizationService,
        ProcessedClientEventRepository processedClientEventRepository
    ) {
        this.syncEventProcessor = syncEventProcessor;
        this.groupAuthorizationService = groupAuthorizationService;
        this.processedClientEventRepository = processedClientEventRepository;
    }

    public SyncEventsResponseDto processEvents(SyncEventsRequestDto request, User user) {
        groupAuthorizationService.requireAuthenticatedUserId(user);
        if (request.getEvents() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "events darf nicht null sein");
        }

        List<SyncEventResultDto> results = new ArrayList<>();
        for (SyncClientEventDto event : request.getEvents()) {
            results.add(processSingleEvent(event, user));
        }
        return new SyncEventsResponseDto(results);
    }

    private SyncEventResultDto processSingleEvent(SyncClientEventDto event, User user) {
        try {
            return syncEventProcessor.processEvent(event, user);
        } catch (DataIntegrityViolationException ex) {
            return duplicateAfterConcurrentInsert(event, user, ex);
        } catch (ResponseStatusException ex) {
            return SyncEventResultDto.failed(
                clientEventIdOrNull(event),
                ex.getStatusCode(),
                ex.getReason(),
                resolutionFor(ex.getStatusCode())
            );
        } catch (RuntimeException ex) {
            log.error("Unhandled exception while processing sync event", ex);
            return SyncEventResultDto.failed(
                clientEventIdOrNull(event),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Sync-Event konnte nicht verarbeitet werden",
                SyncFailureResolution.RETRY
            );
        }
    }

    private SyncEventResultDto duplicateAfterConcurrentInsert(
        SyncClientEventDto event,
        User user,
        DataIntegrityViolationException ex
    ) {
        UUID clientEventId = clientEventIdOrNull(event);
        if (clientEventId == null) {
            return SyncEventResultDto.failed(
                null,
                HttpStatus.BAD_REQUEST,
                "clientEventId darf nicht null sein",
                SyncFailureResolution.DROP
            );
        }

        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        ProcessedClientEvent processedEvent = processedClientEventRepository.findById(clientEventId).orElse(null);
        if (processedEvent != null && processedEvent.getUserId().equals(userId)) {
            return SyncEventResultDto.duplicate(clientEventId, processedEvent.getResult());
        }

        log.warn("Could not resolve duplicate sync event after unique constraint violation", ex);
        return SyncEventResultDto.failed(
            clientEventId,
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Sync-Event konnte nicht dedupliziert werden",
            SyncFailureResolution.RETRY
        );
    }

    private UUID clientEventIdOrNull(SyncClientEventDto event) {
        return event == null ? null : event.getClientEventId();
    }

    private SyncFailureResolution resolutionFor(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError() ? SyncFailureResolution.RETRY : SyncFailureResolution.DROP;
    }
}
