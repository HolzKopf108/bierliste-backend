package com.bierliste.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bierliste.backend.model.Group;
import com.bierliste.backend.model.GroupMemberNotification;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class FcmAndroidPushSenderTest {

    @Test
    void buildPayloadUsesExpectedAndroidDataMessageShape() {
        FcmAndroidPushSender sender = new FcmAndroidPushSender(RestClient.builder(), "");
        Group group = new Group();
        group.setName("Stammtisch");

        GroupMemberNotification notification = new GroupMemberNotification();
        ReflectionTestUtils.setField(notification, "id", 44L);
        notification.setGroupId(11L);
        notification.setActorUserId(22L);
        notification.setActorUsernameSnapshot("Wart");
        notification.setTargetUserId(33L);
        notification.setMessage("Bring heute 5,00€ mit");

        Map<String, Object> payload = sender.buildPayload("device-token-123", group, notification);

        assertThat(payload).containsOnlyKeys("message");

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) payload.get("message");
        assertThat(message.get("token")).isEqualTo("device-token-123");

        @SuppressWarnings("unchecked")
        Map<String, Object> android = (Map<String, Object>) message.get("android");
        assertThat(android).containsEntry("priority", "high");

        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) message.get("data");
        assertThat(data).containsExactly(
            Map.entry("type", "GROUP_MEMBER_NOTIFICATION"),
            Map.entry("groupId", "11"),
            Map.entry("notificationId", "44"),
            Map.entry("actorUserId", "22"),
            Map.entry("targetUserId", "33"),
            Map.entry("groupName", "Stammtisch"),
            Map.entry("title", "Bierliste"),
            Map.entry("message", "Bring heute 5,00€ mit")
        );
    }

    @Test
    void invalidTokenDetectionReturnsTrueForUnregisteredTokens() {
        FcmAndroidPushSender sender = new FcmAndroidPushSender(RestClient.builder(), "");

        String responseBody = """
            {
              "error": {
                "code": 404,
                "message": "Requested entity was not found.",
                "status": "UNREGISTERED",
                "details": [
                  {
                    "@type": "type.googleapis.com/google.firebase.fcm.v1.FcmError",
                    "errorCode": "UNREGISTERED"
                  }
                ]
              }
            }
            """;

        assertThat(sender.isInvalidTokenResponse(responseBody)).isTrue();
    }

    @Test
    void invalidTokenDetectionDoesNotDeleteTokensForGenericBadRequestPayloadErrors() {
        FcmAndroidPushSender sender = new FcmAndroidPushSender(RestClient.builder(), "");

        String responseBody = """
            {
              "error": {
                "code": 400,
                "message": "Invalid value at 'message.android.priority' (TYPE_STRING), \\"HIGH\\"",
                "status": "INVALID_ARGUMENT",
                "details": [
                  {
                    "@type": "type.googleapis.com/google.rpc.BadRequest",
                    "fieldViolations": [
                      {
                        "field": "message.android.priority",
                        "description": "Invalid value at 'message.android.priority' (TYPE_STRING), \\"HIGH\\""
                      }
                    ]
                  }
                ]
              }
            }
            """;

        assertThat(sender.isInvalidTokenResponse(responseBody)).isFalse();
    }
}
