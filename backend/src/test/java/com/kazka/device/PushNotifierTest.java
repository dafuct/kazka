package com.kazka.device;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import com.kazka.auth.AuthProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushNotifierTest {

    @Mock DeviceTokenRepository devices;
    @Mock ApnsClient apnsClient;

    AuthProperties.Apns disabledApns = new AuthProperties.Apns(
            "TEAM", "KEY", "", "app.kazka.ios", "api.sandbox.push.apple.com", false);
    AuthProperties.Apns enabledApns = new AuthProperties.Apns(
            "TEAM", "KEY", "", "app.kazka.ios", "api.sandbox.push.apple.com", true);

    @Test
    void should_doNothing_when_apnsDisabled() {
        var notifier = new PushNotifier(devices, Optional.of(apnsClient), disabledApns);

        notifier.notifyStoryReady("user-1", "story-1", "Test Title");

        verify(devices, never()).findByUserId(any());
        verify(apnsClient, never()).sendNotification(any());
    }

    @Test
    void should_doNothing_when_apnsClientEmpty() {
        var notifier = new PushNotifier(devices, Optional.<ApnsClient>empty(), enabledApns);

        notifier.notifyStoryReady("user-1", "story-1", "Test Title");

        verify(devices, never()).findByUserId(any());
        verify(apnsClient, never()).sendNotification(any());
    }

    @Test
    void should_doNothing_when_userHasNoDevices() {
        var notifier = new PushNotifier(devices, Optional.of(apnsClient), enabledApns);
        when(devices.findByUserId("user-1")).thenReturn(List.of());

        notifier.notifyStoryReady("user-1", "story-1", "Test Title");

        verify(apnsClient, never()).sendNotification(any());
    }

    @Test
    void should_sendOnePushPerDevice_when_userHasMultipleDevices() {
        var notifier = new PushNotifier(devices, Optional.of(apnsClient), enabledApns);
        // Tokens must be valid hex strings — TokenUtil.sanitizeTokenString strips non-hex chars.
        String tokenA = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344";
        String tokenB = "ddccbbaa44332211ddccbbaa44332211ddccbbaa44332211ddccbbaa44332211";
        when(devices.findByUserId("user-1")).thenReturn(List.of(
                deviceToken(tokenA),
                deviceToken(tokenB)));
        // PushNotificationFuture extends CompletableFuture; completing with null short-circuits
        // the whenComplete branches (NPE inside async callback is swallowed by the future).
        @SuppressWarnings({"rawtypes", "unchecked"})
        PushNotificationFuture completed = new PushNotificationFuture<>(null);
        completed.complete(null);
        org.mockito.Mockito.doReturn(completed).when(apnsClient).sendNotification(any());

        notifier.notifyStoryReady("user-1", "story-1", "Test Title");

        ArgumentCaptor<SimpleApnsPushNotification> captor =
                ArgumentCaptor.forClass(SimpleApnsPushNotification.class);
        verify(apnsClient, org.mockito.Mockito.times(2)).sendNotification(captor.capture());
        assertThat(captor.getAllValues()).extracting(SimpleApnsPushNotification::getToken)
                .containsExactlyInAnyOrder(tokenA, tokenB);
        assertThat(captor.getAllValues()).allMatch(notification -> notification.getPayload().contains("story-1"));
    }

    private DeviceToken deviceToken(String token) {
        DeviceToken dt = new DeviceToken();
        dt.setId(UUID.randomUUID().toString());
        dt.setUserId("user-1");
        dt.setDeviceToken(token);
        dt.setPlatform("ios");
        return dt;
    }
}
