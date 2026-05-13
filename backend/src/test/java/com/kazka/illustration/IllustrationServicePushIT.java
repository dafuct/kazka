package com.kazka.illustration;

import com.kazka.AbstractIT;
import com.kazka.device.DeviceToken;
import com.kazka.device.DeviceTokenRepository;
import com.kazka.device.PushNotifier;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import com.kazka.story.StoryRepository;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class IllustrationServicePushIT extends AbstractIT {

    @Autowired private IllustrationService illustrationService;
    @Autowired private StoryRepository stories;
    @Autowired private UserRepository users;
    @Autowired private DeviceTokenRepository devices;

    @MockitoBean private PushNotifier pushNotifier;

    @Test
    void should_fire_push_when_illustration_saves() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                System.getenv("HUGGINGFACE_API_TOKEN") != null,
                "HUGGINGFACE_API_TOKEN not set");

        User u = new User();
        u.setId(UUID.randomUUID().toString());
        u.setEmail("push-it@example.com");
        u.setEmailVerified(true);
        u.setPasswordHash("x");
        users.save(u);

        DeviceToken dt = new DeviceToken();
        dt.setId(UUID.randomUUID().toString());
        dt.setUserId(u.getId());
        dt.setDeviceToken("a".repeat(64));
        dt.setPlatform("ios");
        dt.setLocale("uk");
        devices.save(dt);

        Story s = new Story();
        s.setId(UUID.randomUUID().toString());
        s.setUserId(u.getId());
        s.setTitle("Test");
        s.setContent("Once upon a time…");
        s.setCharacters(List.of("fox"));
        s.setTheme("forest");
        s.setIllustrationStatus(IllustrationStatus.PENDING);
        stories.save(s);

        illustrationService.generateAndStore(s.getId()).block();

        verify(pushNotifier, atLeastOnce()).notifyStoryReady(eq(u.getId()), eq(s.getId()), anyString());
    }
}
