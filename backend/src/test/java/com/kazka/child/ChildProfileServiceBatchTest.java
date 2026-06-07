package com.kazka.child;

import com.kazka.child.dto.CreateChildProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChildProfileServiceBatchTest {

    @Mock ChildProfileRepository repo;
    @Mock CharacterRepository characters;
    @Mock ChildEntitlementResolver tier;
    @InjectMocks ChildProfileService service;

    @Test
    void should_create_all_children_when_batch_submitted() {
        when(tier.maxChildProfiles(anyString())).thenReturn(Integer.MAX_VALUE);
        when(repo.countByUserIdAndArchivedAtIsNull(anyString())).thenReturn(0L);
        when(repo.save(any(ChildProfile.class))).thenAnswer(i -> i.getArgument(0));

        var reqs = List.of(
                new CreateChildProfileRequest("Ivan", (short) 2018, "boy", "uk", List.of()),
                new CreateChildProfileRequest("Olena", (short) 2020, "girl", "uk", List.of()));

        List<ChildProfile> created = service.createBatch("u1", reqs);

        assertThat(created).hasSize(2);
        assertThat(created).extracting(ChildProfile::getName).containsExactly("Ivan", "Olena");
    }
}
