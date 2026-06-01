package com.kazka.comics;

import com.kazka.ai.AiClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActsStructurerTest {

    @Mock AiClient aiClient;
    @InjectMocks ActsStructurer structurer;

    private static final String FIVE = """
        [
          {"scene":"forest dawn","narration":"У лісі прокидалися птахи.","dialog":[{"speaker":"Лис","line":"Привіт"}]},
          {"scene":"river crossing","narration":"Текла річка.","dialog":[]},
          {"scene":"meadow","narration":"Лука сяяла.","dialog":[{"speaker":"Лис","line":"Як гарно"}]},
          {"scene":"storm","narration":"Налетіла буря.","dialog":[]},
          {"scene":"sunset home","narration":"Вечоріло.","dialog":[]}
        ]""";

    @Test
    void should_returnFiveBeats_when_aiReturnsValidJson() {
        when(aiClient.generateText(anyString(), anyString())).thenReturn(Mono.just(FIVE));

        List<Act> acts = structurer.structure("Тестова казка про лиса", "uk").block();

        assertThat(acts).hasSize(5);
        assertThat(acts.get(0).scene()).isEqualTo("forest dawn");
        assertThat(acts.get(0).dialog()).hasSize(1);
        assertThat(acts.get(0).dialog().get(0).speaker()).isEqualTo("Лис");
    }

    @Test
    void should_extractJson_when_aiWrapsInMarkdownFences() {
        when(aiClient.generateText(anyString(), anyString()))
                .thenReturn(Mono.just("```json\n" + FIVE + "\n```"));

        List<Act> acts = structurer.structure("Some tale", "uk").block();

        assertThat(acts).hasSize(5);
        assertThat(acts.get(4).scene()).isEqualTo("sunset home");
    }

    @Test
    void should_throw_when_aiReturnsNonArray() {
        when(aiClient.generateText(anyString(), anyString())).thenReturn(Mono.just("{\"oops\":true}"));

        assertThatThrownBy(() -> structurer.structure("anything", "uk").block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected JSON array of beats");
    }

    @Test
    void should_throw_when_aiReturnsWrongCount() {
        String two = """
            [
              {"scene":"a","narration":"x","dialog":[]},
              {"scene":"b","narration":"y","dialog":[]}
            ]""";
        when(aiClient.generateText(anyString(), anyString())).thenReturn(Mono.just(two));

        assertThatThrownBy(() -> structurer.structure("anything", "uk").block())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expected 5 beats");
    }
}
