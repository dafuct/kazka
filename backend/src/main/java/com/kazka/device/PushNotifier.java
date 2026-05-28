package com.kazka.device;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import com.kazka.auth.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RequiredArgsConstructor
@Service
public class PushNotifier {

    private final DeviceTokenRepository devices;
    private final Optional<ApnsClient> apnsClient;
    private final AuthProperties.Apns apns;

    /**
     * Best-effort: enumerates all device tokens for the user, sends the push,
     * and removes tokens reported as Unregistered/BadDeviceToken. Never throws —
     * caller doesn't care about per-token failures.
     */
    public void notifyStoryReady(String userId, String storyId, String storyTitle) {
        if (!Boolean.TRUE.equals(apns.enabled()) || apnsClient.isEmpty()) {
            log.debug("APNs disabled; skipping push for user={} story={}", userId, storyId);
            return;
        }
        var tokens = devices.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No devices for user={}; skipping push", userId);
            return;
        }
        ApnsClient client = apnsClient.get();
        for (var dt : tokens) {
            sendOne(client, dt, storyId, storyTitle);
        }
    }

    private void sendOne(ApnsClient client, DeviceToken dt, String storyId, String storyTitle) {
        ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder()
                .setAlertTitle("Казка готова!")
                .setAlertBody(storyTitle)
                .setSound("default")
                .addCustomProperty("storyId", storyId)
                .addCustomProperty("type", "story_ready");

        String payload = payloadBuilder.build();
        String token = TokenUtil.sanitizeTokenString(dt.getDeviceToken());

        var notification = new SimpleApnsPushNotification(token, apns.bundleId(), payload);

        CompletableFuture<PushNotificationResponse<SimpleApnsPushNotification>> future =
                client.sendNotification(notification);

        future.whenComplete((response, throwable) -> {
            if (throwable != null) {
                log.warn("APNs send failed for token={}: {}", token, throwable.getMessage());
                return;
            }
            if (!response.isAccepted()) {
                String reason = response.getRejectionReason().orElse("unknown");
                log.warn("APNs rejected token={}: {}", token, reason);
                if ("Unregistered".equalsIgnoreCase(reason) || "BadDeviceToken".equalsIgnoreCase(reason)) {
                    devices.deleteByDeviceToken(dt.getDeviceToken());
                }
            }
        });
    }
}
