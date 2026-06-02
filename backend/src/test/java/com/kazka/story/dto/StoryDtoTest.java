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
        Story s = new Story();
        s.setId("s1");
        s.setTitle("The Fox");
        s.setIllustrationStatus(IllustrationStatus.PENDING);

        StoryDto dto = StoryDto.from(s, List.of(), RESOLVER);

        assertThat(dto.id()).isEqualTo("s1");
        assertThat(dto.title()).isEqualTo("The Fox");
        assertThat(dto.illustrationStatus()).isEqualTo(IllustrationStatus.PENDING);
        assertThat(dto.panels()).isEmpty();
    }
}
