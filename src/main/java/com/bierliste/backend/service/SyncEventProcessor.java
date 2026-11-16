package com.bierliste.backend.service;

import com.bierliste.backend.dto.CounterIncrementResponseDto;
import com.bierliste.backend.dto.SyncClientEventDto;
import com.bierliste.backend.dto.SyncEventResultDto;
import com.bierliste.backend.model.ClientEventType;
import com.bierliste.backend.model.ProcessedClientEvent;
import com.bierliste.backend.model.User;
import com.bierliste.backend.repository.ProcessedClientEventRepository;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SyncEventProcessor {

    private final ProcessedClientEventRepository processedClientEventRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final GroupService groupService;

    public SyncEventProcessor(
        ProcessedClientEventRepository processedClientEventRepository,
        GroupAuthorizationService groupAuthorizationService,
        GroupService groupService
    ) {
        this.processedClientEventRepository = processedClientEventRepository;
        this.groupAuthorizationService = groupAuthorizationService;
        this.groupService = groupService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SyncEventResultDto processEvent(SyncClientEventDto event, User user) {
        Long userId = groupAuthorizationService.requireAuthenticatedUserId(user);
        requireEvent(event);

        UUID clientEventId = requireClientEventId(event);
        ProcessedClientEvent existingEvent = processedClientEventRepository.findById(clientEventId).orElse(null);
        if (existingEvent != null) {
            if (!existingEvent.getUserId().equals(userId)) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "clientEventId wurde bereits fuer einen anderen User verarbeitet"
                );
            }
            return SyncEventResultDto.duplicate(clientEventId, existingEvent.getResult());
        }

        ClientEventType type = requireType(event);
        Long groupId = requireGroupId(event);
        requireClientTimestamp(event);

        ProcessedClientEvent processedEvent = new ProcessedClientEvent();
        processedEvent.setClientEventId(clientEventId);
        processedEvent.setUserId(userId);
        processedEvent.setType(type);
        processedEvent.setGroupId(groupId);
        processedEvent.setProcessedAt(Instant.now());
        processedClientEventRepository.saveAndFlush(processedEvent);

        SyncEventResultDto result = switch (type) {
            case INCREMENT_SELF -> applyIncrementSelf(clientEventId, groupId, event, user);
        };
        processedEvent.setResult(result.getServerStateDelta());
        processedClientEventRepository.saveAndFlush(processedEvent);
        return result;
    }

    private SyncEventResultDto applyIncrementSelf(UUID clientEventId, Long groupId, SyncClientEventDto event, User user) {
        int amount = requireAmount(event.getPayload());
        CounterIncrementResponseDto incrementResult = groupService.incrementOwnCounterForSync(groupId, amount, user);

        Map<String, Object> serverStateDelta = new LinkedHashMap<>();
        serverStateDelta.put("groupId", groupId);
        serverStateDelta.put("count", incrementResult.getCount());
        serverStateDelta.put("incrementRequestId", incrementResult.getIncrementRequestId());
        serverStateDelta.put("undoExpiresAt", incrementResult.getUndoExpiresAt());
        return SyncEventResultDto.applied(clientEventId, serverStateDelta);
    }

    private void requireEvent(SyncClientEventDto event) {
        if (event == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event darf nicht null sein");
        }
    }

    private UUID requireClientEventId(SyncClientEventDto event) {
        if (event.getClientEventId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientEventId darf nicht null sein");
        }
        return event.getClientEventId();
    }

    private ClientEventType requireType(SyncClientEventDto event) {
        if (event.getType() == null || event.getType().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type darf nicht leer sein");
        }
        try {
            return ClientEventType.valueOf(event.getType().trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event-Typ wird nicht unterstuetzt");
        }
    }

    private Long requireGroupId(SyncClientEventDto event) {
        Long groupId = event.getGroupId();
        if (groupId == null || groupId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "groupId muss positiv sein");
        }
        return groupId;
    }

    private void requireClientTimestamp(SyncClientEventDto event) {
        if (event.getClientTimestamp() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientTimestamp darf nicht null sein");
        }
    }

    private int requireAmount(Map<String, Object> payload) {
        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payload darf nicht null sein");
        }

        Object value = payload.get("amount");
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount darf nicht null sein");
        }

        Integer amount = toInteger(value);
        if (amount == null || amount < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount muss mindestens 1 sein");
        }
        return amount;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Integer amount) {
            return amount;
        }
        if (value instanceof Long amount) {
            return amount >= Integer.MIN_VALUE && amount <= Integer.MAX_VALUE ? amount.intValue() : null;
        }
        if (value instanceof Short amount) {
            return amount.intValue();
        }
        if (value instanceof Byte amount) {
            return amount.intValue();
        }
        if (value instanceof BigInteger amount) {
            return amount.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0
                && amount.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0
                ? amount.intValue()
                : null;
        }
        if (value instanceof BigDecimal amount) {
            try {
                return amount.intValueExact();
            } catch (ArithmeticException ex) {
                return null;
            }
        }
        return null;
    }
}
