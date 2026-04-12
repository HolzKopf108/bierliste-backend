package com.bierliste.backend.service;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMemberNotification;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class FcmAndroidPushSender implements AndroidPushSender {

    private static final Logger log = LoggerFactory.getLogger(FcmAndroidPushSender.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
    private static final String FCM_ENDPOINT_TEMPLATE = "https://fcm.googleapis.com/v1/projects/{projectId}/messages:send";
    private static final String FCM_ERROR_TYPE = "type.googleapis.com/google.firebase.fcm.v1.FcmError";
    private static final String BAD_REQUEST_TYPE = "type.googleapis.com/google.rpc.BadRequest";
    private static final String ANDROID_HIGH_PRIORITY = "high";

    private final RestClient restClient;
    private final GoogleCredentials credentials;
    private final String projectId;

    public FcmAndroidPushSender(
        RestClient.Builder restClientBuilder,
        @Value("${app.notifications.fcm.service-account-json-base64:}") String serviceAccountJsonBase64
    ) {
        this.restClient = restClientBuilder.build();

        if (serviceAccountJsonBase64 == null || serviceAccountJsonBase64.isBlank()) {
            this.credentials = null;
            this.projectId = null;
            return;
        }

        ServiceAccountCredentials serviceAccountCredentials = loadCredentials(serviceAccountJsonBase64);
        this.credentials = serviceAccountCredentials.createScoped(List.of(FCM_SCOPE));
        this.projectId = serviceAccountCredentials.getProjectId();

        if (this.projectId == null || this.projectId.isBlank()) {
            throw new IllegalStateException("FCM-Service-Account enthaelt keine project_id");
        }

        log.info("FCM Android push sender configured for projectId={}", this.projectId);
    }

    @Override
    public PushDispatchResult sendGroupMemberNotification(Group group, GroupMemberNotification notification, List<String> deviceTokens) {
        if (deviceTokens.isEmpty()) {
            return new PushDispatchResult(0, 0, List.of(), isConfigured());
        }

        if (!isConfigured()) {
            log.warn(
                "Skipping Android push dispatch for groupId={} notificationId={} because FCM is not configured",
                notification.getGroupId(),
                notification.getId()
            );
            return new PushDispatchResult(deviceTokens.size(), 0, List.of(), false);
        }

        String accessToken = getAccessToken();
        int successCount = 0;
        List<String> invalidTokens = new ArrayList<>();

        for (String deviceToken : deviceTokens) {
            try {
                sendToToken(accessToken, deviceToken, group, notification);
                successCount++;
            } catch (RestClientResponseException ex) {
                if (isInvalidTokenResponse(ex.getResponseBodyAsString())) {
                    invalidTokens.add(deviceToken);
                }
                log.warn(
                    "FCM dispatch failed for notificationId={} targetUserId={} projectId={} status={} response={}",
                    notification.getId(),
                    notification.getTargetUserId(),
                    projectId,
                    ex.getStatusCode(),
                    ex.getResponseBodyAsString()
                );
            } catch (RuntimeException ex) {
                log.warn(
                    "FCM dispatch failed for notificationId={} targetUserId={} projectId={}",
                    notification.getId(),
                    notification.getTargetUserId(),
                    projectId,
                    ex
                );
            }
        }

        if (successCount == 0) {
            log.warn(
                "FCM dispatch completed without successful deliveries for notificationId={} targetUserId={} projectId={} attemptedCount={} invalidTokenCount={}",
                notification.getId(),
                notification.getTargetUserId(),
                projectId,
                deviceTokens.size(),
                invalidTokens.size()
            );
        } else if (invalidTokens.isEmpty()) {
            log.info(
                "FCM dispatch succeeded for notificationId={} targetUserId={} projectId={} attemptedCount={} successCount={}",
                notification.getId(),
                notification.getTargetUserId(),
                projectId,
                deviceTokens.size(),
                successCount
            );
        } else {
            log.warn(
                "FCM dispatch partially succeeded for notificationId={} targetUserId={} projectId={} attemptedCount={} successCount={} invalidTokenCount={}",
                notification.getId(),
                notification.getTargetUserId(),
                projectId,
                deviceTokens.size(),
                successCount,
                invalidTokens.size()
            );
        }

        return new PushDispatchResult(deviceTokens.size(), successCount, List.copyOf(invalidTokens), true);
    }

    private boolean isConfigured() {
        return credentials != null && projectId != null;
    }

    private ServiceAccountCredentials loadCredentials(String serviceAccountJsonBase64) {
        try {
            byte[] jsonBytes = Base64.getDecoder().decode(serviceAccountJsonBase64.trim());
            return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(jsonBytes));
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("FCM-Service-Account konnte nicht geladen werden", ex);
        }
    }

    private synchronized String getAccessToken() {
        if (credentials == null) {
            throw new IllegalStateException("FCM ist nicht konfiguriert");
        }

        try {
            credentials.refreshIfExpired();
            AccessToken accessToken = credentials.getAccessToken();
            if (accessToken == null || accessToken.getExpirationTime() == null) {
                credentials.refresh();
                accessToken = credentials.getAccessToken();
            }
            if (accessToken == null || accessToken.getExpirationTime() == null) {
                throw new IllegalStateException("FCM-Access-Token konnte nicht geladen werden");
            }
            if (accessToken.getExpirationTime().toInstant().minusSeconds(30).isBefore(Instant.now())) {
                credentials.refresh();
                accessToken = credentials.getAccessToken();
            }
            if (accessToken == null) {
                throw new IllegalStateException("FCM-Access-Token fehlt");
            }
            return accessToken.getTokenValue();
        } catch (IOException ex) {
            throw new IllegalStateException("FCM-Access-Token konnte nicht aktualisiert werden", ex);
        }
    }

    private void sendToToken(
        String accessToken,
        String deviceToken,
        Group group,
        GroupMemberNotification notification
    ) {
        Map<String, Object> payload = buildPayload(deviceToken, group, notification);

        restClient.post()
            .uri(FCM_ENDPOINT_TEMPLATE, projectId)
            .contentType(MediaType.APPLICATION_JSON)
            .headers(headers -> headers.setBearerAuth(accessToken))
            .body(payload)
            .retrieve()
            .toBodilessEntity();
    }

    Map<String, Object> buildPayload(
        String deviceToken,
        Group group,
        GroupMemberNotification notification
    ) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", "GROUP_MEMBER_NOTIFICATION");
        data.put("groupId", notification.getGroupId().toString());
        data.put("notificationId", notification.getId().toString());
        data.put("actorUserId", notification.getActorUserId().toString());
        data.put("targetUserId", notification.getTargetUserId().toString());
        data.put("groupName", group.getName());
        data.put("title", "Bierliste");
        data.put("message", notification.getMessage());

        Map<String, Object> androidConfig = new LinkedHashMap<>();
        androidConfig.put("priority", ANDROID_HIGH_PRIORITY);

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("token", deviceToken);
        message.put("android", androidConfig);
        message.put("data", data);

        return Map.of("message", message);
    }

    boolean isInvalidTokenResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        try {
            JsonNode errorNode = OBJECT_MAPPER.readTree(responseBody).path("error");
            String status = errorNode.path("status").asText("");
            String message = errorNode.path("message").asText("");
            JsonNode details = errorNode.path("details");

            if ("UNREGISTERED".equals(status)) {
                return true;
            }

            if (!details.isArray()) {
                return false;
            }

            for (JsonNode detail : details) {
                String type = detail.path("@type").asText("");
                String errorCode = detail.path("errorCode").asText("");

                if (BAD_REQUEST_TYPE.equals(type)) {
                    return false;
                }

                if (FCM_ERROR_TYPE.equals(type) && "UNREGISTERED".equals(errorCode)) {
                    return true;
                }

                if (FCM_ERROR_TYPE.equals(type)
                    && "INVALID_ARGUMENT".equals(errorCode)
                    && message.toLowerCase(Locale.ROOT).contains("registration token")) {
                    return true;
                }
            }
        } catch (IOException ex) {
            log.debug("Could not parse FCM error response body for invalid token detection", ex);
        }

        return false;
    }
}
