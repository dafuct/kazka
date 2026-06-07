package com.kazka.child;

import com.kazka.AbstractIT;
import com.kazka.billing.EntitlementDowngradedEvent;
import com.kazka.billing.EntitlementResolver;
import com.kazka.user.User;
import com.kazka.user.UserRepository;
import com.kazka.user.UserRole;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("integration")
class EntitlementDowngradeListenerIT extends AbstractIT {

    @Autowired ApplicationEventPublisher events;
    @Autowired ChildProfileRepository profiles;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwordEncoder;
    @MockitoBean EntitlementResolver entitlements;

    @Test
    void should_archive_all_but_most_recently_used_profile() {
        String userId = seedUser();
        ChildProfile profileA = makeProfile(userId, "A");
        ChildProfile profileB = makeProfile(userId, "B");
        ChildProfile profileC = makeProfile(userId, "C");
        profiles.saveAll(List.of(profileA, profileB, profileC));

        when(entitlements.isPro(userId)).thenReturn(false);

        events.publishEvent(new EntitlementDowngradedEvent(userId));

        List<ChildProfile> active = profiles.findByUserIdAndArchivedAtIsNullOrderByCreatedAtAsc(userId);
        assertThat(active).hasSize(1);
    }

    private ChildProfile makeProfile(String userId, String name) {
        ChildProfile profile = new ChildProfile();
        profile.setId(UUID.randomUUID().toString());
        profile.setUserId(userId);
        profile.setName(name);
        profile.setAvatarSeed("s");
        profile.setPreferredLanguage("uk");
        return profile;
    }

    private String seedUser() {
        String id = UUID.randomUUID().toString();
        User user = new User();
        user.setId(id);
        user.setEmail(id + "@test");
        user.setDisplayName("T");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(UserRole.USER);
        user.setEmailVerified(true);
        users.save(user);
        return id;
    }
}
