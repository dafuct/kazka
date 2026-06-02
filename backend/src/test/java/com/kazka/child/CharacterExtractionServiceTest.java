package com.kazka.child;

import com.kazka.ai.AiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CharacterExtractionServiceTest {

    @Mock AiClient aiClient;
    @InjectMocks CharacterExtractionService svc;

    @Test
    void should_parse_valid_json_into_candidates() {
        String json = """
                [{"name":"Мурка","kind":"animal","description":"a cat","traits":["curious"],"role":"companion"}]
                """;
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just(json));

        var candidates = svc.extract("body text", "uk").block();
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).name()).isEqualTo("Мурка");
        assertThat(candidates.get(0).traits()).containsExactly("curious");
    }

    @Test
    void should_return_empty_list_on_malformed_json() {
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just("not json"));
        var candidates = svc.extract("body", "en").block();
        assertThat(candidates).isEmpty();
    }

    @Test
    void should_strip_markdown_fences_when_present() {
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just(
                "```json\n[{\"name\":\"X\",\"kind\":\"boy\",\"description\":\"d\",\"traits\":[],\"role\":\"protagonist\"}]\n```"));
        var candidates = svc.extract("body", "en").block();
        assertThat(candidates).hasSize(1);
        assertThat(candidates.get(0).name()).isEqualTo("X");
    }

    @Test
    void should_cap_traits_at_eight() {
        String json = "[{\"name\":\"X\",\"kind\":\"boy\",\"description\":\"d\"," +
                "\"traits\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\"]," +
                "\"role\":\"protagonist\"}]";
        when(aiClient.streamText(anyString(), anyString())).thenReturn(Flux.just(json));
        var candidates = svc.extract("body", "en").block();
        assertThat(candidates.get(0).traits()).hasSize(8);
    }
}
