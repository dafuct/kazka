package com.kazka.child;

import com.kazka.child.dto.CreateChildProfileRequest;
import com.kazka.child.dto.UpdateChildProfileRequest;
import com.kazka.story.exception.PaywallRequiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChildProfileServiceTest {

    @Mock ChildProfileRepository repo;
    @Mock CharacterRepository characters;
    @Mock ChildEntitlementResolver tier;
    @InjectMocks ChildProfileService svc;

    @Test
    void should_createProfile_when_underTierLimit() {
        when(tier.maxChildProfiles("u")).thenReturn(1);
        when(repo.countByUserIdAndArchivedAtIsNull("u")).thenReturn(0L);
        when(repo.save(any(ChildProfile.class))).thenAnswer(i -> i.getArgument(0));

        ChildProfile created = svc.create("u",
                new CreateChildProfileRequest("Лія", (short)2020, "girl", "uk", List.of("dragons")));

        assertThat(created.getName()).isEqualTo("Лія");
        assertThat(created.getAvatarSeed()).isNotBlank();
    }

    @Test
    void should_throw_PaywallRequired_when_creatingProfileAtFreeLimit() {
        when(tier.maxChildProfiles("u")).thenReturn(1);
        when(repo.countByUserIdAndArchivedAtIsNull("u")).thenReturn(1L);

        assertThatThrownBy(() -> svc.create("u",
                new CreateChildProfileRequest("Other", null, null, "uk", List.of())))
                .isInstanceOf(PaywallRequiredException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void should_return404_when_fetchingProfileOfAnotherUser() {
        when(repo.findByIdAndUserId("p1", "u")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.requireOwned("p1", "u"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void should_archiveOnDelete_notHardDelete() {
        ChildProfile profile = new ChildProfile();
        profile.setId("p1"); profile.setUserId("u");
        when(repo.findByIdAndUserId("p1", "u")).thenReturn(Optional.of(profile));

        svc.archive("p1", "u");

        ArgumentCaptor<ChildProfile> captor = ArgumentCaptor.forClass(ChildProfile.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getArchivedAt()).isNotNull();
    }

    @Test
    void should_updateMutableFields() {
        ChildProfile profile = new ChildProfile();
        profile.setId("p1"); profile.setUserId("u"); profile.setName("Old"); profile.setPreferredLanguage("uk");
        when(repo.findByIdAndUserId("p1", "u")).thenReturn(Optional.of(profile));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        ChildProfile updated = svc.update("p1", "u",
                new UpdateChildProfileRequest("New", (short)2019, "boy", "en", List.of("space")));

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getBirthYear()).isEqualTo((short)2019);
        assertThat(updated.getPreferredLanguage()).isEqualTo("en");
        assertThat(updated.getInterests()).containsExactly("space");
    }
}
