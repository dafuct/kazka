package com.kazka.child;

import com.kazka.child.dto.ExtractedCandidateDto;
import com.kazka.story.exception.PaywallRequiredException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

    @Mock CharacterRepository repo;
    @Mock ChildProfileService profiles;
    @Mock ChildEntitlementResolver tier;
    @Mock StoryCharacterRepository joinRepo;
    @InjectMocks CharacterService svc;

    @Test
    void should_mergeTraitsAsUnion_on_upsert() {
        ChildProfile profile = new ChildProfile(); profile.setId("p1"); profile.setUserId("u");
        when(profiles.requireOwned("p1", "u")).thenReturn(profile);
        when(tier.maxSavedCharacters("u")).thenReturn(Integer.MAX_VALUE);

        com.kazka.child.Character existing = new com.kazka.child.Character();
        existing.setId("c1"); existing.setChildProfileId("p1");
        existing.setName("Мурка"); existing.setKind("animal");
        existing.setDescription("a cat"); existing.setTraits(List.of("curious"));
        existing.setUsageCount(2);
        when(repo.findByChildProfileIdAndName("p1", "Мурка")).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var candidate = new ExtractedCandidateDto("Мурка", "animal", "a cat with green eyes",
                List.of("brave", "curious"), "companion");
        svc.upsertConfirmed("p1", "u", "story-7", List.of(candidate));

        ArgumentCaptor<com.kazka.child.Character> captor = ArgumentCaptor.forClass(com.kazka.child.Character.class);
        verify(repo).save(captor.capture());
        com.kazka.child.Character saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("c1");
        assertThat(saved.getDescription()).isEqualTo("a cat");
        assertThat(saved.getTraits()).containsExactlyInAnyOrder("curious", "brave");
        assertThat(saved.getUsageCount()).isEqualTo(3);
        assertThat(saved.getLastUsedAt()).isNotNull();
    }

    @Test
    void should_throw_PaywallRequired_when_confirmingAtFreeLimit() {
        ChildProfile profile = new ChildProfile(); profile.setId("p1"); profile.setUserId("u");
        when(profiles.requireOwned("p1", "u")).thenReturn(profile);
        when(tier.maxSavedCharacters("u")).thenReturn(0);
        when(repo.countByChildProfileIdAndArchivedAtIsNull("p1")).thenReturn(0L);
        when(repo.findByChildProfileIdAndName(eq("p1"), any())).thenReturn(Optional.empty());

        var candidate = new ExtractedCandidateDto("Олег", "boy", "a kind boy", List.of(), "protagonist");
        assertThatThrownBy(() -> svc.upsertConfirmed("p1", "u", "story-1", List.of(candidate)))
                .isInstanceOf(PaywallRequiredException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void should_createNewCharacter_when_nameNotSeenBefore() {
        ChildProfile profile = new ChildProfile(); profile.setId("p1"); profile.setUserId("u");
        when(profiles.requireOwned("p1", "u")).thenReturn(profile);
        when(tier.maxSavedCharacters("u")).thenReturn(Integer.MAX_VALUE);
        when(repo.findByChildProfileIdAndName("p1", "Олег")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var candidate = new ExtractedCandidateDto("Олег", "boy", "a kind boy",
                List.of("kind"), "protagonist");
        var saved = svc.upsertConfirmed("p1", "u", "story-1", List.of(candidate));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getName()).isEqualTo("Олег");
        assertThat(saved.get(0).getUsageCount()).isEqualTo(1);
        assertThat(saved.get(0).getFirstStoryId()).isEqualTo("story-1");
    }
}
