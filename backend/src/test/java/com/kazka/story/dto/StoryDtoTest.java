package com.kazka.story.dto;

import com.kazka.illustration.ImageUrlResolver;
import com.kazka.story.IllustrationStatus;
import com.kazka.story.Story;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StoryDtoTest {

    /** Mimics a real resolver: null key -> null, otherwise a signed-looking URL. */
    private static final ImageUrlResolver RESOLVER =
            key -> key == null ? null : "https://signed.example/" + key;

    @Test
    void from_mapsBasicFields() {
        Story story = new Story();
        story.setId("s1");
        story.setTitle("The Fox");
        story.setIllustrationStatus(IllustrationStatus.PENDING);

        StoryDto dto = StoryDto.from(story, List.of(), RESOLVER);

        assertThat(dto.id()).isEqualTo("s1");
        assertThat(dto.title()).isEqualTo("The Fox");
        assertThat(dto.illustrationStatus()).isEqualTo(IllustrationStatus.PENDING);
        assertThat(dto.panels()).isEmpty();
    }
}
