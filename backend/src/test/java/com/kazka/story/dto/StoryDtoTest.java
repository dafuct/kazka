package com.kazka.story.dto;

import com.kazka.illustration.ImageUrlResolver;
import com.kazka.story.Story;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoryDtoTest {

    /** Mimics a real resolver: null key -> null, otherwise a signed-looking URL. */
    private static final ImageUrlResolver RESOLVER =
            key -> key == null ? null : "https://signed.example/" + key;

    @Test
    void from_resolvesIllustrationKeysToUrls() {
        Story s = new Story();
        s.setId("s1");
        s.setIllustrationPathLight("s1-light.png");
        s.setIllustrationPathDark("s1-dark.png");

        StoryDto dto = StoryDto.from(s, RESOLVER);

        assertThat(dto.illustrationPathLight()).isEqualTo("https://signed.example/s1-light.png");
        assertThat(dto.illustrationPathDark()).isEqualTo("https://signed.example/s1-dark.png");
    }

    @Test
    void from_nullIllustration_staysNull() {
        Story s = new Story();
        s.setId("s2");

        StoryDto dto = StoryDto.from(s, RESOLVER);

        assertThat(dto.illustrationPathLight()).isNull();
        assertThat(dto.illustrationPathDark()).isNull();
    }
}
